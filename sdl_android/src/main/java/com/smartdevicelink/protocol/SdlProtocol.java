package com.smartdevicelink.protocol;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.exception.SdlExceptionCause;
import com.smartdevicelink.protocol.enums.ControlFrameTags;
import com.smartdevicelink.protocol.enums.FrameDataControlFrameType;
import com.smartdevicelink.protocol.enums.FrameType;
import com.smartdevicelink.protocol.enums.MessageType;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.rpc.ImageResolution;
import com.smartdevicelink.proxy.rpc.VideoStreamingFormat;
import com.smartdevicelink.proxy.rpc.enums.VideoStreamingCodec;
import com.smartdevicelink.proxy.rpc.enums.VideoStreamingProtocol;
import com.smartdevicelink.security.SdlSecurityBase;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.TransportConstants;
import com.smartdevicelink.transport.TransportManager;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.TransportRecord;
import com.smartdevicelink.util.BitConverter;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.Version;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SdlProtocol {
    private static final String TAG ="SdlProtocol";
    private final static String FailurePropagating_Msg = "Failure propagating ";

    private static final int TLS_MAX_RECORD_SIZE = 16384;

    private static final int PRIMARY_TRANSPORT_ID    = 1;
    private static final int SECONDARY_TRANSPORT_ID  = 2;


    protected static final int V1_HEADER_SIZE = 8;
    protected static final int V2_HEADER_SIZE = 12;

    //If increasing MAX PROTOCOL VERSION major version, make sure to alter it in SdlPsm
    public static final Version MAX_PROTOCOL_VERSION = new Version("5.1.0");

    public static final int V1_V2_MTU_SIZE = 1500;
    public static final int V3_V4_MTU_SIZE = 131072;

    public static final List<SessionType> HIGH_BANDWIDTH_SERVICES
            = Arrays.asList(SessionType.NAV, SessionType.PCM);

    // Lock to ensure all frames are sent uninterrupted
    private final Object FRAME_LOCK = new Object();

    private final ISdlProtocol iSdlProtocol;
    private final MultiplexTransportConfig transportConfig;
    private final Hashtable<Integer, MessageFrameAssembler> _assemblerForMessageID = new Hashtable<>();
    private final Hashtable<Byte, Hashtable<Integer, MessageFrameAssembler>> _assemblerForSessionID = new Hashtable<>();
    private final Hashtable<Byte, Object> _messageLocks = new Hashtable<>();
    private final HashMap<SessionType, Long> mtus = new HashMap<>();
    private final HashMap<SessionType, TransportRecord> activeTransports = new HashMap<>();
    private final Map<TransportType, List<ISecondaryTransportListener>> secondaryTransportListeners = new HashMap<>();

    private List<Integer> audio;
    private List<Integer> video;
    private List<TransportRecord> connectedTransports;

    private TransportManager transportManager;
    private Version protocolVersion = new Version("1.0.0");
    private int hashID = 0;
    private int messageID = 0;
    private int headerSize = 8;

    /**
     * Requested transports for primary and secondary
     */
    List<TransportType> requestedPrimaryTransports, requestedSecondaryTransports;

    /**
     * List of secondary transports supported by the module
     */
    List<TransportType> supportedSecondaryTransports;

    /**
     * Holds the priority of transports for a specific service when that service can be started
     * on a primary or secondary transport.
     */
    Map<SessionType, List<Integer>> transportPriorityForServiceMap;
    boolean requiresHighBandwidth;
    Map<TransportType, Bundle> secondaryTransportParams;
    TransportRecord connectedPrimaryTransport;


    @SuppressWarnings("ConstantConditions")
    public SdlProtocol(@NonNull ISdlProtocol iSdlProtocol, @NonNull MultiplexTransportConfig config) {
        if (iSdlProtocol == null ) {
            throw new IllegalArgumentException("Provided protocol listener interface reference is null");
        } // end-if

        this.iSdlProtocol = iSdlProtocol;
        this.transportConfig = config;
        this.requestedPrimaryTransports = this.transportConfig.getPrimaryTransports();
        this.requestedSecondaryTransports = this.transportConfig.getSecondaryTransports();
        this.requiresHighBandwidth = this.transportConfig.requiresHighBandwidth();
        this.transportManager = new TransportManager(transportConfig, transportEventListener);


        mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
    } // end-ctor


    public void start(){
        transportManager.start();

    }
    /**
     * Retrieves the max payload size for a packet to be sent to the module
     * @return the max transfer unit
     */
    public int getMtu(){
        return mtus.get(SessionType.RPC).intValue();
    }

    public long getMtu(SessionType type){
        Long mtu = mtus.get(type);
        if(mtu == null){
            mtu = mtus.get(SessionType.RPC);
        }
        return mtu;
    }

    public boolean isConnected(){
        return transportManager != null && transportManager.isConnected(null,null);
    }

    public TransportRecord getTransportForSession(SessionType type){
        return activeTransports.get(type);
    }

    /**
     * Resets the protocol to init status
     */
    protected void reset(){
        protocolVersion = new Version("1.0.0");
        hashID = 0;
        messageID = 0;
        headerSize = 8;
        this.activeTransports.clear();
        this.mtus.clear();
        mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        this.secondaryTransportParams = null;
        this._assemblerForMessageID.clear();
        this._assemblerForSessionID.clear();
        this._messageLocks.clear();
    }

    /**
     * For logging purposes, prints active services on each connected transport
     */
    protected void printActiveTransports(){
        StringBuilder activeTransportString = new StringBuilder();
        activeTransportString.append("Active transports --- \n");

        for(Map.Entry entry : activeTransports.entrySet()){
            String sessionString = null;
            if(entry.getKey().equals(SessionType.NAV)) {
                sessionString = "NAV";
            }else if(entry.getKey().equals(SessionType.PCM)) {
                sessionString = "PCM";
            }else if(entry.getKey().equals(SessionType.RPC)) {
                sessionString = "RPC";
            }
            if(sessionString != null){
                activeTransportString.append("Session: ");

                activeTransportString.append(sessionString);
                activeTransportString.append(" Transport: ");
                activeTransportString.append(entry.getValue().toString());
                activeTransportString.append("\n");
            }
        }
        Log.d(TAG, activeTransportString.toString());
    }

    protected void printSecondaryTransportDetails(List<String> secondary, List<Integer> audio, List<Integer> video){
        StringBuilder secondaryDetailsBldr = new StringBuilder();
        secondaryDetailsBldr.append("Checking secondary transport details \n");

        if(secondary != null){
            secondaryDetailsBldr.append("Supported secondary transports: ");
            for(String s : secondary){
                secondaryDetailsBldr.append(" ").append(s);
            }
            secondaryDetailsBldr.append("\n");
        }else{
            Log.d(TAG, "Supported secondary transports list is empty!");
        }
        if(audio != null){
            secondaryDetailsBldr.append("Supported audio transports: ");
            for(int a : audio){
                secondaryDetailsBldr.append(" ").append(a);
            }
            secondaryDetailsBldr.append("\n");
        }
        if(video != null){
            secondaryDetailsBldr.append("Supported video transports: ");
            for(int v : video){
                secondaryDetailsBldr.append(" ").append(v);
            }
            secondaryDetailsBldr.append("\n");
        }

        Log.d(TAG, secondaryDetailsBldr.toString());
    }


    private void setTransportPriorityForService(SessionType serviceType, List<Integer> order){
        if(transportPriorityForServiceMap == null){
            transportPriorityForServiceMap = new HashMap<>();
        }
        this.transportPriorityForServiceMap.put(serviceType, order);
        for(SessionType service : HIGH_BANDWIDTH_SERVICES){
            if (transportPriorityForServiceMap.get(service) != null
                    && transportPriorityForServiceMap.get(service).contains(PRIMARY_TRANSPORT_ID)) {
                if(connectedPrimaryTransport != null) {
                    activeTransports.put(service, connectedPrimaryTransport);
                }
            }
        }
    }

    /**
     * Handles when a secondary transport can be used to start services on or when the request as failed.
     * @param transportRecord the transport type that the event has taken place on
     * @param registered if the transport was successfully registered on
     */
    private void handleSecondaryTransportRegistration(TransportRecord transportRecord, boolean registered){
        if(registered) {
            //Session has been registered on secondary transport
            Log.d(TAG, transportRecord.getType().toString() + " transport was registered!");
            if (supportedSecondaryTransports.contains(transportRecord.getType())) {
                // If the transport type that is now available to be used it should be checked
                // against the list of services that might be able to be started on it

                for(SessionType secondaryService : HIGH_BANDWIDTH_SERVICES){
                    if (transportPriorityForServiceMap.containsKey(secondaryService)) {
                        // If this service type has extra information from the RPC StartServiceACK
                        // parse through it to find which transport should be used to start this
                        // specific service type
                        for(int transportNum : transportPriorityForServiceMap.get(secondaryService)){
                            if(transportNum == PRIMARY_TRANSPORT_ID){
                                break; // Primary is favored for this service type, break out...
                            }else if(transportNum == SECONDARY_TRANSPORT_ID){
                                // The secondary transport can be used to start this service
                                activeTransports.put(secondaryService, transportRecord);
                                break;
                            }
                        }
                    }
                }
            }
        }else{
            Log.d(TAG, transportRecord.toString() + " transport was NOT registered!");
        }
        //Notify any listeners for this secondary transport
        List<ISecondaryTransportListener> listenerList = secondaryTransportListeners.remove(transportRecord.getType());
        if(listenerList != null){
            for(ISecondaryTransportListener listener : listenerList){
                if(registered) {
                    listener.onConnectionSuccess(transportRecord);
                }else{
                    listener.onConnectionFailure();
                }
            }
        }

        if(DebugTool.isDebugEnabled()){
            printActiveTransports();
        }
    }

    public void onTransportsConnectedUpdate(List<TransportRecord> transports){
        Log.d(TAG, "Connected transport update");

        //Temporary: this logic should all be changed to handle multiple transports of the same type
        ArrayList<TransportType> connectedTransports = new ArrayList<>();
        if(transports != null) {
            for (TransportRecord record : transports) {
                connectedTransports.add(record.getType());
            }
        }

        if(connectedPrimaryTransport != null && !connectedTransports.contains(connectedPrimaryTransport.getType())){
            //The primary transport being used is no longer part of the connected transports
            //The transport manager callbacks should handle the disconnect code
            connectedPrimaryTransport = null;
            return;
        }

        if(activeTransports.get(SessionType.RPC) == null){
            //There is no currently active transport for the RPC service meaning no primary transport
            TransportRecord preferredPrimaryTransport = getPreferredTransport(requestedPrimaryTransports,transports);
            if(preferredPrimaryTransport != null) {
                connectedPrimaryTransport = preferredPrimaryTransport;
                startService(SessionType.RPC, (byte) 0x00, false);
            }else{
                onTransportNotAccepted("No transports match requested primary transport");
            }
            // return;
        }else if(secondaryTransportListeners != null
                && transports != null
                && iSdlProtocol!= null){
            // Check to see if there is a listener for a given transport.
            // If a listener exists, it can be assumed that the transport should be registered on
            for(TransportRecord record: transports){
                if(secondaryTransportListeners.get(record.getType()) != null
                        && !secondaryTransportListeners.get(record.getType()).isEmpty()){
                    registerSecondaryTransport(iSdlProtocol.getSessionId(), record);
                }
            }
        }
    }

    /**
     * Retrieves the preferred transport for the given connected transport
     * @param preferredList the list of preferred transports (primary or secondary)
     * @param connectedTransports the current list of connected transports
     * @return the preferred connected transport
     */
    private TransportRecord getPreferredTransport(List<TransportType> preferredList, List<TransportRecord> connectedTransports) {
        for (TransportType transportType : preferredList) {
            for(TransportRecord record: connectedTransports) {
                if (record.getType().equals(transportType)) {
                    return record;
                }
            }
        }
        return null;
    }

    public TransportRecord getPreferredPrimaryTransport(List<TransportRecord> transports){
        return getPreferredTransport(requestedPrimaryTransports, transports);

    }

    private void onTransportNotAccepted(String info){
        if(iSdlProtocol != null) {
            iSdlProtocol.shutdown(info);
        }
    }


    public Version getProtocolVersion(){
        return this.protocolVersion;
    }

    public int getMajorVersionByte(){
        return this.protocolVersion.getMajor();

    }

    /**
     * This method will set the major protocol version that we should use. It will also set the default MTU based on version.
     * @param version major version to use
     */
    protected void setVersion(byte version) {
        if (version > 5) {
            this.protocolVersion = new Version("5.0.0"); //protect for future, proxy only supports v5 or lower
            headerSize = 12;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE);
        } else if (version == 5) {
            this.protocolVersion = new Version("5.0.0");
            headerSize = 12;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE);
        }else if (version == 4) {
            this.protocolVersion = new Version("4.0.0");
            headerSize = 12;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE); //versions 4 supports 128k MTU
        } else if (version == 3) {
            this.protocolVersion = new Version("3.0.0");
            headerSize = 12;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE); //versions 3 supports 128k MTU
        } else if (version == 2) {
            this.protocolVersion = new Version("2.0.0");
            headerSize = 12;
            mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        } else if (version == 1){
            this.protocolVersion = new Version("1.0.0");
            headerSize = 8;
            mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        }
    }

  /*  @Deprecated
    public void StartProtocolSession(SessionType sessionType) {
        SdlPacket header = SdlPacketFactory.createStartSession(sessionType, 0x00, getMajorVersionByte(), (byte) 0x00, false);
        if(sessionType.equals(SessionType.RPC)){ // check for RPC session
            header.putTag(ControlFrameTags.RPC.StartService.PROTOCOL_VERSION, MAX_PROTOCOL_VERSION.toString());
        }
        handlePacketToSend(header);
    } // end-method
*/
    public void endSession(byte sessionID, int hashId) {
        SdlPacket header;
        if(protocolVersion.getMajor() < 5){
            header = SdlPacketFactory.createEndSession(SessionType.RPC, sessionID, hashId, (byte)protocolVersion.getMajor(), BitConverter.intToByteArray(hashId));
        }else{
            header = SdlPacketFactory.createEndSession(SessionType.RPC, sessionID, hashId, (byte)protocolVersion.getMajor(), new byte[0]);
            header.putTag(ControlFrameTags.RPC.EndService.HASH_ID, hashId);
        }

        handlePacketToSend(header);

    } // end-method

    public void sendPacket(SdlPacket packet){
        if(transportManager != null){
            transportManager.sendPacket(packet);
        }
    }

    public void sendMessage(ProtocolMessage protocolMsg) {
        protocolMsg.setRPCType((byte) 0x00); //always sending a request
        SessionType sessionType = protocolMsg.getSessionType();
        byte sessionID = protocolMsg.getSessionID();

        byte[] data;
        if (protocolVersion.getMajor() > 1 && sessionType != SessionType.NAV && sessionType != SessionType.PCM) {
            if (sessionType.eq(SessionType.CONTROL)) {
                final byte[] secureData = protocolMsg.getData().clone();
                data = new byte[headerSize + secureData.length];

                final BinaryFrameHeader binFrameHeader =
                        SdlPacketFactory.createBinaryFrameHeader(protocolMsg.getRPCType(),protocolMsg.getFunctionID(), protocolMsg.getCorrID(), 0);
                System.arraycopy(binFrameHeader.assembleHeaderBytes(), 0, data, 0, headerSize);
                System.arraycopy(secureData, 0, data, headerSize, secureData.length);
            }
            else if (protocolMsg.getBulkData() != null) {
                data = new byte[12 + protocolMsg.getJsonSize() + protocolMsg.getBulkData().length];
                sessionType = SessionType.BULK_DATA;
            } else {
                data = new byte[12 + protocolMsg.getJsonSize()];
            }
            if (!sessionType.eq(SessionType.CONTROL)) {
                BinaryFrameHeader binFrameHeader = SdlPacketFactory.createBinaryFrameHeader(protocolMsg.getRPCType(), protocolMsg.getFunctionID(), protocolMsg.getCorrID(), protocolMsg.getJsonSize());
                System.arraycopy(binFrameHeader.assembleHeaderBytes(), 0, data, 0, 12);
                System.arraycopy(protocolMsg.getData(), 0, data, 12, protocolMsg.getJsonSize());
                if (protocolMsg.getBulkData() != null) {
                    System.arraycopy(protocolMsg.getBulkData(), 0, data, 12 + protocolMsg.getJsonSize(), protocolMsg.getBulkData().length);
                }
            }
        } else {
            data = protocolMsg.getData();
        }

        if (iSdlProtocol != null && protocolMsg.getPayloadProtected()){

            if (data != null && data.length > 0) {
                byte[] dataToRead = new byte[TLS_MAX_RECORD_SIZE];
                SdlSecurityBase sdlSec = iSdlProtocol.getSdlSecurity();
                if (sdlSec == null)
                    return;

                Integer iNumBytes = sdlSec.encryptData(data, dataToRead);
                if ((iNumBytes == null) || (iNumBytes <= 0))
                    return;

                byte[] encryptedData = new byte[iNumBytes];
                System.arraycopy(dataToRead, 0, encryptedData, 0, iNumBytes);
                data = encryptedData;
            }
        }

        // Get the message lock for this protocol session
        Object messageLock = _messageLocks.get(sessionID);
        if (messageLock == null) {
            handleProtocolError("Error sending protocol message to SDL.",
                    new SdlException("Attempt to send protocol message prior to startSession ACK.", SdlExceptionCause.SDL_UNAVAILABLE));
            return;
        }

        synchronized(messageLock) {
            if (data.length > getMtu(sessionType)) {

                messageID++;

                // Assemble first frame.
                Long mtu = getMtu(sessionType);
                int frameCount = Long.valueOf(data.length / mtu).intValue();
                if (data.length % mtu > 0) {
                    frameCount++;
                }
                //byte[] firstFrameData = new byte[headerSize];
                byte[] firstFrameData = new byte[8];
                // First four bytes are data size.
                System.arraycopy(BitConverter.intToByteArray(data.length), 0, firstFrameData, 0, 4);
                // Second four bytes are frame count.
                System.arraycopy(BitConverter.intToByteArray(frameCount), 0, firstFrameData, 4, 4);

                SdlPacket firstHeader = SdlPacketFactory.createMultiSendDataFirst(sessionType, sessionID, messageID, (byte)protocolVersion.getMajor(),firstFrameData,protocolMsg.getPayloadProtected());
                firstHeader.setPriorityCoefficient(1+protocolMsg.priorityCoefficient);
                firstHeader.setTransportRecord(activeTransports.get(sessionType));
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
                    if (bytesToWrite > mtu) {
                        bytesToWrite = mtu.intValue();
                    }
                    SdlPacket consecHeader = SdlPacketFactory.createMultiSendDataRest(sessionType, sessionID, bytesToWrite, frameSequenceNumber , messageID, (byte)protocolVersion.getMajor(),data, currentOffset, bytesToWrite, protocolMsg.getPayloadProtected());
                    consecHeader.setTransportRecord(activeTransports.get(sessionType));
                    consecHeader.setPriorityCoefficient(i+2+protocolMsg.priorityCoefficient);
                    handlePacketToSend(consecHeader);
                    currentOffset += bytesToWrite;
                }
            } else {
                messageID++;
                SdlPacket header = SdlPacketFactory.createSingleSendData(sessionType, sessionID, data.length, messageID, (byte)protocolVersion.getMajor(),data, protocolMsg.getPayloadProtected());
                header.setPriorityCoefficient(protocolMsg.priorityCoefficient);
                header.setTransportRecord(activeTransports.get(sessionType));
                handlePacketToSend(header);
            }
        }
    }

    public void handlePacketReceived(SdlPacket packet){
        //Check for a version difference
        if (getMajorVersionByte() == 1) {
            setVersion((byte)packet.version);
        }

        MessageFrameAssembler assembler = getFrameAssemblerForFrame(packet);
        assembler.handleFrame(packet);

    }


    protected MessageFrameAssembler getFrameAssemblerForFrame(SdlPacket packet) {
        Integer iSessionId = packet.getSessionId();
        Byte bySessionId = iSessionId.byteValue();

        Hashtable<Integer, MessageFrameAssembler> hashSessionID = _assemblerForSessionID.get(bySessionId);
        if (hashSessionID == null) {
            hashSessionID = new Hashtable<>();
            _assemblerForSessionID.put(bySessionId, hashSessionID);
        } // end-if

        MessageFrameAssembler ret = _assemblerForMessageID.get(packet.getMessageId());
        if (ret == null) {
            ret = new MessageFrameAssembler();
            _assemblerForMessageID.put(packet.getMessageId(), ret);
        } // end-if

        return ret;
    } // end-method



    public void registerSecondaryTransport(byte sessionId, TransportRecord transportRecord) {
        SdlPacket header = SdlPacketFactory.createRegisterSecondaryTransport(sessionId, (byte)protocolVersion.getMajor());
        header.setTransportRecord(transportRecord);
        handlePacketToSend(header);
    }

    public void startService(SessionType serviceType, byte sessionID, boolean isEncrypted) {
        final SdlPacket header = SdlPacketFactory.createStartSession(serviceType, 0x00, (byte)protocolVersion.getMajor(), sessionID, isEncrypted);
        if(SessionType.RPC.equals(serviceType)){
            if(connectedPrimaryTransport != null) {
                header.setTransportRecord(connectedPrimaryTransport);
            }
            //This is going to be our primary transport
            header.putTag(ControlFrameTags.RPC.StartService.PROTOCOL_VERSION, MAX_PROTOCOL_VERSION.toString());
            handlePacketToSend(header);
            return; // We don't need to go any further
        }else if(serviceType.equals(SessionType.NAV)){
            if(iSdlProtocol != null){
                VideoStreamingParameters videoStreamingParameters = iSdlProtocol.getDesiredVideoParams();
                if(videoStreamingParameters != null) {
                    ImageResolution desiredResolution = videoStreamingParameters.getResolution();
                    VideoStreamingFormat desiredFormat = videoStreamingParameters.getFormat();
                    if (desiredResolution != null) {
                        header.putTag(ControlFrameTags.Video.StartService.WIDTH, desiredResolution.getResolutionWidth());
                        header.putTag(ControlFrameTags.Video.StartService.HEIGHT, desiredResolution.getResolutionHeight());
                    }
                    if (desiredFormat != null) {
                        header.putTag(ControlFrameTags.Video.StartService.VIDEO_CODEC, desiredFormat.getCodec().toString());
                        header.putTag(ControlFrameTags.Video.StartService.VIDEO_PROTOCOL, desiredFormat.getProtocol().toString());
                    }
                }
            }
        }
        if(transportPriorityForServiceMap == null
                || transportPriorityForServiceMap.get(serviceType) == null
                || transportPriorityForServiceMap.get(serviceType).isEmpty()){
            //If there is no transport priority for this service it can be assumed it's primary
            header.setTransportRecord(connectedPrimaryTransport);
            handlePacketToSend(header);
            return;
        }
        int transportPriority = transportPriorityForServiceMap.get(serviceType).get(0);
        if(transportPriority == PRIMARY_TRANSPORT_ID){
            // Primary is favored, and we're already connected...
            header.setTransportRecord(connectedPrimaryTransport);
            handlePacketToSend(header);
        }else if(transportPriority == SECONDARY_TRANSPORT_ID) {
            // Secondary is favored
            for(TransportType secondaryTransportType : supportedSecondaryTransports) {

                if(!requestedSecondaryTransports.contains(secondaryTransportType)){
                    // Secondary transport is not accepted by the client
                    continue;
                }

                if(activeTransports.get(serviceType) != null
                        && activeTransports.get(serviceType).getType() !=null
                        && activeTransports.get(serviceType).getType().equals(secondaryTransportType)){
                    // Transport is already active and accepted
                    header.setTransportRecord(activeTransports.get(serviceType));
                    handlePacketToSend(header);
                    return;
                }



                //If the secondary transport isn't connected yet that will have to be performed first

                List<ISecondaryTransportListener> listenerList = secondaryTransportListeners.get(secondaryTransportType);
                if(listenerList == null){
                    listenerList = new ArrayList<>();
                    secondaryTransportListeners.put(secondaryTransportType, listenerList);
                }

                //Check to see if the primary transport can also be used as a backup
                final boolean primaryTransportBackup = transportPriorityForServiceMap.get(serviceType).contains(PRIMARY_TRANSPORT_ID);

                listenerList.add(new ISecondaryTransportListener() {
                    @Override
                    public void onConnectionSuccess(TransportRecord transportRecord) {
                        header.setTransportRecord(transportRecord);
                        handlePacketToSend(header);
                    }

                    @Override
                    public void onConnectionFailure() {
                        if(primaryTransportBackup) {
                            // Primary is also supported as backup
                            header.setTransportRecord(connectedPrimaryTransport);
                            handlePacketToSend(header);
                        }else{
                            Log.d(TAG, "Failed to connect secondary transport, threw away StartService");
                        }
                    }
                });

                if(transportManager.isConnected(secondaryTransportType,null)){
                    //The transport is actually connected, however no service has been registered
                    registerSecondaryTransport(sessionID,transportManager.getTransportRecord(secondaryTransportType,null));
                }else if(secondaryTransportParams.containsKey(secondaryTransportType)) {
                    //No acceptable secondary transport is connected, so first one must be connected
                    header.setTransportRecord(new TransportRecord(secondaryTransportType,""));
                    transportManager.requestSecondaryTransportConnection(sessionID,secondaryTransportParams.get(secondaryTransportType));
                }else{
                    Log.w(TAG, "No params to connect to secondary transport");
                }

            }
        }
    }

    private void sendHeartBeatACK(SessionType sessionType, byte sessionID) {
        final SdlPacket heartbeat = SdlPacketFactory.createHeartbeatACK(SessionType.CONTROL, sessionID, (byte)protocolVersion.getMajor());
        heartbeat.setTransportRecord(activeTransports.get(sessionType));
        handlePacketToSend(heartbeat);
    }

    public void endService(SessionType serviceType, byte sessionID) {
        if(serviceType.equals(SessionType.RPC)){ //RPC session will close all other sessions so we want to make sure we use the correct EndProtocolSession method
            endSession(sessionID,hashID);
        }else {
            SdlPacket header = SdlPacketFactory.createEndSession(serviceType, sessionID, hashID, (byte)protocolVersion.getMajor(), new byte[0]);
            TransportRecord transportRecord = activeTransports.get(serviceType);
            if(transportRecord != null){
                header.setTransportRecord(transportRecord);
                handlePacketToSend(header);
            }
        }
    }

    /* --------------------------------------------------------------------------------------------
       -----------------------------------   OLD ABSTRACT PROTOCOL   ---------------------------------
       -------------------------------------------------------------------------------------------*/


    // This method is called whenever a protocol has an entire frame to send
    /**
     * SdlPacket should have included payload at this point.
     * @param packet packet that will be sent to the router service
     */
    protected void handlePacketToSend(SdlPacket packet) {
        synchronized(FRAME_LOCK) {

            if(packet!=null){
                iSdlProtocol.onProtocolMessageBytesToSend(packet);
            }

        }
    }

    /** This method handles the end of a protocol session. A callback is
     * sent to the protocol listener.
     **/
    protected void handleServiceEndedNAK(SdlPacket packet, SessionType serviceType) {
        if(packet.version >= 5){
            if(DebugTool.isDebugEnabled()) {
                //Currently this is only during a debugging session. Might pass back in the future
                String rejectedTag = null;
                if (serviceType.equals(SessionType.RPC)) {
                    rejectedTag = ControlFrameTags.RPC.EndServiceNAK.REJECTED_PARAMS;
                } else if (serviceType.equals(SessionType.PCM)) {
                    rejectedTag = ControlFrameTags.Audio.EndServiceNAK.REJECTED_PARAMS;
                } else if (serviceType.equals(SessionType.NAV)) {
                    rejectedTag = ControlFrameTags.Video.EndServiceNAK.REJECTED_PARAMS;
                }

                List<String> rejectedParams = (List<String>) packet.getTag(rejectedTag);
                if(rejectedParams != null && rejectedParams.size() > 0){
                    StringBuilder builder = new StringBuilder();
                    builder.append("Rejected params for service type ");
                    builder.append(serviceType.getName());
                    builder.append(" :");
                    for(String rejectedParam : rejectedParams){
                        builder.append(rejectedParam);
                        builder.append(" ");
                    }
                    DebugTool.logWarning(builder.toString());
                }

            }
        }

        iSdlProtocol.onProtocolSessionEndedNACKed(serviceType, (byte)packet.getSessionId(), "");
    }

    // This method handles the end of a protocol session. A callback is
    // sent to the protocol listener.
    protected void handleServiceEnded(SdlPacket packet, SessionType sessionType) {

        iSdlProtocol.onProtocolSessionEnded(sessionType, (byte)packet.getSessionId(), "");

    }

    /**
     * This method handles the startup of a protocol session. A callback is sent
     * to the protocol listener.
     * @param packet StarServiceACK packet
     * @param serviceType the service type that has just been started
     */
    protected void handleProtocolSessionStarted(SdlPacket packet, SessionType serviceType) {
        // Use this sessionID to create a message lock
        Object messageLock = _messageLocks.get((byte)packet.getSessionId());
        if (messageLock == null) {
            messageLock = new Object();
            _messageLocks.put((byte)packet.getSessionId(), messageLock);
        }
        if(packet.version >= 5){
            String mtuTag = null;
            if(serviceType.equals(SessionType.RPC)){
                mtuTag = ControlFrameTags.RPC.StartServiceACK.MTU;
            }else if(serviceType.equals(SessionType.PCM)){
                mtuTag = ControlFrameTags.Audio.StartServiceACK.MTU;
            }else if(serviceType.equals(SessionType.NAV)){
                mtuTag = ControlFrameTags.Video.StartServiceACK.MTU;
            }
            Object mtu = packet.getTag(mtuTag);
            if(mtu!=null){
                mtus.put(serviceType,(Long) packet.getTag(mtuTag));
            }
            if(serviceType.equals(SessionType.RPC)){
                hashID = (Integer) packet.getTag(ControlFrameTags.RPC.StartServiceACK.HASH_ID);
                Object version = packet.getTag(ControlFrameTags.RPC.StartServiceACK.PROTOCOL_VERSION);

                if(version!=null) {
                    //At this point we have confirmed the negotiated version between the module and the proxy
                    protocolVersion = new Version((String) version);
                }else{
                    protocolVersion = new Version("5.0.0");
                }

                //Check to make sure this is a transport we are willing to accept
                TransportRecord transportRecord = packet.getTransportRecord();

                if(transportRecord == null || !requestedPrimaryTransports.contains(transportRecord.getType())){
                    onTransportNotAccepted("Transport is not in requested primary transports");
                    return;
                }


                // This enables custom behavior based on protocol version specifics
                if (protocolVersion.isNewerThan(new Version("5.1.0")) >= 0) {

                    if (activeTransports.get(SessionType.RPC) == null) {    //Might be a better way to handle this

                        ArrayList<String> secondary = (ArrayList<String>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.SECONDARY_TRANSPORTS);
                        audio = (ArrayList<Integer>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.AUDIO_SERVICE_TRANSPORTS);
                        video = (ArrayList<Integer>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.VIDEO_SERVICE_TRANSPORTS);

                        activeTransports.put(SessionType.RPC, transportRecord);
                        activeTransports.put(SessionType.BULK_DATA, transportRecord);
                        activeTransports.put(SessionType.CONTROL, transportRecord);

                        //Build out the supported secondary transports received from the
                        // RPC start service ACK.
                        supportedSecondaryTransports = new ArrayList<>();
                        if (secondary == null) {
                            // If no secondary transports were attached we should assume
                            // the Video and Audio services can be used on primary
                            if (requiresHighBandwidth
                                    && TransportType.BLUETOOTH.equals(transportRecord.getType())) {
                                //transport can't support high bandwidth
                                onTransportNotAccepted(transportRecord.getType() + " can't support high bandwidth requirement, and secondary transport not supported.");
                                return;
                            }

                            activeTransports.put(SessionType.NAV, transportRecord);
                            activeTransports.put(SessionType.PCM, transportRecord);
                        }else{

                            if(DebugTool.isDebugEnabled()){
                                printSecondaryTransportDetails(secondary,audio,video);
                            }
                            for (String s : secondary) {
                                switch (s) {
                                    case TransportConstants.TCP_WIFI:
                                        supportedSecondaryTransports.add(TransportType.TCP);
                                        break;
                                    case TransportConstants.AOA_USB:
                                        supportedSecondaryTransports.add(TransportType.USB);
                                        break;
                                    case TransportConstants.SPP_BLUETOOTH:
                                        supportedSecondaryTransports.add(TransportType.BLUETOOTH);
                                        break;
                                }
                            }


                            sendTransportNotification();

                        }

                        setTransportPriorityForService(SessionType.PCM, audio);
                        setTransportPriorityForService(SessionType.NAV, video);

                    } else {
                        Log.w(TAG, "Received a start service ack for RPC service while already active on a different transport.");
                        return;
                    }
                }else {

                    //Version is either not included or lower than 5.1.0
                    if (requiresHighBandwidth
                            && TransportType.BLUETOOTH.equals(transportRecord.getType())) {
                        //transport can't support high bandwidth
                        onTransportNotAccepted(transportRecord.getType() + " can't support high bandwidth requirement, and secondary transport not supported in this protocol version: " + version);
                        return;
                    }

                    activeTransports.put(SessionType.RPC, transportRecord);
                    activeTransports.put(SessionType.BULK_DATA, transportRecord);
                    activeTransports.put(SessionType.CONTROL, transportRecord);
                    activeTransports.put(SessionType.NAV, transportRecord);
                    activeTransports.put(SessionType.PCM, transportRecord);
                }


            }else if(serviceType.equals(SessionType.NAV)){
                if(iSdlProtocol != null) {
                    ImageResolution acceptedResolution = new ImageResolution();
                    VideoStreamingFormat acceptedFormat = new VideoStreamingFormat();
                    acceptedResolution.setResolutionHeight((Integer) packet.getTag(ControlFrameTags.Video.StartServiceACK.HEIGHT));
                    acceptedResolution.setResolutionWidth((Integer) packet.getTag(ControlFrameTags.Video.StartServiceACK.WIDTH));
                    acceptedFormat.setCodec(VideoStreamingCodec.valueForString((String) packet.getTag(ControlFrameTags.Video.StartServiceACK.VIDEO_CODEC)));
                    acceptedFormat.setProtocol(VideoStreamingProtocol.valueForString((String) packet.getTag(ControlFrameTags.Video.StartServiceACK.VIDEO_PROTOCOL)));
                    VideoStreamingParameters agreedVideoParams = iSdlProtocol.getDesiredVideoParams();
                    agreedVideoParams.setResolution(acceptedResolution);
                    agreedVideoParams.setFormat(acceptedFormat);
                    iSdlProtocol.setAcceptedVideoParams(agreedVideoParams);
                }
            }
        } else {
            TransportRecord transportRecord = packet.getTransportRecord();
            if(transportRecord == null || (requiresHighBandwidth
                    && TransportType.BLUETOOTH.equals(transportRecord.getType()))){
                //transport can't support high bandwidth
                onTransportNotAccepted((transportRecord != null ? transportRecord.getType().toString() : "Transport") + "can't support high bandwidth requirement, and secondary transport not supported in this protocol version");
                return;
            }
            //If version < 5 and transport is acceptable we need to just add these
            activeTransports.put(SessionType.RPC, transportRecord);
            activeTransports.put(SessionType.BULK_DATA, transportRecord);
            activeTransports.put(SessionType.CONTROL, transportRecord);
            activeTransports.put(SessionType.NAV, transportRecord);
            activeTransports.put(SessionType.PCM, transportRecord);

            if (protocolVersion.getMajor() > 1){
                if (packet.payload!= null && packet.dataSize == 4){ //hashid will be 4 bytes in length
                    hashID = BitConverter.intFromByteArray(packet.payload, 0);
                }
            }
        }

        iSdlProtocol.onProtocolSessionStarted(serviceType, (byte) packet.getSessionId(), (byte)protocolVersion.getMajor(), "", hashID, packet.isEncrypted());
    }

    protected void handleProtocolSessionNAKed(SdlPacket packet, SessionType serviceType) {
        List<String> rejectedParams = null;
        if(packet.version >= 5){
            if(DebugTool.isDebugEnabled()) {
                //Currently this is only during a debugging session. Might pass back in the future
                String rejectedTag = null;
                if (serviceType.equals(SessionType.RPC)) {
                    rejectedTag = ControlFrameTags.RPC.StartServiceNAK.REJECTED_PARAMS;
                } else if (serviceType.equals(SessionType.PCM)) {
                    rejectedTag = ControlFrameTags.Audio.StartServiceNAK.REJECTED_PARAMS;
                } else if (serviceType.equals(SessionType.NAV)) {
                    rejectedTag = ControlFrameTags.Video.StartServiceNAK.REJECTED_PARAMS;
                }

                rejectedParams = (List<String>) packet.getTag(rejectedTag);
                if(rejectedParams != null && rejectedParams.size() > 0){
                    StringBuilder builder = new StringBuilder();
                    builder.append("Rejected params for service type ");
                    builder.append(serviceType.getName());
                    builder.append(" :");
                    for(String rejectedParam : rejectedParams){
                        builder.append(rejectedParam);
                        builder.append(" ");
                    }
                    DebugTool.logWarning(builder.toString());
                }

            }
        }
        if (serviceType.eq(SessionType.NAV) || serviceType.eq(SessionType.PCM)) {
            //handleProtocolSessionNACKed(serviceType, (byte)packet.getSessionId(), getMajorVersionByte(), "", rejectedParams);
            iSdlProtocol.onProtocolSessionNACKed(serviceType, (byte)packet.sessionId, (byte)protocolVersion.getMajor(), "", rejectedParams);

        } else {
            handleProtocolError("Got StartSessionNACK for protocol sessionID = " + packet.getSessionId(), null);
        }
    }

    // This method handles protocol errors. A callback is sent to the protocol
    // listener.
    protected void handleProtocolError(String string, Exception ex) {
        iSdlProtocol.onProtocolError(string, ex);
    }

    protected void handleProtocolHeartbeat(SessionType sessionType, byte sessionID) {
        sendHeartBeatACK(sessionType,sessionID);
    }

    protected void handleServiceDataACK(SdlPacket packet, SessionType sessionType) {

        if (packet.getPayload() != null && packet.getDataSize() == 4){ //service data ack will be 4 bytes in length
            int serviceDataAckSize = BitConverter.intFromByteArray(packet.getPayload(), 0);
            iSdlProtocol.onProtocolServiceDataACK(sessionType, serviceDataAckSize, (byte)packet.getSessionId ());

        }
    }

    private void sendTransportNotification (){
        boolean isMediaSupported = false;
        List<TransportType> connectedPrimaryTransports = new ArrayList<>();
        List<TransportType> connectedSecondaryTransports = new ArrayList<>();

        if (connectedTransports != null && audio != null && video != null){

            // Get a list of connected primary transports and list of connected secondary transports
            for (TransportRecord transportRecord : connectedTransports){
                if(supportedSecondaryTransports != null && supportedSecondaryTransports.contains(transportRecord.getType())){
                    connectedSecondaryTransports.add(transportRecord.getType());
                } else {
                    connectedPrimaryTransports.add(transportRecord.getType());
                }
            }

            // Check if there is at least on transport that supports media
            if ( (audio.contains(PRIMARY_TRANSPORT_ID) && video.contains(PRIMARY_TRANSPORT_ID) && !connectedPrimaryTransports.isEmpty())
                    || (audio.contains(SECONDARY_TRANSPORT_ID) && video.contains(SECONDARY_TRANSPORT_ID) && !connectedSecondaryTransports.isEmpty()) ){
                isMediaSupported = true;
            }


            // send notifactin
            Log.i("Bilalo89", "connectedTransports: " + connectedTransports);
            Log.i("Bilalo89", "supportedSecondaryTransports: " + supportedSecondaryTransports);
            Log.i("Bilalo89", "audio: " + audio);
            Log.i("Bilalo89", "video: " + video);


            Log.i("Bilalo89", "connectedPrimaryTransports: " + connectedPrimaryTransports);
            Log.i("Bilalo89", "connectedSecondaryTransports: " + connectedSecondaryTransports);
            Log.i("Bilalo89", "isMediaSupported: " + isMediaSupported);
        }

    }

    /* --------------------------------------------------------------------------------------------
       -----------------------------------   TRANSPORT_TYPE LISTENER   ---------------------------------
       -------------------------------------------------------------------------------------------*/

    @SuppressWarnings("FieldCanBeLocal")
    private final TransportManager.TransportEventListener transportEventListener = new TransportManager.TransportEventListener() {
        private boolean requestedSession = false;

        @Override
        public void onPacketReceived(SdlPacket packet) {
            handlePacketReceived(packet);
        }

        @Override
        public void onTransportConnected(List<TransportRecord> connectedTransports) {
            Log.d(TAG, "onTransportConnected");

            SdlProtocol.this.connectedTransports = connectedTransports;
            sendTransportNotification();

            //In the future we should move this logic into the Protocol Layer
            TransportRecord transportRecord = getTransportForSession(SessionType.RPC);
            if(transportRecord == null && !requestedSession){ //There is currently no transport registered
                requestedSession = true;
                transportManager.requestNewSession(getPreferredPrimaryTransport(connectedTransports));
            }
            onTransportsConnectedUpdate(connectedTransports);
            if(DebugTool.isDebugEnabled()){
                printActiveTransports();
            }
        }

        @Override
        public void onTransportDisconnected(String info, TransportRecord disconnectedTransport, List<TransportRecord> connectedTransports) {

            SdlProtocol.this.connectedTransports = connectedTransports;
            sendTransportNotification();


            if (disconnectedTransport == null) {
                Log.d(TAG, "onTransportDisconnected");
                transportManager.close(iSdlProtocol.getSessionId());
                iSdlProtocol.shutdown("No transports left connected");
                return;
            } else {
                Log.d(TAG, "onTransportDisconnected - " + disconnectedTransport.getType().name());
            }

            //In the future we will actually compare the record but at this point we can assume only
            //a single transport record per transport.
            //TransportType type = disconnectedTransport.getType();
            if(disconnectedTransport.equals(getTransportForSession(SessionType.NAV))){
                //stopVideoStream();
                iSdlProtocol.stopStream(SessionType.NAV);
            }
            if(disconnectedTransport.equals(getTransportForSession(SessionType.PCM))){
                //stopAudioStream();
                iSdlProtocol.stopStream(SessionType.PCM);
            }

            if(disconnectedTransport.equals(getTransportForSession(SessionType.RPC))){
                //transportTypes.remove(type);
                boolean primaryTransportAvailable = false;
                if(requestedPrimaryTransports != null && requestedPrimaryTransports.size() > 1){
                    for (TransportType transportType: requestedPrimaryTransports){ Log.d(TAG, "Checking " + transportType.name());
                        if(!disconnectedTransport.getType().equals(transportType)
                                && transportManager != null
                                && transportManager.isConnected(transportType,null)){
                            primaryTransportAvailable = true;
                            ( transportConfig).setService(transportManager.getRouterService());
                            break;
                        }
                    }
                }
                transportManager.close(iSdlProtocol.getSessionId());
                transportManager = null;
                requestedSession = false;

                iSdlProtocol.onTransportDisconnected(info, primaryTransportAvailable, transportConfig);

            } //else Transport was not primary, continuing to stay connected

        }

        @Override
        public void onError(String info) {
            iSdlProtocol.shutdown(info);

        }

        @Override
        public boolean onLegacyModeEnabled(String info) {
            //Await a connection from the legacy transport
            if(requestedPrimaryTransports!= null && requestedPrimaryTransports.contains(TransportType.BLUETOOTH)
                    && !transportConfig.requiresHighBandwidth()){
                Log.d(TAG, "Entering legacy mode; creating new protocol instance");
                reset();
                return true;
            }else{
                Log.d(TAG, "Bluetooth is not an acceptable transport; not moving to legacy mode");
                return false;
            }
        }
    };

