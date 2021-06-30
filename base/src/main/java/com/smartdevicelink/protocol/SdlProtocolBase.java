/*
 * Copyright (c) 2019 Livio, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Livio Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.smartdevicelink.protocol;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

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
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.TransportConstants;
import com.smartdevicelink.transport.TransportManagerBase;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.TransportRecord;
import com.smartdevicelink.util.BitConverter;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.Version;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SdlProtocolBase {
    private static final String TAG = "SdlProtocol";
    private final static String FailurePropagating_Msg = "Failure propagating ";

    private static final int TLS_MAX_RECORD_SIZE = 16384;
    private final static int TLS_RECORD_HEADER_SIZE = 5;
    private final static int TLS_RECORD_MES_AUTH_CDE_SIZE = 32;
    private final static int TLS_MAX_RECORD_PADDING_SIZE = 256;
    private final static int TLS_MAX_DATA_TO_ENCRYPT_SIZE = TLS_MAX_RECORD_SIZE - TLS_RECORD_HEADER_SIZE - TLS_RECORD_MES_AUTH_CDE_SIZE - TLS_MAX_RECORD_PADDING_SIZE;

    private static final int PRIMARY_TRANSPORT_ID = 1;
    private static final int SECONDARY_TRANSPORT_ID = 2;

    /**
     * Original header size based on version 1.0.0 only
     */
    public static final int V1_HEADER_SIZE = 8;
    /**
     * Larger header size that is used by versions 2.0.0 and up
     */
    public static final int V2_HEADER_SIZE = 12;

    //If increasing MAX PROTOCOL VERSION major version, make sure to alter it in SdlPsm
    private static final Version MAX_PROTOCOL_VERSION = new Version(5, 4, 0);

    public static final int V1_V2_MTU_SIZE = 1500;
    public static final int V3_V4_MTU_SIZE = 131072;

    private static final List<SessionType> HIGH_BANDWIDTH_SERVICES
            = Arrays.asList(SessionType.NAV, SessionType.PCM);

    // Lock to ensure all frames are sent uninterrupted
    private final Object FRAME_LOCK = new Object();

    private final Object TRANSPORT_MANAGER_LOCK = new Object();

    private final ISdlProtocol iSdlProtocol;
    private final Hashtable<Integer, SdlProtocol.MessageFrameAssembler> _assemblerForMessageID = new Hashtable<>();
    private final Hashtable<Byte, Object> _messageLocks = new Hashtable<>();
    private final HashMap<SessionType, Long> mtus = new HashMap<>();
    private final HashMap<SessionType, TransportRecord> activeTransports = new HashMap<>();
    private final HashMap<SessionType, Boolean> serviceStartedOnTransport = new HashMap<>();
    private final Map<TransportType, List<ISecondaryTransportListener>> secondaryTransportListeners = new HashMap<>();


    private Version protocolVersion = new Version("1.0.0");
    private int hashID = 0;
    private int messageID = 0;
    private int headerSize = V1_HEADER_SIZE;

    /**
     * Transport config that this protocol instance should use along with the supplied transport manager
     */
    final BaseTransportConfig transportConfig;

    /**
     * The transport manager used for this protocol instance.
     */
    TransportManagerBase transportManager;


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
    Map<TransportType, TransportRecord> secondaryTransportParams;
    TransportRecord connectedPrimaryTransport;


    public SdlProtocolBase(@NonNull ISdlProtocol iSdlProtocol, @NonNull BaseTransportConfig config) {
        if (iSdlProtocol == null) {
            throw new IllegalArgumentException("Provided protocol listener interface reference is null");
        } // end-if

        this.iSdlProtocol = iSdlProtocol;
        this.transportConfig = config;
        if (!config.getTransportType().equals(TransportType.MULTIPLEX)) {
            this.requestedPrimaryTransports = Collections.singletonList(transportConfig.getTransportType());
            this.requestedSecondaryTransports = Collections.emptyList();
            this.requiresHighBandwidth = false;
        }
        mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));

    } // end-ctor

    void setTransportManager(TransportManagerBase transportManager) {
        synchronized (TRANSPORT_MANAGER_LOCK) {
            this.transportManager = transportManager;
        }
    }

    public void start() {
        synchronized (TRANSPORT_MANAGER_LOCK) {
            if (transportManager == null) {
                throw new IllegalStateException("Attempting to start without setting a transport manager.");
            }
            transportManager.start();
        }
    }

    /**
     * Retrieves the max payload size for a packet to be sent to the module
     *
     * @return the max transfer unit
     */
    public int getMtu() {
        return Long.valueOf(getMtu(SessionType.RPC)).intValue();
    }

    public long getMtu(SessionType type) {
        Long mtu = mtus.get(type);
        if (mtu == null) {
            mtu = mtus.get(SessionType.RPC);
        }
        if (mtu == null) { //If MTU is still null, use the oldest/smallest
            mtu = (long) V1_V2_MTU_SIZE;
        }
        return mtu;
    }

    @Deprecated
    public void resetSession() {
    }

    public boolean isConnected() {
        synchronized (TRANSPORT_MANAGER_LOCK) {
            return transportManager != null && transportManager.isConnected(null, null);
        }
    }

    /**
     * Resets the protocol to init status
     */
    protected void reset() {
        protocolVersion = new Version("1.0.0");
        hashID = 0;
        messageID = 0;
        headerSize = V1_HEADER_SIZE;
        this.activeTransports.clear();
        this.serviceStartedOnTransport.clear();
        this.mtus.clear();
        mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        this.secondaryTransportParams = null;
        this._assemblerForMessageID.clear();
        this._messageLocks.clear();
    }

    /**
     * For logging purposes, prints active services on each connected transport
     */
    protected void printActiveTransports() {
        StringBuilder activeTransportString = new StringBuilder();
        activeTransportString.append("Active transports --- \n");

        for (Map.Entry entry : activeTransports.entrySet()) {
            String sessionString = null;
            if (entry.getKey().equals(SessionType.NAV)) {
                sessionString = "NAV";
            } else if (entry.getKey().equals(SessionType.PCM)) {
                sessionString = "PCM";
            } else if (entry.getKey().equals(SessionType.RPC)) {
                sessionString = "RPC";
            }
            if (sessionString != null) {
                activeTransportString.append("Session: ");

                activeTransportString.append(sessionString);
                activeTransportString.append(" Transport: ");
                activeTransportString.append(entry.getValue().toString());
                activeTransportString.append("\n");
            }
        }
        DebugTool.logInfo(TAG, activeTransportString.toString());
    }

    protected void printSecondaryTransportDetails(List<String> secondary, List<Integer> audio, List<Integer> video) {
        StringBuilder secondaryDetailsBldr = new StringBuilder();
        secondaryDetailsBldr.append("Checking secondary transport details \n");

        if (secondary != null) {
            secondaryDetailsBldr.append("Supported secondary transports: ");
            for (String s : secondary) {
                secondaryDetailsBldr.append(" ").append(s);
            }
            secondaryDetailsBldr.append("\n");
        } else {
            DebugTool.logInfo(TAG, "Supported secondary transports list is empty!");
        }
        if (audio != null) {
            secondaryDetailsBldr.append("Supported audio transports: ");
            for (int a : audio) {
                secondaryDetailsBldr.append(" ").append(a);
            }
            secondaryDetailsBldr.append("\n");
        }
        if (video != null) {
            secondaryDetailsBldr.append("Supported video transports: ");
            for (int v : video) {
                secondaryDetailsBldr.append(" ").append(v);
            }
            secondaryDetailsBldr.append("\n");
        }

        DebugTool.logInfo(TAG, secondaryDetailsBldr.toString());
    }


    private TransportRecord getTransportForSession(SessionType type) {
        return activeTransports.get(type);
    }

    private void setTransportPriorityForService(SessionType serviceType, List<Integer> order) {
        if (transportPriorityForServiceMap == null) {
            transportPriorityForServiceMap = new HashMap<>();
        }
        this.transportPriorityForServiceMap.put(serviceType, order);
        for (SessionType service : HIGH_BANDWIDTH_SERVICES) {
            if (transportPriorityForServiceMap.get(service) != null
                    && transportPriorityForServiceMap.get(service).contains(PRIMARY_TRANSPORT_ID)) {
                if (connectedPrimaryTransport != null) {
                    activeTransports.put(service, connectedPrimaryTransport);
                }
            }
        }
    }

    /**
     * Handles when a secondary transport can be used to start services on or when the request as failed.
     *
     * @param transportRecord the transport type that the event has taken place on
     * @param registered      if the transport was successfully registered on
     */
    private void handleSecondaryTransportRegistration(TransportRecord transportRecord, boolean registered) {
        if (registered) {
            //Session has been registered on secondary transport
            DebugTool.logInfo(TAG, transportRecord.getType().toString() + " transport was registered!");
            if (supportedSecondaryTransports.contains(transportRecord.getType())) {
                // If the transport type that is now available to be used it should be checked
                // against the list of services that might be able to be started on it

                for (SessionType secondaryService : HIGH_BANDWIDTH_SERVICES) {
                    if (transportPriorityForServiceMap.containsKey(secondaryService)) {
                        // If this service type has extra information from the RPC StartServiceACK
                        // parse through it to find which transport should be used to start this
                        // specific service type
                        List<Integer> transportNumList = transportPriorityForServiceMap.get(secondaryService);
                        if (transportNumList != null) {
                            for (int transportNum : transportNumList) {
                                if (transportNum == PRIMARY_TRANSPORT_ID) {
                                    break; // Primary is favored for this service type, break out...
                                } else if (transportNum == SECONDARY_TRANSPORT_ID) {
                                    // The secondary transport can be used to start this service
                                    activeTransports.put(secondaryService, transportRecord);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            DebugTool.logInfo(TAG, transportRecord.toString() + " transport was NOT registered!");
        }
        //Notify any listeners for this secondary transport
        List<ISecondaryTransportListener> listenerList = secondaryTransportListeners.remove(transportRecord.getType());
        if (listenerList != null) {
            for (ISecondaryTransportListener listener : listenerList) {
                if (registered) {
                    listener.onConnectionSuccess(transportRecord);
                } else {
                    listener.onConnectionFailure();
                }
            }
        }

        if (DebugTool.isDebugEnabled()) {
            printActiveTransports();
        }
    }

    private void onTransportsConnectedUpdate(List<TransportRecord> transports) {
        //DebugTool.logInfo(TAG, "Connected transport update");

        //Temporary: this logic should all be changed to handle multiple transports of the same type
        ArrayList<TransportType> connectedTransports = new ArrayList<>();
        if (transports != null) {
            for (TransportRecord record : transports) {
                connectedTransports.add(record.getType());
            }
        }

        if (connectedPrimaryTransport != null && !connectedTransports.contains(connectedPrimaryTransport.getType())) {
            //The primary transport being used is no longer part of the connected transports
            //The transport manager callbacks should handle the disconnect code
            connectedPrimaryTransport = null;
            notifyDevTransportListener();
            return;
        }

        if (activeTransports.get(SessionType.RPC) == null) {
            //There is no currently active transport for the RPC service meaning no primary transport
            TransportRecord preferredPrimaryTransport = getPreferredTransport(requestedPrimaryTransports, transports);
            if (preferredPrimaryTransport != null) {
                connectedPrimaryTransport = preferredPrimaryTransport;
                startService(SessionType.RPC, (byte) 0x00, false);
            } else {
                onTransportNotAccepted("No transports match requested primary transport");
            }
            //Return to that the developer does not receive the transport callback at this time
            // as it is better to wait until the RPC service is registered and secondary transport
            //information is available
            return;
        } else if (secondaryTransportListeners != null
                && transports != null
                && iSdlProtocol != null) {
            // Check to see if there is a listener for a given transport.
            // If a listener exists, it can be assumed that the transport should be registered on
            for (TransportRecord record : transports) {
                if (secondaryTransportListeners.get(record.getType()) != null
                        && !secondaryTransportListeners.get(record.getType()).isEmpty()) {
                    registerSecondaryTransport((byte) iSdlProtocol.getSessionId(), record);
                }
            }
        }
        //Update the developer that a new transport has become available
        notifyDevTransportListener();
    }


    /**
     * Check to see if a transport is available to start/use the supplied service.
     *
     * @param serviceType the session that should be checked for transport availability
     * @return true if there is either a supported
     * transport currently connected or a transport is
     * available to connect with for the supplied service type.
     * <br>false if there is no
     * transport connected to support the service type in question and
     * no possibility in the foreseeable future.
     */
    public boolean isTransportForServiceAvailable(@NonNull SessionType serviceType) {
        if (connectedPrimaryTransport == null) {
            //If there is no connected primary then there is no transport available for any service
            return false;
        } else if (activeTransports != null && activeTransports.containsKey(serviceType)) {
            //There is an active transport that this service can be used on
            //This should catch RPC, Bulk, and Control service types
            return true;
        }

        if (transportPriorityForServiceMap != null) {
            List<Integer> transportPriority = transportPriorityForServiceMap.get(serviceType);

            if (transportPriority != null && !transportPriority.isEmpty()) {
                if (transportPriority.contains(PRIMARY_TRANSPORT_ID)) {
                    //If the transport priority for this service type contains primary then
                    // the service can be used/started
                    return true;
                } else if (transportPriority.contains(SECONDARY_TRANSPORT_ID)) {
                    //This would mean only secondary transport is supported for this service
                    return isSecondaryTransportAvailable(false);
                }
            }
        }

        //No transport priority for this service type
        if (connectedPrimaryTransport.getType() == TransportType.USB || connectedPrimaryTransport.getType() == TransportType.TCP) {
            //Since the only service type that should reach this point are ones that require a high
            //bandwidth, true can be returned if the primary transport is a high bandwidth transport
            return true;
        } else {
            //Since the only service type that should reach this point are ones that require a high
            //bandwidth, true can be returned if a secondary transport is a high bandwidth transport
            return isSecondaryTransportAvailable(true);
        }
    }

    /**
     * Checks to see if a secondary transport is available for this session
     *
     * @param onlyHighBandwidth if only high bandwidth transports should be included in this check
     * @return true if any connected or potential transport meets the criteria to be a secondary
     * transport
     */
    private boolean isSecondaryTransportAvailable(boolean onlyHighBandwidth) {
        if (supportedSecondaryTransports != null) {
            for (TransportType supportedSecondary : supportedSecondaryTransports) {
                if (!onlyHighBandwidth || supportedSecondary == TransportType.USB || supportedSecondary == TransportType.TCP) {
                    synchronized (TRANSPORT_MANAGER_LOCK) {
                        if (transportManager != null && transportManager.isConnected(supportedSecondary, null)) {
                            //A supported secondary transport is already connected
                            return true;
                        } else if (secondaryTransportParams != null && secondaryTransportParams.containsKey(supportedSecondary)) {
                            //A secondary transport is available to connect to
                            return true;
                        }
                    }
                }
            }
        }
        // No supported secondary transports
        return false;
    }


    /**
     * If the library allows for multiple transports per session this should be handled
     */
    void notifyDevTransportListener() {
        //Does nothing in base class
    }

    /**
     * Retrieves the preferred transport for the given connected transport
     *
     * @param preferredList       the list of preferred transports (primary or secondary)
     * @param connectedTransports the current list of connected transports
     * @return the preferred connected transport
     */
    private TransportRecord getPreferredTransport(List<TransportType> preferredList, List<TransportRecord> connectedTransports) {
        for (TransportType transportType : preferredList) {
            for (TransportRecord record : connectedTransports) {
                if (record.getType().equals(transportType)) {
                    return record;
                }
            }
        }
        return null;
    }

    private void onTransportNotAccepted(String info) {
        if (iSdlProtocol != null) {
            iSdlProtocol.shutdown(info);
        }
    }


    public Version getProtocolVersion() {
        return this.protocolVersion;
    }

    /**
     * This method will set the major protocol version that we should use. It will also set the default MTU based on version.
     *
     * @param version major version to use
     */
    protected void setVersion(byte version) {
        if (version > 5) {
            this.protocolVersion = new Version("5.0.0"); //protect for future, proxy only supports v5 or lower
            headerSize = V2_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE);
        } else if (version == 5) {
            this.protocolVersion = new Version("5.0.0");
            headerSize = V2_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE);
        } else if (version == 4) {
            this.protocolVersion = new Version("4.0.0");
            headerSize = V2_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE); //versions 4 supports 128k MTU
        } else if (version == 3) {
            this.protocolVersion = new Version("3.0.0");
            headerSize = V2_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) V3_V4_MTU_SIZE); //versions 3 supports 128k MTU
        } else if (version == 2) {
            this.protocolVersion = new Version("2.0.0");
            headerSize = V2_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        } else if (version == 1) {
            this.protocolVersion = new Version("1.0.0");
            headerSize = V1_HEADER_SIZE;
            mtus.put(SessionType.RPC, (long) (V1_V2_MTU_SIZE - headerSize));
        }
    }

    public void endSession(byte sessionID) {
        SdlPacket header = SdlPacketFactory.createEndSession(SessionType.RPC, sessionID, hashID, (byte) protocolVersion.getMajor(), hashID);
        handlePacketToSend(header);
        synchronized (TRANSPORT_MANAGER_LOCK) {
            if (transportManager != null) {
                transportManager.close(sessionID);
            }
        }
    } // end-method

    public void sendMessage(ProtocolMessage protocolMsg) {
        SessionType sessionType = protocolMsg.getSessionType();
        byte sessionID = protocolMsg.getSessionID();
        boolean requiresEncryption = protocolMsg.getPayloadProtected();
        SdlSecurityBase sdlSec = null;
        byte[] data;
        if (protocolVersion.getMajor() > 1 && sessionType != SessionType.NAV && sessionType != SessionType.PCM) {
            if (sessionType.eq(SessionType.CONTROL)) {
                final byte[] secureData = protocolMsg.getData().clone();
                data = new byte[headerSize + secureData.length];

                final BinaryFrameHeader binFrameHeader =
                        SdlPacketFactory.createBinaryFrameHeader(protocolMsg.getRPCType(), protocolMsg.getFunctionID(), protocolMsg.getCorrID(), 0);
                System.arraycopy(binFrameHeader.assembleHeaderBytes(), 0, data, 0, headerSize);
                System.arraycopy(secureData, 0, data, headerSize, secureData.length);
            } else if (protocolMsg.getBulkData() != null) {
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

        if (requiresEncryption) {
            if (iSdlProtocol == null) {
                DebugTool.logError(TAG, "Unable to encrypt packet, protocol callback was null");
                return;
            }
            sdlSec = iSdlProtocol.getSdlSecurity();
            if (sdlSec == null) {
                DebugTool.logError(TAG, "Unable to encrypt packet, security library not found");
                return;
            }
        }

        // Get the message lock for this protocol session
        Object messageLock = _messageLocks.get(sessionID);
        if (messageLock == null) {
            handleProtocolError("Error sending protocol message to SDL.",
                    new SdlException("Attempt to send protocol message prior to startSession ACK.", SdlExceptionCause.SDL_UNAVAILABLE));
            return;
        }

        //Set the MTU according to service MTU provided by the IVI .
        //If encryption is required the MTU will be set to lowest value between the max data size for encryption or service MTU
        final Long mtu = requiresEncryption ? Math.min(TLS_MAX_DATA_TO_ENCRYPT_SIZE, getMtu(sessionType)) : getMtu(sessionType);

        synchronized (messageLock) {
            if (data != null && data.length > mtu) {
                //Since the packet is larger than the mtu size, it will be sent as a multi-frame packet
                messageID++;

                // Assemble first frame.
                int frameCount = (int) Math.ceil(data.length / (double) mtu);

                byte[] firstFrameData = new byte[8];
                // First four bytes are data size.
                System.arraycopy(BitConverter.intToByteArray(data.length), 0, firstFrameData, 0, 4);
                // Second four bytes are frame count.
                System.arraycopy(BitConverter.intToByteArray(frameCount), 0, firstFrameData, 4, 4);

                //NOTE: First frames cannot be encrypted because their payloads need to be exactly 8 bytes
                SdlPacket firstHeader = SdlPacketFactory.createMultiSendDataFirst(sessionType, sessionID, messageID, (byte) protocolVersion.getMajor(), firstFrameData, false);
                firstHeader.setPriorityCoefficient(1 + protocolMsg.priorityCoefficient);
                firstHeader.setTransportRecord(activeTransports.get(sessionType));
                //Send the first frame
                handlePacketToSend(firstHeader);

                int currentOffset = 0;
                byte frameSequenceNumber = 0;

                byte[] dataBuffer, encryptedData;
                for (int i = 0; i < frameCount; i++) {

                    frameSequenceNumber++;

                    if (frameSequenceNumber == SdlPacket.FRAME_INFO_FINAL_CONNESCUTIVE_FRAME) {
                        //If sequence numbers roll over to 0, increment again to avoid
                        //using the reserved sequence value for the final frame
                        ++frameSequenceNumber;
                    }

                    if (i == frameCount - 1) {
                        frameSequenceNumber = SdlPacket.FRAME_INFO_FINAL_CONNESCUTIVE_FRAME;
                    }

                    int bytesToWrite = data.length - currentOffset;
                    if (bytesToWrite > mtu) {
                        bytesToWrite = mtu.intValue();
                    }

                    SdlPacket consecutiveFrame;
                    if (requiresEncryption) {
                        //Retrieve a chunk of the data into a temporary buffer to be encrypted
                        dataBuffer = new byte[bytesToWrite];
                        System.arraycopy(data, currentOffset, dataBuffer, 0, bytesToWrite);

                        encryptedData = new byte[TLS_MAX_RECORD_SIZE];
                        Integer numberOfBytesEncrypted = sdlSec.encryptData(dataBuffer, encryptedData);
                        if (numberOfBytesEncrypted == null || (numberOfBytesEncrypted <= 0)) {
                            DebugTool.logError(TAG, "Unable to encrypt data");
                            return;
                        }

                        consecutiveFrame = SdlPacketFactory.createMultiSendDataRest(sessionType, sessionID, numberOfBytesEncrypted, frameSequenceNumber, messageID, (byte) protocolVersion.getMajor(), encryptedData, 0, numberOfBytesEncrypted, true);
                    } else {
                        consecutiveFrame = SdlPacketFactory.createMultiSendDataRest(sessionType, sessionID, bytesToWrite, frameSequenceNumber, messageID, (byte) protocolVersion.getMajor(), data, currentOffset, bytesToWrite, false);
                    }

                    consecutiveFrame.setTransportRecord(activeTransports.get(sessionType));
                    consecutiveFrame.setPriorityCoefficient(i + 2 + protocolMsg.priorityCoefficient);
                    handlePacketToSend(consecutiveFrame);
                    currentOffset += bytesToWrite;
                }
            } else {
                messageID++;
                if (requiresEncryption && data != null && data.length > 0) {
                    //Encrypt the data before sending
                    byte[] encryptedData = new byte[TLS_MAX_RECORD_SIZE];
                    Integer numberOfBytesEncrypted = sdlSec.encryptData(data, encryptedData);
                    if (numberOfBytesEncrypted == null || (numberOfBytesEncrypted <= 0)) {
                        DebugTool.logError(TAG, "Unable to encrypt data");
                        return;
                    }
                    //Put the encrypted bytes back into the data array
                    data = new byte[numberOfBytesEncrypted];
                    System.arraycopy(encryptedData, 0, data, 0, numberOfBytesEncrypted);
                }
                int dataLength = data != null ? data.length : 0;
                SdlPacket header = SdlPacketFactory.createSingleSendData(sessionType, sessionID, dataLength, messageID, (byte) protocolVersion.getMajor(), data, requiresEncryption);
                header.setPriorityCoefficient(protocolMsg.priorityCoefficient);
                header.setTransportRecord(activeTransports.get(sessionType));
                handlePacketToSend(header);
            }
        }
    }

    protected void handlePacketReceived(SdlPacket packet) {
        //Check for a version difference
        if (protocolVersion == null || protocolVersion.getMajor() == 1) {
            setVersion((byte) packet.version);
        }

        SdlProtocolBase.MessageFrameAssembler assembler = getFrameAssemblerForFrame(packet);
        assembler.handleFrame(packet);

    }


    protected SdlProtocolBase.MessageFrameAssembler getFrameAssemblerForFrame(SdlPacket packet) {
        Integer iSessionId = packet.getSessionId();
        Byte bySessionId = iSessionId.byteValue();

        SdlProtocolBase.MessageFrameAssembler ret = _assemblerForMessageID.get(packet.getMessageId());
        if (ret == null) {
            ret = new SdlProtocolBase.MessageFrameAssembler();
            _assemblerForMessageID.put(packet.getMessageId(), ret);
        } // end-if

        return ret;
    } // end-method


    private void registerSecondaryTransport(byte sessionId, TransportRecord transportRecord) {
        SdlPacket header = SdlPacketFactory.createRegisterSecondaryTransport(sessionId, (byte) protocolVersion.getMajor());
        header.setTransportRecord(transportRecord);
        handlePacketToSend(header);
    }

    public void startService(SessionType serviceType, byte sessionID, boolean isEncrypted) {
        final SdlPacket header = SdlPacketFactory.createStartSession(serviceType, 0x00, (byte) protocolVersion.getMajor(), sessionID, isEncrypted);
        serviceStartedOnTransport.put(serviceType, true);
        if (SessionType.RPC.equals(serviceType)) {
            if (connectedPrimaryTransport != null) {
                header.setTransportRecord(connectedPrimaryTransport);
            }
            //This is going to be our primary transport
            header.putTag(ControlFrameTags.RPC.StartService.PROTOCOL_VERSION, MAX_PROTOCOL_VERSION.toString());
            handlePacketToSend(header);
            return; // We don't need to go any further
        } else if (serviceType.equals(SessionType.NAV)) {
            if (iSdlProtocol != null) {
                VideoStreamingParameters videoStreamingParameters = iSdlProtocol.getDesiredVideoParams();
                if (videoStreamingParameters != null) {
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
        if (transportPriorityForServiceMap == null
                || transportPriorityForServiceMap.get(serviceType) == null
                || transportPriorityForServiceMap.get(serviceType).isEmpty()) {
            //If there is no transport priority for this service it can be assumed it's primary
            header.setTransportRecord(connectedPrimaryTransport);
            handlePacketToSend(header);
            return;
        }
        int transportPriority = transportPriorityForServiceMap.get(serviceType).get(0);
        if (transportPriority == PRIMARY_TRANSPORT_ID) {
            // Primary is favored, and we're already connected...
            header.setTransportRecord(connectedPrimaryTransport);
            handlePacketToSend(header);
        } else if (transportPriority == SECONDARY_TRANSPORT_ID) {
            // Secondary is favored
            for (TransportType secondaryTransportType : supportedSecondaryTransports) {

                if (!requestedSecondaryTransports.contains(secondaryTransportType)) {
                    // Secondary transport is not accepted by the client
                    continue;
                }

                if (activeTransports.get(serviceType) != null
                        && activeTransports.get(serviceType).getType() != null
                        && activeTransports.get(serviceType).getType().equals(secondaryTransportType)) {
                    // Transport is already active and accepted
                    header.setTransportRecord(activeTransports.get(serviceType));
                    handlePacketToSend(header);
                    return;
                }


                //If the secondary transport isn't connected yet that will have to be performed first

                List<ISecondaryTransportListener> listenerList = secondaryTransportListeners.get(secondaryTransportType);
                if (listenerList == null) {
                    listenerList = new ArrayList<>();
                    secondaryTransportListeners.put(secondaryTransportType, listenerList);
                }

                //Check to see if the primary transport can also be used as a backup
                final boolean primaryTransportBackup = transportPriorityForServiceMap.get(serviceType).contains(PRIMARY_TRANSPORT_ID);

                ISecondaryTransportListener secondaryListener = new ISecondaryTransportListener() {
                    @Override
                    public void onConnectionSuccess(TransportRecord transportRecord) {
                        header.setTransportRecord(transportRecord);
                        handlePacketToSend(header);
                    }

                    @Override
                    public void onConnectionFailure() {
                        if (primaryTransportBackup) {
                            // Primary is also supported as backup
                            header.setTransportRecord(connectedPrimaryTransport);
                            handlePacketToSend(header);
                        } else {
                            DebugTool.logInfo(TAG, "Failed to connect secondary transport, threw away StartService");
                        }
                    }
                };

                synchronized (TRANSPORT_MANAGER_LOCK) {
                    if (transportManager != null) {
                        if (transportManager.isConnected(secondaryTransportType, null)) {
                            //The transport is actually connected, however no service has been registered
                            listenerList.add(secondaryListener);
                            registerSecondaryTransport(sessionID, transportManager.getTransportRecord(secondaryTransportType, null));
                        } else if (secondaryTransportParams != null && secondaryTransportParams.containsKey(secondaryTransportType)) {
                            //No acceptable secondary transport is connected, so first one must be connected
                            header.setTransportRecord(new TransportRecord(secondaryTransportType, ""));
                            listenerList.add(secondaryListener);
                            transportManager.requestSecondaryTransportConnection(sessionID, secondaryTransportParams.get(secondaryTransportType));
                        } else {
                            DebugTool.logWarning(TAG, "No params to connect to secondary transport");
                            //Unable to register or start a secondary connection. Use the callback in case
                            //there is a chance to use the primary transport for this service.
                            secondaryListener.onConnectionFailure();
                        }
                    } else {
                        DebugTool.logError(TAG, "transportManager is null");
                    }
                }
            }
        }
    }

    private void sendHeartBeatACK(SessionType sessionType, byte sessionID) {
        final SdlPacket heartbeat = SdlPacketFactory.createHeartbeatACK(SessionType.CONTROL, sessionID, (byte) protocolVersion.getMajor());
        heartbeat.setTransportRecord(activeTransports.get(sessionType));
        handlePacketToSend(heartbeat);
    }

    public void endService(SessionType serviceType, byte sessionID) {
        if (serviceType.equals(SessionType.RPC)) { //RPC session will close all other sessions so we want to make sure we use the correct EndProtocolSession method
            endSession(sessionID);
        } else {
            SdlPacket header = SdlPacketFactory.createEndSession(serviceType, sessionID, hashID, (byte) protocolVersion.getMajor(), new byte[0]);
            TransportRecord transportRecord = activeTransports.get(serviceType);
            if (transportRecord != null) {
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
     *
     * @param packet packet that will be sent to the router service
     */
    protected void handlePacketToSend(SdlPacket packet) {
        synchronized (FRAME_LOCK) {
            synchronized (TRANSPORT_MANAGER_LOCK) {
                if (packet != null && transportManager != null) {
                    transportManager.sendPacket(packet);
                }
            }
        }
    }

    /**
     * This method handles the end of a protocol session. A callback is
     * sent to the protocol listener.
     **/
    protected void handleServiceEndedNAK(SdlPacket packet, SessionType serviceType) {
        String error = "Service ended NAK received for service type " + serviceType.getName();
        if (packet.version >= 5) {
            if (DebugTool.isDebugEnabled()) {
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
                if (rejectedParams != null && rejectedParams.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Rejected params for service type ");
                    builder.append(serviceType.getName());
                    builder.append(" :");
                    for (String rejectedParam : rejectedParams) {
                        builder.append(rejectedParam);
                        builder.append(" ");
                    }
                    error = builder.toString();
                    DebugTool.logWarning(TAG, error);
                }

            }
        }

        iSdlProtocol.onServiceError(packet, serviceType, packet.getSessionId(), error);
    }

    // This method handles the end of a protocol session. A callback is
    // sent to the protocol listener.

    //FIXME do we do anything in this class for this?
    protected void handleServiceEnded(SdlPacket packet, SessionType sessionType) {
        iSdlProtocol.onServiceEnded(packet, sessionType, packet.getSessionId());
    }


    /**
     * This method handles the startup of a protocol session. A callback is sent
     * to the protocol listener.
     *
     * @param packet      StarServiceACK packet
     * @param serviceType the service type that has just been started
     */
    protected void handleStartServiceACK(SdlPacket packet, SessionType serviceType) {
        // Use this sessionID to create a message lock
        Object messageLock = _messageLocks.get((byte) packet.getSessionId());
        if (messageLock == null) {
            messageLock = new Object();
            _messageLocks.put((byte) packet.getSessionId(), messageLock);
        }
        if (packet.version >= 5) {
            String mtuTag = null;
            if (serviceType.equals(SessionType.RPC)) {
                mtuTag = ControlFrameTags.RPC.StartServiceACK.MTU;
            } else if (serviceType.equals(SessionType.PCM)) {
                mtuTag = ControlFrameTags.Audio.StartServiceACK.MTU;
            } else if (serviceType.equals(SessionType.NAV)) {
                mtuTag = ControlFrameTags.Video.StartServiceACK.MTU;
            }
            Object mtu = packet.getTag(mtuTag);
            if (mtu != null) {
                mtus.put(serviceType, (Long) packet.getTag(mtuTag));
            }
            if (serviceType.equals(SessionType.RPC)) {
                hashID = (Integer) packet.getTag(ControlFrameTags.RPC.StartServiceACK.HASH_ID);
                Object version = packet.getTag(ControlFrameTags.RPC.StartServiceACK.PROTOCOL_VERSION);

                if (version != null) {
                    //At this point we have confirmed the negotiated version between the module and the proxy
                    protocolVersion = new Version((String) version);
                } else {
                    protocolVersion = new Version("5.0.0");
                }

                //Check to make sure this is a transport we are willing to accept
                TransportRecord transportRecord = packet.getTransportRecord();

                if (transportRecord == null || !requestedPrimaryTransports.contains(transportRecord.getType())) {
                    onTransportNotAccepted("Transport is not in requested primary transports");
                    return;
                }


                // This enables custom behavior based on protocol version specifics
                if (protocolVersion.isNewerThan(new Version("5.1.0")) >= 0) {

                    if (activeTransports.get(SessionType.RPC) == null) {    //Might be a better way to handle this

                        ArrayList<String> secondary = (ArrayList<String>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.SECONDARY_TRANSPORTS);
                        ArrayList<Integer> audio = (ArrayList<Integer>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.AUDIO_SERVICE_TRANSPORTS);
                        ArrayList<Integer> video = (ArrayList<Integer>) packet.getTag(ControlFrameTags.RPC.StartServiceACK.VIDEO_SERVICE_TRANSPORTS);

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

                            if (video == null || video.contains(PRIMARY_TRANSPORT_ID)) {
                                activeTransports.put(SessionType.NAV, transportRecord);
                            }
                            if (audio == null || audio.contains(PRIMARY_TRANSPORT_ID)) {
                                activeTransports.put(SessionType.PCM, transportRecord);
                            }
                        } else {

                            if (DebugTool.isDebugEnabled()) {
                                printSecondaryTransportDetails(secondary, audio, video);
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
                        }

                        setTransportPriorityForService(SessionType.PCM, audio);
                        setTransportPriorityForService(SessionType.NAV, video);

                        //Update the developer on the transport status
                        notifyDevTransportListener();

                    } else {
                        DebugTool.logInfo(TAG, "Received a start service ack for RPC service while already active on a different transport.");
                        iSdlProtocol.onServiceStarted(packet, serviceType, (byte) packet.getSessionId(), protocolVersion, packet.isEncrypted());
                        return;
                    }

                    if (protocolVersion.isNewerThan(new Version(5, 2, 0)) >= 0) {
                        String authToken = (String) packet.getTag(ControlFrameTags.RPC.StartServiceACK.AUTH_TOKEN);
                        if (authToken != null) {
                            iSdlProtocol.onAuthTokenReceived(authToken);
                        }
                    }
                } else {

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

                    //Inform the developer of the initial transport connection
                    notifyDevTransportListener();
                }


            } else if (serviceType.equals(SessionType.NAV)) {
                if (iSdlProtocol != null) {
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
            if (serviceType.equals(SessionType.RPC)) {
                TransportRecord transportRecord = packet.getTransportRecord();
                if (transportRecord == null || (requiresHighBandwidth
                        && TransportType.BLUETOOTH.equals(transportRecord.getType()))) {
                    //transport can't support high bandwidth
                    onTransportNotAccepted((transportRecord != null ? transportRecord.getType().toString() : "Transport ") + "can't support high bandwidth requirement, and secondary transport not supported in this protocol version");
                    return;
                }
                //If version < 5 and transport is acceptable we need to just add these
                activeTransports.put(SessionType.RPC, transportRecord);
                activeTransports.put(SessionType.BULK_DATA, transportRecord);
                activeTransports.put(SessionType.CONTROL, transportRecord);
                activeTransports.put(SessionType.NAV, transportRecord);
                activeTransports.put(SessionType.PCM, transportRecord);

                if (protocolVersion.getMajor() > 1) {
                    if (packet.payload != null && packet.dataSize == 4) { //hashId will be 4 bytes in length
                        hashID = BitConverter.intFromByteArray(packet.payload, 0);
                    }
                }
            } else if (serviceType.equals(SessionType.NAV)) {
                //Protocol versions <5 don't support param negotiation
                iSdlProtocol.setAcceptedVideoParams(iSdlProtocol.getDesiredVideoParams());
            }
        }
        if (iSdlProtocol != null) {
            iSdlProtocol.onServiceStarted(packet, serviceType, (byte) packet.getSessionId(), protocolVersion, packet.isEncrypted());
        }
    }

    protected void handleProtocolSessionNAKed(SdlPacket packet, SessionType serviceType) {
        String error = "Service start NAK received for service type " + serviceType.getName();
        List<String> rejectedParams;
        if (packet.version >= 5) {
            if (DebugTool.isDebugEnabled()) {
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
                if (rejectedParams != null && rejectedParams.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Rejected params for service type ");
                    builder.append(serviceType.getName());
                    builder.append(" :");
                    for (String rejectedParam : rejectedParams) {
                        builder.append(rejectedParam);
                        builder.append(" ");
                    }
                    error = builder.toString();
                    DebugTool.logWarning(TAG, error);
                }

            }
        }
        if (serviceType.eq(SessionType.NAV) || serviceType.eq(SessionType.PCM)) {
            iSdlProtocol.onServiceError(packet, serviceType, (byte) packet.sessionId, error);

        } else {
            //TODO should there be any additional checks here? Or should this be more explicit in
            // what types of services would cause this protocol error
            handleProtocolError("Got StartSessionNACK for protocol sessionID = " + packet.getSessionId(), null);
        }
    }

    // This method handles protocol errors. A callback is sent to the protocol
    // listener.
    protected void handleProtocolError(String string, Exception ex) {
        iSdlProtocol.onProtocolError(string, ex);
    }

    protected void handleProtocolHeartbeat(SessionType sessionType, byte sessionID) {
        sendHeartBeatACK(sessionType, sessionID);
    }


    /* --------------------------------------------------------------------------------------------
       -----------------------------------   TRANSPORT_TYPE LISTENER   ---------------------------------
       -------------------------------------------------------------------------------------------*/

    final TransportManagerBase.TransportEventListener transportEventListener = new TransportManagerBase.TransportEventListener() {
        private boolean requestedSession = false;

        @Override
        public void onPacketReceived(SdlPacket packet) {
            handlePacketReceived(packet);
        }

        @Override
        public void onTransportConnected(List<TransportRecord> connectedTransports) {
            DebugTool.logInfo(TAG, "onTransportConnected");
            //In the future we should move this logic into the Protocol Layer
            TransportRecord transportRecord = getTransportForSession(SessionType.RPC);
            if (transportRecord == null && !requestedSession) { //There is currently no transport registered
                requestedSession = true;
                synchronized (TRANSPORT_MANAGER_LOCK) {
                    if (transportManager != null) {
                        transportManager.requestNewSession(getPreferredTransport(requestedPrimaryTransports, connectedTransports));
                    }
                }
            }
            onTransportsConnectedUpdate(connectedTransports);
            if (DebugTool.isDebugEnabled()) {
                printActiveTransports();
            }
        }

        @Override
        public void onTransportDisconnected(String info, TransportRecord disconnectedTransport, List<TransportRecord> connectedTransports) {
            if (disconnectedTransport == null) {
                DebugTool.logInfo(TAG, "onTransportDisconnected");
                synchronized (TRANSPORT_MANAGER_LOCK) {
                    if (transportManager != null) {
                        transportManager.close(iSdlProtocol.getSessionId());
                    }
                }
                iSdlProtocol.shutdown("No transports left connected");
                return;
            } else {
                DebugTool.logInfo(TAG, "onTransportDisconnected - " + disconnectedTransport.getType().name());
            }

            //In the future we will actually compare the record but at this point we can assume only
            //a single transport record per transport.
            //TransportType type = disconnectedTransport.getType();
            if (getTransportForSession(SessionType.NAV) != null && disconnectedTransport.equals(getTransportForSession(SessionType.NAV))) {
                if (serviceStartedOnTransport.get(SessionType.NAV) != null && serviceStartedOnTransport.get(SessionType.NAV)) {
                    iSdlProtocol.onServiceError(null, SessionType.NAV, iSdlProtocol.getSessionId(), "Transport disconnected");
                    activeTransports.remove(SessionType.NAV);
                    serviceStartedOnTransport.remove(SessionType.NAV);
                }
            }
            if (getTransportForSession(SessionType.PCM) != null && disconnectedTransport.equals(getTransportForSession(SessionType.PCM))) {
                if (serviceStartedOnTransport.get(SessionType.PCM) != null && serviceStartedOnTransport.get(SessionType.PCM)) {
                    iSdlProtocol.onServiceError(null, SessionType.PCM, iSdlProtocol.getSessionId(), "Transport disconnected");
                    activeTransports.remove(SessionType.PCM);
                    serviceStartedOnTransport.remove(SessionType.PCM);
                }
            }

            if ((getTransportForSession(SessionType.RPC) != null && disconnectedTransport.equals(getTransportForSession(SessionType.RPC))) || disconnectedTransport.equals(connectedPrimaryTransport)) {
                //Primary transport has been disconnected. Let's check if we can recover.
                //transportTypes.remove(type);
                boolean primaryTransportAvailable = false;
                if (requestedPrimaryTransports != null && requestedPrimaryTransports.size() > 1) {
                    for (TransportType transportType : requestedPrimaryTransports) {
                        DebugTool.logInfo(TAG, "Checking " + transportType.name());

                        synchronized (TRANSPORT_MANAGER_LOCK) {
                            if (!disconnectedTransport.getType().equals(transportType)
                                    && transportManager != null
                                    && transportManager.isConnected(transportType, null)) {

                                //There is currently a supported primary transport

                                //See if any high bandwidth transport is available currently
                                boolean highBandwidthAvailable = transportManager.isHighBandwidthAvailable();

                                if (requiresHighBandwidth) {
                                    if (!highBandwidthAvailable) {
                                        if (TransportType.BLUETOOTH.equals(transportType)
                                                && requestedSecondaryTransports != null
                                                && supportedSecondaryTransports != null) {
                                            for (TransportType secondaryTransport : requestedSecondaryTransports) {
                                                DebugTool.logInfo(TAG, "Checking secondary " + secondaryTransport.name());
                                                if (supportedSecondaryTransports.contains(secondaryTransport)) {
                                                    //Should only be USB or TCP
                                                    highBandwidthAvailable = true;
                                                    break;
                                                }
                                            }
                                        }
                                    } // High bandwidth already available
                                }

                                if (!requiresHighBandwidth || (requiresHighBandwidth && highBandwidthAvailable)) {
                                    primaryTransportAvailable = true;
                                    transportManager.updateTransportConfig(transportConfig);
                                    break;
                                }
                            }
                        }
                    }
                }
                connectedPrimaryTransport = null;
                synchronized (TRANSPORT_MANAGER_LOCK) {
                    if (transportManager != null) {
                        transportManager.close(iSdlProtocol.getSessionId());
                    }
                    transportManager = null;
                }
                requestedSession = false;

                activeTransports.clear();
                serviceStartedOnTransport.clear();

                iSdlProtocol.onTransportDisconnected(info, primaryTransportAvailable, transportConfig);

            } //else Transport was not primary, continuing to stay connected

            //Update the developer since a transport just disconnected
            notifyDevTransportListener();

        }

        @Override
        public void onError(String info) {
            iSdlProtocol.shutdown(info);

        }

        @Override
        public boolean onLegacyModeEnabled(String info) {
            //Await a connection from the legacy transport
            if (requestedPrimaryTransports != null && requestedPrimaryTransports.contains(TransportType.BLUETOOTH)
                    && !SdlProtocolBase.this.requiresHighBandwidth) {
                DebugTool.logInfo(TAG, "Entering legacy mode; creating new protocol instance");
                reset();
                return true;
            } else {
                DebugTool.logInfo(TAG, "Bluetooth is not an acceptable transport; not moving to legacy mode");
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
            } catch (OutOfMemoryError e) {
                DebugTool.logError(TAG, "OutOfMemory error", e); //Garbled bits were received
                accumulator = null;
            }
        }

        protected void handleRemainingFrame(SdlPacket packet) {
            accumulator.write(packet.payload, 0, (int) packet.getDataSize());
            notifyIfFinished(packet);
        }

        protected void notifyIfFinished(SdlPacket packet) {
            if (packet.getFrameType() == FrameType.Consecutive && packet.getFrameInfo() == 0x0) {
                ProtocolMessage message = new ProtocolMessage();
                message.setPayloadProtected(packet.isEncrypted());
                message.setSessionType(SessionType.valueOf((byte) packet.getServiceType()));
                message.setSessionID((byte) packet.getSessionId());
                //If it is WiPro 2.0 it must have binary header
                if (protocolVersion.getMajor() > 1) {
                    BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
                            parseBinaryHeader(accumulator.toByteArray());
                    if (binFrameHeader == null) {
                        return;
                    }
                    message.setVersion((byte) protocolVersion.getMajor());
                    message.setRPCType(binFrameHeader.getRPCType());
                    message.setFunctionID(binFrameHeader.getFunctionID());
                    message.setCorrID(binFrameHeader.getCorrID());
                    if (binFrameHeader.getJsonSize() > 0)
                        message.setData(binFrameHeader.getJsonData());
                    if (binFrameHeader.getBulkData() != null)
                        message.setBulkData(binFrameHeader.getBulkData());
                } else {
                    message.setData(accumulator.toByteArray());
                }

                _assemblerForMessageID.remove(packet.getMessageId());

                try {
                    iSdlProtocol.onProtocolMessageReceived(message);
                } catch (Exception e) {
                    DebugTool.logError(TAG, FailurePropagating_Msg + "onProtocolMessageReceived: " + e.toString(), e);
                } // end-catch

                accumulator = null;
            } // end-if
        } // end-method

        protected void handleMultiFrameMessageFrame(SdlPacket packet) {
            if (packet.getFrameType() == FrameType.First) {
                handleFirstDataFrame(packet);
            } else {
                if (accumulator != null) {
                    handleRemainingFrame(packet);
                }
            }

        } // end-method

        protected void handleFrame(SdlPacket packet) {

            if (packet.getPayload() != null && packet.getDataSize() > 0 && packet.isEncrypted()) {

                SdlSecurityBase sdlSec = iSdlProtocol.getSdlSecurity();
                byte[] dataToRead = new byte[TLS_MAX_RECORD_SIZE];

                Integer numberOfDecryptedBytes = sdlSec.decryptData(packet.getPayload(), dataToRead);
                if ((numberOfDecryptedBytes == null) || (numberOfDecryptedBytes <= 0)) {
                    return;
                }

                byte[] decryptedData = new byte[numberOfDecryptedBytes];
                System.arraycopy(dataToRead, 0, decryptedData, 0, numberOfDecryptedBytes);
                packet.payload = decryptedData;
                packet.dataSize = numberOfDecryptedBytes;
            }

            if (packet.getFrameType().equals(FrameType.Control)) {
                handleControlFrame(packet);
            } else {
                // Must be a form of data frame (single, first, consecutive, etc.)
                if (packet.getFrameType() == FrameType.First
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
            DebugTool.logInfo(TAG, "Received HeartbeatACK - " + packet.toString());
        }

        private void handleProtocolHeartbeat(SdlPacket packet) {
            SdlProtocolBase.this.handleProtocolHeartbeat(SessionType.valueOf((byte) packet.getServiceType()), (byte) packet.getSessionId());
        }

        /**
         * Directing method that will push the packet to the method that can handle it best
         *
         * @param packet a control frame packet
         */
        private void handleControlFrame(SdlPacket packet) {
            Integer frameTemp = packet.getFrameInfo();
            Byte frameInfo = frameTemp.byteValue();

            SessionType serviceType = SessionType.valueOf((byte) packet.getServiceType());

            if (frameInfo == FrameDataControlFrameType.Heartbeat.getValue()) {

                handleProtocolHeartbeat(packet);

            } else if (frameInfo == FrameDataControlFrameType.HeartbeatACK.getValue()) {

                handleProtocolHeartbeatACK(packet);

            } else if (frameInfo == FrameDataControlFrameType.StartSessionACK.getValue()) {

                handleStartServiceACK(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.StartSessionNACK.getValue()) {

                String reason = (String) packet.getTag(ControlFrameTags.RPC.StartServiceNAK.REASON);
                DebugTool.logWarning(TAG, reason);
                handleProtocolSessionNAKed(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.EndSession.getValue()
                    || frameInfo == FrameDataControlFrameType.EndSessionACK.getValue()) {

                handleServiceEnded(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.EndSessionNACK.getValue()) {

                String reason = (String) packet.getTag(ControlFrameTags.RPC.EndServiceNAK.REASON);
                DebugTool.logWarning(TAG, reason);
                handleServiceEndedNAK(packet, serviceType);

            } else if (frameInfo == FrameDataControlFrameType.ServiceDataACK.getValue()) {

                //Currently unused

            } else if (frameInfo == FrameDataControlFrameType.RegisterSecondaryTransportACK.getValue()) {

                handleSecondaryTransportRegistration(packet.getTransportRecord(), true);

            } else if (frameInfo == FrameDataControlFrameType.RegisterSecondaryTransportNACK.getValue()) {

                String reason = (String) packet.getTag(ControlFrameTags.RPC.RegisterSecondaryTransportNAK.REASON);
                DebugTool.logWarning(TAG, reason);
                handleSecondaryTransportRegistration(packet.getTransportRecord(), false);

            } else if (frameInfo == FrameDataControlFrameType.TransportEventUpdate.getValue()) {

                // Get TCP params
                String ipAddr = (String) packet.getTag(ControlFrameTags.RPC.TransportEventUpdate.TCP_IP_ADDRESS);
                Integer port = (Integer) packet.getTag(ControlFrameTags.RPC.TransportEventUpdate.TCP_PORT);

                if (secondaryTransportParams == null) {
                    secondaryTransportParams = new HashMap<>();
                }

                if (ipAddr != null && port != null) {
                    String address = (port != null && port > 0) ? ipAddr + ":" + port : ipAddr;
                    secondaryTransportParams.put(TransportType.TCP, new TransportRecord(TransportType.TCP, address));

                    //A new secondary transport just became available. Notify the developer.
                    notifyDevTransportListener();
                }

            }

            _assemblerForMessageID.remove(packet.getMessageId());

        } // end-method

        private void handleSingleFrameMessageFrame(SdlPacket packet) {
            ProtocolMessage message = new ProtocolMessage();
            message.setPayloadProtected(packet.isEncrypted());
            SessionType serviceType = SessionType.valueOf((byte) packet.getServiceType());
            if (serviceType == SessionType.RPC) {
                message.setMessageType(MessageType.RPC);
            } else if (serviceType == SessionType.BULK_DATA) {
                message.setMessageType(MessageType.BULK);
            } // end-if
            message.setSessionType(serviceType);
            message.setSessionID((byte) packet.getSessionId());
            //If it is WiPro 2.0 it must have binary header
            boolean isControlService = message.getSessionType().equals(SessionType.CONTROL);
            if (protocolVersion.getMajor() > 1 && !isControlService) {
                BinaryFrameHeader binFrameHeader = BinaryFrameHeader.
                        parseBinaryHeader(packet.payload);
                if (binFrameHeader == null) {
                    return;
                }
                message.setVersion((byte) protocolVersion.getMajor());
                message.setRPCType(binFrameHeader.getRPCType());
                message.setFunctionID(binFrameHeader.getFunctionID());
                message.setCorrID(binFrameHeader.getCorrID());
                if (binFrameHeader.getJsonSize() > 0) {
                    message.setData(binFrameHeader.getJsonData());
                }
                if (binFrameHeader.getBulkData() != null) {
                    message.setBulkData(binFrameHeader.getBulkData());
                }
            } else {
                message.setData(packet.payload);
            }

            _assemblerForMessageID.remove(packet.getMessageId());

            try {
                iSdlProtocol.onProtocolMessageReceived(message);
            } catch (Exception ex) {
                DebugTool.logError(TAG, FailurePropagating_Msg + "onProtocolMessageReceived: " + ex.toString(), ex);
                handleProtocolError(FailurePropagating_Msg + "onProtocolMessageReceived: ", ex);
            } // end-catch
        } // end-method
    } // end-class

    public String getBluetoothMacAddress() {
        for (TransportRecord record : activeTransports.values()) {
            if (record.getType() == TransportType.BLUETOOTH) {
                return record.getAddress();
            }
        }

        return null;
    }

}
