package com.smartdevicelink.protocol;

import java.io.ByteArrayOutputStream;
import java.util.Hashtable;

import android.util.Log;

import com.smartdevicelink.exception.*;
import com.smartdevicelink.protocol.enums.*;
import com.smartdevicelink.util.BitConverter;
import com.smartdevicelink.util.DebugTool;

public class WiProProtocol extends AbstractProtocol {
	byte _version = 1;
	private final static String FailurePropagating_Msg = "Failure propagating ";

	private static final int MTU_SIZE = 1500;
	private static int HEADER_SIZE = 8;
	private static int MAX_DATA_SIZE = MTU_SIZE - HEADER_SIZE;

	int hashID = 0;
	int messageID = 0;

    @SuppressWarnings("unused")
    private int _heartbeatSendInterval_ms = 0;
    @SuppressWarnings("unused")
    private int _heartbeatReceiveInterval_ms = 0;
	
	Hashtable<Integer, MessageFrameAssembler> _assemblerForMessageID = new Hashtable<Integer, MessageFrameAssembler>();
	Hashtable<Byte, Hashtable<Integer, MessageFrameAssembler>> _assemblerForSessionID = new Hashtable<Byte, Hashtable<Integer, MessageFrameAssembler>>();
	Hashtable<Byte, Object> _messageLocks = new Hashtable<Byte, Object>();

	// Hide no-arg ctor
	private WiProProtocol() {
		super(null);
	} // end-ctor

	public WiProProtocol(IProtocolListener protocolListener) {
		super(protocolListener);
	} // end-ctor
	
	public byte getVersion() {
		return this._version;
	}
	
	public void setVersion(byte version) {
		this._version = version;
		if (version > 1) {
			this._version = 2;
			HEADER_SIZE = 12;
			MAX_DATA_SIZE = MTU_SIZE - HEADER_SIZE;
		}
		else if (version == 1){
			HEADER_SIZE = 8;
			MAX_DATA_SIZE = MTU_SIZE - HEADER_SIZE;
		}
			
	}

	public void StartProtocolSession(SessionType sessionType) {
		SdlPacket header = SdlPacketFactory.createStartSession(sessionType, 0x00, _version, (byte) 0x00);
		handlePacketToSend(header);
	} // end-method

	private void sendStartProtocolSessionACK(SessionType sessionType, byte sessionID) {
		SdlPacket header = SdlPacketFactory.createStartSessionACK(sessionType, sessionID, 0x00, _version);
		handlePacketToSend(header);
	} // end-method
	
	public void EndProtocolSession(SessionType sessionType, byte sessionID) {
		SdlPacket header = SdlPacketFactory.createEndSession(sessionType, sessionID, hashID, _version);
		handlePacketToSend(header);
	} // end-method