/* -------------------------------------------------------------------------------------------------
-----------------------------------   Internal Classes    ------------------------------------------
--------------------------------------------------------------------------------------------------*/


    protected class MessageFrameAssembler {
        protected ByteArrayOutputStream accumulator = null;
        protected int totalSize = 0;

        protected void handleFirstDataFrame(SdlPacket packet) {
            //The message is new, so let's figure out how big it is.
            totalSize = BitConverter.intFromByteArray(packet.payload, 0) - headerSize;
            try {
                accumulator = new ByteArrayOutputStream(totalSize);
            }catch(OutOfMemoryError e){
                DebugTool.logError("OutOfMemory error", e); //Garbled bits were received
                accumulator = null;
            }
        }

        protected void handleRemainingFrame(SdlPacket packet) {
            accumulator.write(packet.payload, 0, (int)packet.getDataSize());
            notifyIfFinished(packet);
        }

        protected void notifyIfFinished(SdlPacket packet) {
            if (packet.getFrameType() == FrameType.Consecutive && packet.getFrameInfo() == 0x0) {
                ProtocolMessage message = new ProtocolMessage();
                message.setPayloadProtected(packet.isEncrypted());
                message.setSessionType(SessionType.valueOf((byte)packet.getServiceType()));
                message.setSessionID((byte)packet.getSessionId());
                //If it is WiPro 2.0 it must have binary header
                if (protocolVersion.getMajor() > 1) {
                    BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
                            parseBinaryHeader(accumulator.toByteArray());
                    if(binFrameHeader == null) {
                        return;
                    }
                    message.setVersion((byte)protocolVersion.getMajor());
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
                    iSdlProtocol.onProtocolMessageReceived(message);
                } catch (Exception excp) {
                    DebugTool.logError(FailurePropagating_Msg + "onProtocolMessageReceived: " + excp.toString(), excp);
                } // end-catch

                accumulator = null;
            } // end-if
        } // end-method

        protected void handleMultiFrameMessageFrame(SdlPacket packet) {
            if (packet.getFrameType() == FrameType.First) {
                handleFirstDataFrame(packet);
            }
            else{
                if(accumulator != null){
                    handleRemainingFrame(packet);
                }
            }

        } // end-method

        protected void handleFrame(SdlPacket packet) {

            if (packet.getPayload() != null && packet.getDataSize() > 0 && packet.isEncrypted() ) {

                SdlSecurityBase sdlSec = iSdlProtocol.getSdlSecurity();
                byte[] dataToRead = new byte[4096];

                Integer iNumBytes = sdlSec.decryptData(packet.getPayload(), dataToRead);
                if ((iNumBytes == null) || (iNumBytes <= 0)){
                    return;
                }

                byte[] decryptedData = new byte[iNumBytes];
                System.arraycopy(dataToRead, 0, decryptedData, 0, iNumBytes);
                packet.payload = decryptedData;
            }

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
            }
        }

        private void handleProtocolHeartbeatACK(SdlPacket packet) {
            //Heartbeat is not supported in the SdlProtocol class beyond responding with ACKs to
            //heartbeat messages. Receiving this ACK is suspicious and should be logged
            DebugTool.logInfo("Received HeartbeatACK - " + packet.toString());
        }

        private void handleProtocolHeartbeat(SdlPacket packet) {
            SdlProtocol.this.handleProtocolHeartbeat(SessionType.valueOf((byte)packet.getServiceType()),(byte)packet.getSessionId());
        }

        /**
         * Directing method that will push the packet to the method that can handle it best
         * @param packet a control frame packet
         */
        private void handleControlFrame(SdlPacket packet) {
            Integer frameTemp = packet.getFrameInfo();
            Byte frameInfo = frameTemp.byteValue();

            SessionType serviceType = SessionType.valueOf((byte)packet.getServiceType());

            if (frameInfo == FrameDataControlFrameType.Heartbeat.getValue()) {

                handleProtocolHeartbeat(packet);

            }else if (frameInfo == FrameDataControlFrameType.HeartbeatACK.getValue()) {

                handleProtocolHeartbeatACK(packet);

            }else if (frameInfo == FrameDataControlFrameType.StartSessionACK.getValue()) {

                handleProtocolSessionStarted(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.StartSessionNACK.getValue()) {

                handleProtocolSessionNAKed(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.EndSession.getValue()
                    || frameInfo == FrameDataControlFrameType.EndSessionACK.getValue()) {

                handleServiceEnded(packet,serviceType);

            } else if (frameInfo == FrameDataControlFrameType.EndSessionNACK.getValue()) {

                handleServiceEndedNAK(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.ServiceDataACK.getValue()) {

                handleServiceDataACK(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.RegisterSecondaryTransportACK.getValue()) {

                handleSecondaryTransportRegistration(packet.getTransportRecord(),true);

            } else if (frameInfo == FrameDataControlFrameType.RegisterSecondaryTransportNACK.getValue()) {

                String reason = (String) packet.getTag(ControlFrameTags.RPC.RegisterSecondaryTransportNAK.REASON);
                DebugTool.logWarning(reason);
                handleSecondaryTransportRegistration(packet.getTransportRecord(),false);

            } else if (frameInfo == FrameDataControlFrameType.TransportEventUpdate.getValue()) {

                // Get TCP params
                String ipAddr = (String) packet.getTag(ControlFrameTags.RPC.TransportEventUpdate.TCP_IP_ADDRESS);
                Integer port = (Integer) packet.getTag(ControlFrameTags.RPC.TransportEventUpdate.TCP_PORT);

                if(secondaryTransportParams == null){
                    secondaryTransportParams = new HashMap<>();
                }

                if(ipAddr != null && port != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(ControlFrameTags.RPC.TransportEventUpdate.TCP_IP_ADDRESS, ipAddr);
                    bundle.putInt(ControlFrameTags.RPC.TransportEventUpdate.TCP_PORT, port);
                    bundle.putString(TransportConstants.TRANSPORT_TYPE, TransportType.TCP.name());
                    secondaryTransportParams.put(TransportType.TCP, bundle);
                }

            }

            _assemblerForMessageID.remove(packet.getMessageId());

        } // end-method

        private void handleSingleFrameMessageFrame(SdlPacket packet) {
            ProtocolMessage message = new ProtocolMessage();
            message.setPayloadProtected(packet.isEncrypted());
            SessionType serviceType = SessionType.valueOf((byte)packet.getServiceType());
            if (serviceType == SessionType.RPC) {
                message.setMessageType(MessageType.RPC);
            } else if (serviceType == SessionType.BULK_DATA) {
                message.setMessageType(MessageType.BULK);
            } // end-if
            message.setSessionType(serviceType);
            message.setSessionID((byte)packet.getSessionId());
            //If it is WiPro 2.0 it must have binary header
            boolean isControlService = message.getSessionType().equals(SessionType.CONTROL);
            if (protocolVersion.getMajor() > 1 && !isControlService) {
                BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
                        parseBinaryHeader(packet.payload);
                if(binFrameHeader == null) {
                    return;
                }
                message.setVersion((byte)protocolVersion.getMajor());
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
                iSdlProtocol.onProtocolMessageReceived(message);
            } catch (Exception ex) {
                DebugTool.logError(FailurePropagating_Msg + "onProtocolMessageReceived: " + ex.toString(), ex);
                handleProtocolError(FailurePropagating_Msg + "onProtocolMessageReceived: ", ex);
            } // end-catch
        } // end-method
    } // end-class


}