	public void SendMessage(ProtocolMessage protocolMsg) {	
		protocolMsg.setRPCType((byte) 0x00); //always sending a request
		SessionType sessionType = protocolMsg.getSessionType();
		byte sessionID = protocolMsg.getSessionID();
		
		byte[] data = null;
		if (_version > 1 && sessionType != SessionType.NAV) {
			if (protocolMsg.getBulkData() != null) {
				data = new byte[12 + protocolMsg.getJsonSize() + protocolMsg.getBulkData().length];
				sessionType = SessionType.Bulk_Data;
			} else {
				data = new byte[12 + protocolMsg.getJsonSize()];
			}//FIXME this needs to be cleaned up. There is no need to copy data to two different arrays before sending
			BinaryFrameHeader binFrameHeader = new BinaryFrameHeader();
			binFrameHeader = SdlPacketFactory.createBinaryFrameHeader(protocolMsg.getRPCType(), protocolMsg.getFunctionID(), protocolMsg.getCorrID(), protocolMsg.getJsonSize());
			System.arraycopy(binFrameHeader.assembleHeaderBytes(), 0, data, 0, 12);
			System.arraycopy(protocolMsg.getData(), 0, data, 12, protocolMsg.getJsonSize());
			if (protocolMsg.getBulkData() != null) {
				System.arraycopy(protocolMsg.getBulkData(), 0, data, 12 + protocolMsg.getJsonSize(), protocolMsg.getBulkData().length);
			}
		} else {
			data = protocolMsg.getData();
		}
		
		// Get the message lock for this protocol session
		Object messageLock = _messageLocks.get(sessionID);
		if (messageLock == null) {
			handleProtocolError("Error sending protocol message to SDL.", 
					new SdlException("Attempt to send protocol message prior to startSession ACK.", SdlExceptionCause.SDL_UNAVAILABLE));
			return;
		}
		
		synchronized(messageLock) {
			if (data.length > MAX_DATA_SIZE) {
				
				messageID++;
	
				// Assemble first frame.
				int frameCount = data.length / MAX_DATA_SIZE;
				if (data.length % MAX_DATA_SIZE > 0) {
					frameCount++;
				}
				//byte[] firstFrameData = new byte[HEADER_SIZE];
				byte[] firstFrameData = new byte[8];
				// First four bytes are data size.
				System.arraycopy(BitConverter.intToByteArray(data.length), 0, firstFrameData, 0, 4);
				// Second four bytes are frame count.
				System.arraycopy(BitConverter.intToByteArray(frameCount), 0, firstFrameData, 4, 4);
				SdlPacket firstHeader = SdlPacketFactory.createMultiSendDataFirst(sessionType, sessionID, messageID, _version,firstFrameData);
				//Send the first frame
				handlePacketToSend(firstHeader);
				
				int currentOffset = 0;
				byte frameSequenceNumber = 0;

				for (int i = 0; i < frameCount; i++) {
					if (i < (frameCount - 1)) {
	                     ++frameSequenceNumber;
	                        if (frameSequenceNumber ==
	                                SdlPacket.FRAME_INFO_FINAL_CONNESCUTIVE_FRAME) {
	                            // we can't use 0x00 as frameSequenceNumber, because
	                            // it's reserved for the last frame
	                            ++frameSequenceNumber;
	                        }
					} else {
						frameSequenceNumber = SdlPacket.FRAME_INFO_FINAL_CONNESCUTIVE_FRAME;
					} // end-if
					
					int bytesToWrite = data.length - currentOffset;
					if (bytesToWrite > MAX_DATA_SIZE) { 
						bytesToWrite = MAX_DATA_SIZE; 
					}
					SdlPacket consecHeader = SdlPacketFactory.createMultiSendDataRest(sessionType, sessionID, bytesToWrite, frameSequenceNumber , messageID, _version,data, currentOffset, bytesToWrite);
					handlePacketToSend(consecHeader);
					currentOffset += bytesToWrite;
				}
			} else {
				messageID++;
				SdlPacket header = SdlPacketFactory.createSingleSendData(sessionType, sessionID, data.length, messageID, _version,data);
				handlePacketToSend(header);
			}
		}
	}

	public void handledPacketReceived(SdlPacket packet){
		//Check for a version difference
		if (_version == 1) {
			setVersion((byte)packet.version);	
		}
		
		MessageFrameAssembler assembler = getFrameAssemblerForFrame(packet);
		assembler.handleFrame(packet);
	}

	
	
	protected MessageFrameAssembler getFrameAssemblerForFrame(SdlPacket packet) {
		Hashtable<Integer, MessageFrameAssembler> hashSessionID = _assemblerForSessionID.get(packet.getSessionId());
		if (hashSessionID == null) {
			hashSessionID = new Hashtable<Integer, MessageFrameAssembler>();
			_assemblerForSessionID.put((byte)packet.getSessionId(), hashSessionID);
		} // end-if
		
		MessageFrameAssembler ret = (MessageFrameAssembler) _assemblerForMessageID.get(Integer.valueOf(packet.getMessageId()));
		if (ret == null) {
			ret = new MessageFrameAssembler();
			_assemblerForMessageID.put(Integer.valueOf(packet.getMessageId()), ret);
		} // end-if
		
		return ret;
	} // end-method

	protected class MessageFrameAssembler {
		protected boolean hasFirstFrame = false;
		protected ByteArrayOutputStream accumulator = null;
		protected int totalSize = 0;
		protected int framesRemaining = 0;

		protected void handleFirstDataFrame(SdlPacket packet) {
			//The message is new, so let's figure out how big it is.
			hasFirstFrame = true;
			totalSize = BitConverter.intFromByteArray(packet.payload, 0) - HEADER_SIZE;
			framesRemaining = BitConverter.intFromByteArray(packet.payload, 4);
			accumulator = new ByteArrayOutputStream(totalSize);
		}
		
		protected void handleRemainingFrame(SdlPacket packet) {
			accumulator.write(packet.payload, 0, (int)packet.getDataSize());
			notifyIfFinished(packet);
		}
		
		protected void notifyIfFinished(SdlPacket packet) {
			//if (framesRemaining == 0) {
			if (packet.getFrameType() == FrameType.Consecutive && packet.getFrameInfo() == 0x0) 
			{
				ProtocolMessage message = new ProtocolMessage();
				message.setSessionType(SessionType.valueOf((byte)packet.getServiceType()));
				message.setSessionID((byte)packet.getSessionId());
				//If it is WiPro 2.0 it must have binary header
				if (_version > 1) {
					BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
							parseBinaryHeader(accumulator.toByteArray());
					message.setVersion(_version);
					message.setRPCType(binFrameHeader.getRPCType());
					message.setFunctionID(binFrameHeader.getFunctionID());
					message.setCorrID(binFrameHeader.getCorrID());
					if (binFrameHeader.getJsonSize() > 0) message.setData(binFrameHeader.getJsonData());
					if (binFrameHeader.getBulkData() != null) message.setBulkData(binFrameHeader.getBulkData());
				} else{
					message.setData(accumulator.toByteArray());
				}
				
				_assemblerForMessageID.remove(packet.getMessageId());
				
				try {
					handleProtocolMessageReceived(message);
				} catch (Exception excp) {
					DebugTool.logError(FailurePropagating_Msg + "onProtocolMessageReceived: " + excp.toString(), excp);
				} // end-catch
				
				hasFirstFrame = false;
				accumulator = null;
			} // end-if
		} // end-method
		
		protected void handleMultiFrameMessageFrame(SdlPacket packet) {
			if (packet.getFrameType() == FrameType.First){
				handleFirstDataFrame(packet);
			}
			else{
				handleRemainingFrame(packet);
			}
				
		} // end-method
		
		protected void handleFrame(SdlPacket packet) {
			if (packet.getFrameType().equals(FrameType.Control)) {
				handleControlFrame(packet);
			} else {
				// Must be a form of data frame (single, first, consecutive, etc.)
				if (   packet.getFrameType() == FrameType.First
					|| packet.getFrameType() == FrameType.Consecutive
					) {
					handleMultiFrameMessageFrame(packet);
				} else {
					handleSingleFrameMessageFrame(packet);
				}
			} // end-if
		} // end-method
		
        private void handleProtocolHeartbeatACK(SdlPacket packet) {
        		WiProProtocol.this.handleProtocolHeartbeatACK(SessionType.valueOf((byte)packet.getServiceType()),(byte)packet.getSessionId());
        } // end-method		
		
		private void handleControlFrame(SdlPacket packet) {
            int frameInfo = packet.getFrameInfo();
            SessionType serviceType = SessionType.valueOf((byte)packet.getServiceType());
			if (frameInfo == FrameDataControlFrameType.HeartbeatACK.getValue()) {
                handleProtocolHeartbeatACK(packet);
            }
            else if (frameInfo == FrameDataControlFrameType.StartSession.getValue()) {
				sendStartProtocolSessionACK(serviceType, (byte)packet.getSessionId());
			} else if (frameInfo == FrameDataControlFrameType.StartSessionACK.getValue()) {
				// Use this sessionID to create a message lock
				Object messageLock = _messageLocks.get(packet.getSessionId());
				if (messageLock == null) {
					messageLock = new Object();
					_messageLocks.put((byte)packet.getSessionId(), messageLock);
				}
				//hashID = BitConverter.intFromByteArray(data, 0);
				if (_version > 1){
					hashID = packet.getMessageId();
				}
				handleProtocolSessionStarted(serviceType,(byte) packet.getSessionId(), _version, "");				
			} else if (frameInfo == FrameDataControlFrameType.StartSessionNACK.getValue()) {
				if (serviceType.eq(SessionType.NAV)) {
					handleProtocolSessionNACKed(serviceType, (byte)packet.getSessionId(), _version, "");
				} else {
					handleProtocolError("Got StartSessionNACK for protocol sessionID=" + packet.getSessionId(), null);
				}
			} else if (frameInfo == FrameDataControlFrameType.EndSession.getValue()) {
				//if (hashID == BitConverter.intFromByteArray(data, 0)) 
				if (_version > 1) {
					if (hashID == packet.getMessageId()){
						handleProtocolSessionEnded(serviceType, (byte)packet.getSessionId(), "");
					}//else...nothing
				} else {
					handleProtocolSessionEnded(serviceType, (byte)packet.getSessionId(), "");
				}
			} else if (frameInfo == FrameDataControlFrameType.EndSessionACK.getValue()) {
				handleProtocolSessionEnded(serviceType, (byte)packet.getSessionId(), "");
			}
		} // end-method
				
		private void handleSingleFrameMessageFrame(SdlPacket packet) {
			ProtocolMessage message = new ProtocolMessage();
            SessionType serviceType = SessionType.valueOf((byte)packet.getServiceType());
			if (serviceType == SessionType.RPC) {
				message.setMessageType(MessageType.RPC);
			} else if (serviceType == SessionType.Bulk_Data) {
				message.setMessageType(MessageType.BULK);
			} // end-if
			message.setSessionType(serviceType);
			message.setSessionID((byte)packet.getSessionId());
			//If it is WiPro 2.0 it must have binary header
			if (_version > 1) {
				BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
						parseBinaryHeader(packet.payload);
				message.setVersion(_version);
				message.setRPCType(binFrameHeader.getRPCType());
				message.setFunctionID(binFrameHeader.getFunctionID());
				message.setCorrID(binFrameHeader.getCorrID());
				if (binFrameHeader.getJsonSize() > 0){
					message.setData(binFrameHeader.getJsonData());
				}
				if (binFrameHeader.getBulkData() != null){
					message.setBulkData(binFrameHeader.getBulkData());
				}
			} else {
				message.setData(packet.payload);
			}
			
			_assemblerForMessageID.remove(packet.getMessageId());
			
			try {
				handleProtocolMessageReceived(message);
			} catch (Exception ex) {
				DebugTool.logError(FailurePropagating_Msg + "onProtocolMessageReceived: " + ex.toString(), ex);
				handleProtocolError(FailurePropagating_Msg + "onProtocolMessageReceived: ", ex);
			} // end-catch
		} // end-method
	} // end-class

	@Override
	public void StartProtocolService(SessionType sessionType, byte sessionID) {
		SdlPacket header = SdlPacketFactory.createStartSession(sessionType, 0x00, _version, sessionID);
		handlePacketToSend(header);
		
	}

	@Override
	public void SetHeartbeatSendInterval(int heartbeatSendInterval_ms) {
		_heartbeatSendInterval_ms = heartbeatSendInterval_ms;
		
	}

	@Override
	public void SetHeartbeatReceiveInterval(int heartbeatReceiveInterval_ms) {
		_heartbeatReceiveInterval_ms = heartbeatReceiveInterval_ms;
		
	}

	@Override
	public void SendHeartBeat(byte sessionID) {
        final SdlPacket heartbeat = SdlPacketFactory.createHeartbeat(SessionType.Heartbeat, sessionID, _version);        
        handlePacketToSend(heartbeat);		
	}
} // end-class