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

package com.smartdevicelink.managers.lifecycle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.ServiceEncryptionListener;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.marshal.JsonRPCMarshaller;
import com.smartdevicelink.protocol.ISdlServiceListener;
import com.smartdevicelink.protocol.ProtocolMessage;
import com.smartdevicelink.protocol.SdlProtocolBase;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.MessageType;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.OnAppInterfaceUnregistered;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.RegisterAppInterface;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SetDisplayLayout;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.SystemRequest;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.TemplateColorScheme;
import com.smartdevicelink.proxy.rpc.UnregisterAppInterface;
import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.AppInterfaceUnregisteredReason;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.PredefinedLayout;
import com.smartdevicelink.proxy.rpc.enums.RequestType;
import com.smartdevicelink.proxy.rpc.enums.Result;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.security.SdlSecurityBase;
import com.smartdevicelink.session.ISdlSessionListener;
import com.smartdevicelink.session.SdlSession;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.util.CorrelationIdGenerator;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.FileUtls;
import com.smartdevicelink.util.SystemInfo;
import com.smartdevicelink.util.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

abstract class BaseLifecycleManager {

    static final String TAG = "Lifecycle Manager";
    public static final Version MAX_SUPPORTED_RPC_VERSION = new Version(8, 0, 0);

    // Protected Correlation IDs
    private final int REGISTER_APP_INTERFACE_CORRELATION_ID = 65529,
            UNREGISTER_APP_INTERFACE_CORRELATION_ID = 65530;

    // Sdl Synchronization Objects
    private static final Object RPC_LISTENER_LOCK = new Object(),
            ON_UPDATE_LISTENER_LOCK = new Object(),
            ON_REQUEST_LISTENER_LOCK = new Object(),
            ON_NOTIFICATION_LISTENER_LOCK = new Object();

    SdlSession session;
    final AppConfig appConfig;
    Version rpcSpecVersion = MAX_SUPPORTED_RPC_VERSION;
    HashMap<Integer, CopyOnWriteArrayList<OnRPCListener>> rpcListeners;
    HashMap<Integer, OnRPCResponseListener> rpcResponseListeners;
    HashMap<Integer, CopyOnWriteArrayList<OnRPCNotificationListener>> rpcNotificationListeners;
    HashMap<Integer, CopyOnWriteArrayList<OnRPCRequestListener>> rpcRequestListeners;
    SystemCapabilityManager systemCapabilityManager;
    private EncryptionLifecycleManager encryptionLifecycleManager;
    RegisterAppInterfaceResponse raiResponse = null;
    private OnHMIStatus currentHMIStatus;
    boolean firstTimeFull = true;
    final LifecycleListener lifecycleListener;
    private List<Class<? extends SdlSecurityBase>> _secList = null;
    private String authToken;
    final Version minimumProtocolVersion;
    final Version minimumRPCVersion;
    BaseTransportConfig _transportConfig;
    private Taskmaster taskmaster;
    private boolean didCheckSystemInfo = false;
    String lastDisplayLayoutRequestTemplate;
    DisplayCapabilities initialMediaCapabilities;

    BaseLifecycleManager(AppConfig appConfig, BaseTransportConfig config, LifecycleListener listener) {
        this.appConfig = appConfig;
        this._transportConfig = config;
        this.lifecycleListener = listener;
        this.minimumProtocolVersion = appConfig.getMinimumProtocolVersion();
        this.minimumRPCVersion = appConfig.getMinimumRPCVersion();
        initialize();
    }

    public void start() {
        try {
            session.startSession();
        } catch (SdlException e) {
            DebugTool.logError(TAG,"Error attempting to start session", e);
        }
    }

    /**
     * Start a secured RPC service
     */
    public void startRPCEncryption() {
        if (session != null) {
            session.startService(SessionType.RPC, true);
        }
    }

    public synchronized void stop() {
        if (session != null) {
            session.close();
            session = null;
        }
        if (taskmaster != null) {
            taskmaster.shutdown();
            taskmaster = null;
        }
    }

    Taskmaster getTaskmaster() {
        if (taskmaster == null) {
            Taskmaster.Builder builder = new Taskmaster.Builder();
            int threadCount = 2;
            // Give NAVIGATION & PROJECTION apps an extra thread to handle audio/video streaming operations
            if (appConfig != null && appConfig.appType != null && (appConfig.appType.contains(AppHMIType.NAVIGATION) || appConfig.appType.contains(AppHMIType.PROJECTION))) {
                threadCount = 3;
            }
            builder.setThreadCount(threadCount);
            builder.shouldBeDaemon(true);
            taskmaster = builder.build();
            taskmaster.start();
        }
        return taskmaster;
    }

    Version getProtocolVersion() {
        if (session != null && session.getProtocolVersion() != null) {
            return session.getProtocolVersion();
        }
        return new Version(1, 0, 0);
    }

    private void sendRPCs(List<? extends RPCMessage> messages, final OnMultipleRequestListener listener) {
        if (messages != null) {
            for (RPCMessage message : messages) {
                // Request Specifics
                if (message instanceof RPCRequest) {
                    RPCRequest request = ((RPCRequest) message);
                    final OnRPCResponseListener devOnRPCResponseListener = request.getOnRPCResponseListener();
                    request.setCorrelationID(CorrelationIdGenerator.generateId());
                    if (listener != null) {
                        listener.addCorrelationId(request.getCorrelationID());
                        request.setOnRPCResponseListener(new OnRPCResponseListener() {
                            @Override
                            public void onResponse(int correlationId, RPCResponse response) {
                                if (devOnRPCResponseListener != null) {
                                    devOnRPCResponseListener.onResponse(correlationId, response);
                                }
                                if (listener.getSingleRpcResponseListener() != null) {
                                    listener.getSingleRpcResponseListener().onResponse(correlationId, response);
                                }
                            }
                        });
                    }
                    sendRPCMessagePrivate(request, false);
                } else {
                    // Notifications and Responses
                    sendRPCMessagePrivate(message, false);
                    if (listener != null) {
                        listener.onUpdate(messages.size());
                        if (messages.size() == 0) {
                            listener.onFinished();
                        }
                    }
                }
            }
        }
    }

    private void sendSequentialRPCs(final List<? extends RPCMessage> messages, final OnMultipleRequestListener listener) {
        if (messages != null) {
            // Break out of recursion, we have finished the requests
            if (messages.size() == 0) {
                if (listener != null) {
                    listener.onFinished();
                }
                return;
            }

            RPCMessage rpc = messages.remove(0);

            // Request Specifics
            if (rpc.getMessageType().equals(RPCMessage.KEY_REQUEST)) {
                RPCRequest request = (RPCRequest) rpc;
                request.setCorrelationID(CorrelationIdGenerator.generateId());

                final OnRPCResponseListener devOnRPCResponseListener = request.getOnRPCResponseListener();

                request.setOnRPCResponseListener(new OnRPCResponseListener() {
                    @Override
                    public void onResponse(int correlationId, RPCResponse response) {
                        if (devOnRPCResponseListener != null) {
                            devOnRPCResponseListener.onResponse(correlationId, response);
                        }
                        if (listener != null) {
                            listener.onResponse(correlationId, response);
                            listener.onUpdate(messages.size());
                        }
                        // recurse after onResponse
                        sendSequentialRPCs(messages, listener);
                    }
                });
                sendRPCMessagePrivate(request, false);
            } else {
                // Notifications and Responses
                sendRPCMessagePrivate(rpc, false);
                if (listener != null) {
                    listener.onUpdate(messages.size());
                }
                // recurse after sending a notification or response as there is no response.
                sendSequentialRPCs(messages, listener);
            }
        }
    }

    /**
     * This method is used to ensure all of the methods in this class can remain private and no grantees can be made
     * to the developer what methods are available or not.
     *
     * <b>NOTE: THERE IS NO GUARANTEE THIS WILL BE A VALID SYSTEM CAPABILITY MANAGER</b>
     *
     * @param sdlManager this must be a working manager instance
     * @return the system capability manager.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SystemCapabilityManager getSystemCapabilityManager(SdlManager sdlManager) {
        if (sdlManager != null) {
            return systemCapabilityManager;
        }
        return null;
    }

    private boolean isConnected() {
        if (session != null) {
            return session.getIsConnected();
        } else {
            return false;
        }
    }

    /**
     * Method to retrieve the RegisterAppInterface Response message that was sent back from the
     * module. It contains various attributes about the connected module and can be used to adapt
     * to different module types and their supported features.
     *
     * @return RegisterAppInterfaceResponse received from the module or null if the app has not yet
     * registered with the module.
     */
    public RegisterAppInterfaceResponse getRegisterAppInterfaceResponse() {
        return this.raiResponse;
    }

    /**
     * Get the current OnHMIStatus
     *
     * @return OnHMIStatus object represents the current OnHMIStatus
     */
    public OnHMIStatus getCurrentHMIStatus() {
        return currentHMIStatus;
    }

    void onClose(String info, Exception e, SdlDisconnectedReason reason) {
        DebugTool.logInfo(TAG, "onClose");
        if (lifecycleListener != null) {
            lifecycleListener.onClosed((LifecycleManager) this, info, e, reason);
        }
    }

    /**
     * This method is used to ensure all of the methods in this class can remain private and no grantees can be made
     * to the developer what methods are available or not.
     *
     * @param sdlManager this must be a working manager instance
     * @return the internal interface that hooks into this manager
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ISdl getInternalInterface(SdlManager sdlManager) {
        if (sdlManager != null) {
            return internalInterface;
        }
        return null;
    }

    /* *******************************************************************************************************
     ********************************** INTERNAL - RPC LISTENERS !! START !! *********************************
     *********************************************************************************************************/

    private void setupInternalRpcListeners() {
        addRpcListener(FunctionID.REGISTER_APP_INTERFACE, rpcListener);
        addRpcListener(FunctionID.ON_HMI_STATUS, rpcListener);
        addRpcListener(FunctionID.ON_HASH_CHANGE, rpcListener);
        addRpcListener(FunctionID.ON_SYSTEM_REQUEST, rpcListener);
        addRpcListener(FunctionID.ON_APP_INTERFACE_UNREGISTERED, rpcListener);
        addRpcListener(FunctionID.UNREGISTER_APP_INTERFACE, rpcListener);

        /*  These are legacy and are necessary for older systems   */
        addRpcListener(FunctionID.ON_SYNC_P_DATA, rpcListener);
        addRpcListener(FunctionID.ON_ENCODED_SYNC_P_DATA, rpcListener);
    }

    private final OnRPCListener rpcListener = new OnRPCListener() {
        @Override
        public void onReceived(RPCMessage message) {
            //Make sure this is a response as expected
            FunctionID functionID = message.getFunctionID();
            if (functionID != null) {
                switch (functionID) {
                    case REGISTER_APP_INTERFACE:
                        //We have begun
                        DebugTool.logInfo(TAG, "RAI Response");
                        BaseLifecycleManager.this.raiResponse = (RegisterAppInterfaceResponse) message;
                        SdlMsgVersion rpcVersion = ((RegisterAppInterfaceResponse) message).getSdlMsgVersion();
                        if (rpcVersion != null) {
                            BaseLifecycleManager.this.rpcSpecVersion = new Version(rpcVersion.getMajorVersion(), rpcVersion.getMinorVersion(), rpcVersion.getPatchVersion());
                        } else {
                            BaseLifecycleManager.this.rpcSpecVersion = MAX_SUPPORTED_RPC_VERSION;
                        }
                        if (minimumRPCVersion != null && minimumRPCVersion.isNewerThan(rpcSpecVersion) == 1) {
                            DebugTool.logWarning(TAG, String.format("Disconnecting from head unit, the configured minimum RPC version %s is greater than the supported RPC version %s", minimumRPCVersion, rpcSpecVersion));
                            UnregisterAppInterface msg = new UnregisterAppInterface();
                            msg.setCorrelationID(UNREGISTER_APP_INTERFACE_CORRELATION_ID);
                            sendRPCMessagePrivate(msg, true);
                            clean();
                            onClose("RPC spec version not supported: " + rpcSpecVersion.toString(), null, SdlDisconnectedReason.MINIMUM_RPC_VERSION_HIGHER_THAN_SUPPORTED);
                            return;
                        }
                        if (!didCheckSystemInfo && lifecycleListener != null) {
                            didCheckSystemInfo = true;
                            VehicleType vehicleType = raiResponse.getVehicleType();
                            String systemSoftwareVersion = raiResponse.getSystemSoftwareVersion();
                            if (vehicleType != null || systemSoftwareVersion != null) {
                                SystemInfo systemInfo = new SystemInfo(vehicleType, systemSoftwareVersion, null);
                                boolean validSystemInfo = lifecycleListener.onSystemInfoReceived(systemInfo);
                                if (!validSystemInfo) {
                                    DebugTool.logWarning(TAG, "Disconnecting from head unit, the system info was not accepted.");
                                    UnregisterAppInterface msg = new UnregisterAppInterface();
                                    msg.setCorrelationID(UNREGISTER_APP_INTERFACE_CORRELATION_ID);
                                    sendRPCMessagePrivate(msg, true);
                                    clean();
                                    onClose("System not supported", null, SdlDisconnectedReason.DEFAULT);
                                    return;
                                }
                            }
                            //If the vehicle is acceptable and this is the first check, init security lib
                            setSecurityLibraryIfAvailable(vehicleType);
                        }
                        // HAX: Issue #1690, Ford Sync bug returning incorrect display capabilities (https://github.com/smartdevicelink/sdl_java_suite/issues/1690). Store the initial capabilities if we are a media app so that we can use them in the future.
                        if (appConfig.appType.contains(AppHMIType.MEDIA)) {
                            initialMediaCapabilities = raiResponse.getDisplayCapabilities();
                        }
                        systemCapabilityManager.parseRAIResponse(raiResponse);
                        break;
                    case ON_HMI_STATUS:
                        DebugTool.logInfo(TAG, "on hmi status");
                        boolean shouldInit = currentHMIStatus == null;
                        currentHMIStatus = (OnHMIStatus) message;
                        if (lifecycleListener != null && shouldInit) {
                            lifecycleListener.onConnected((LifecycleManager) BaseLifecycleManager.this);
                        }
                        break;
                    case ON_HASH_CHANGE:
                        break;
                    case ON_SYSTEM_REQUEST:
                    case ON_ENCODED_SYNC_P_DATA:
                    case ON_SYNC_P_DATA:
                        if (functionID.equals(FunctionID.ON_ENCODED_SYNC_P_DATA) || functionID.equals(FunctionID.ON_SYNC_P_DATA)) {
                            DebugTool.logInfo(TAG, "Received legacy SYNC_P_DATA, handling it as OnSystemRequest");
                        } else {
                            DebugTool.logInfo(TAG, "Received OnSystemRequest");
                        }

                        final OnSystemRequest onSystemRequest = (OnSystemRequest) message;
                        RequestType requestType = onSystemRequest.getRequestType();
                        FileType fileType = onSystemRequest.getFileType();

                        if (onSystemRequest.getUrl() != null) {
                            if ((requestType == RequestType.PROPRIETARY && fileType == FileType.JSON)
                                    || (requestType == RequestType.HTTP && fileType == FileType.BINARY)
                                    || functionID.equals(FunctionID.ON_ENCODED_SYNC_P_DATA)
                                    || functionID.equals(FunctionID.ON_SYNC_P_DATA)) {
                                DebugTool.logInfo(TAG, "List of conditionals has passed");
                                Thread handleOffboardTransmissionThread = new Thread() {
                                    @Override
                                    public void run() {
                                        DebugTool.logInfo(TAG, "Attempting to fetch policies");
                                        RPCRequest request = PoliciesFetcher.fetchPolicies(onSystemRequest);
                                        if (request != null && isConnected()) {
                                            sendRPCMessagePrivate(request, true);
                                        }
                                    }
                                };
                                handleOffboardTransmissionThread.start();
                                return;
                            } else if (requestType == RequestType.ICON_URL) {
                                //Download the icon file and send SystemRequest RPC
                                Thread handleOffBoardTransmissionThread = new Thread() {
                                    @Override
                                    public void run() {
                                        final String urlHttps = onSystemRequest.getUrl().replaceFirst("http://", "https://");
                                        byte[] file = FileUtls.downloadFile(urlHttps);
                                        if (file != null) {
                                            SystemRequest systemRequest = new SystemRequest();
                                            systemRequest.setFileName(onSystemRequest.getUrl());
                                            systemRequest.setBulkData(file);
                                            systemRequest.setRequestType(RequestType.ICON_URL);
                                            if (isConnected()) {
                                                sendRPCMessagePrivate(systemRequest, true);
                                            }
                                        } else {
                                            DebugTool.logError(TAG, "File was null at: " + urlHttps);
                                        }
                                    }
                                };
                                handleOffBoardTransmissionThread.start();
                                return;
                            }
                        }

                        break;
                    case ON_APP_INTERFACE_UNREGISTERED:

                        OnAppInterfaceUnregistered onAppInterfaceUnregistered = (OnAppInterfaceUnregistered) message;

                        if (!onAppInterfaceUnregistered.getReason().equals(AppInterfaceUnregisteredReason.LANGUAGE_CHANGE)) {
                            DebugTool.logInfo(TAG, "on app interface unregistered");
                            clean();
                            onClose("OnAppInterfaceUnregistered received from head unit", null, SdlDisconnectedReason.APP_INTERFACE_UNREG);
                        } else {
                            DebugTool.logInfo(TAG, "re-registering for language change");
                            cycle(SdlDisconnectedReason.LANGUAGE_CHANGE);
                        }
                        break;
                    case UNREGISTER_APP_INTERFACE:
                        DebugTool.logInfo(TAG, "unregister app interface");
                        clean();
                        onClose("UnregisterAppInterface response received from head unit", null, SdlDisconnectedReason.APP_INTERFACE_UNREG);
                        break;
                }
            }
        }


    };

    /* *******************************************************************************************************
     ********************************** INTERNAL - RPC LISTENERS !! END !! *********************************
     *********************************************************************************************************/


    /* *******************************************************************************************************
     ********************************** METHODS - RPC LISTENERS !! START !! **********************************
     *********************************************************************************************************/

    private boolean onRPCReceived(final RPCMessage message) {
        synchronized (RPC_LISTENER_LOCK) {
            if (message == null || message.getFunctionID() == null) {
                return false;
            }

            final int id = message.getFunctionID().getId();
            CopyOnWriteArrayList<OnRPCListener> listeners = rpcListeners.get(id);
            if (listeners != null && listeners.size() > 0) {
                for (OnRPCListener listener : listeners) {
                    listener.onReceived(message);
                }
                return true;
            }
            return false;
        }
    }

    private void addRpcListener(FunctionID id, OnRPCListener listener) {
        synchronized (RPC_LISTENER_LOCK) {
            if (id != null && listener != null) {
                if (!rpcListeners.containsKey(id.getId())) {
                    rpcListeners.put(id.getId(), new CopyOnWriteArrayList<OnRPCListener>());
                }

                CopyOnWriteArrayList<OnRPCListener> listeners = rpcListeners.get(id.getId());
                if (listeners != null) {
                    listeners.add(listener);
                }
            }
        }
    }

    private boolean removeOnRPCListener(FunctionID id, OnRPCListener listener) {
        synchronized (RPC_LISTENER_LOCK) {
            if (rpcListeners != null
                    && id != null
                    && listener != null
                    && rpcListeners.containsKey(id.getId())) {
                return rpcListeners.get(id.getId()).remove(listener);
            }
        }
        return false;
    }

    /**
     * Will provide callback to the listener either onFinish or onError depending on the RPCResponses result code,
     * <p>Will automatically remove the listener for the list of listeners on completion.
     *
     * @param msg The RPCResponse message that was received
     * @return if a listener was called or not
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean onRPCResponseReceived(RPCResponse msg) {
        synchronized (ON_UPDATE_LISTENER_LOCK) {
            int correlationId = msg.getCorrelationID();
            if (rpcResponseListeners != null
                    && rpcResponseListeners.containsKey(correlationId)) {
                OnRPCResponseListener listener = rpcResponseListeners.get(correlationId);
                if (listener != null) {
                    listener.onResponse(correlationId, msg);
                }
                rpcResponseListeners.remove(correlationId);
                return true;
            }
            return false;
        }
    }

    /**
     * Add a listener that will receive the response to the specific RPCRequest sent with the corresponding correlation id
     *
     * @param listener      that will get called back when a response is received
     * @param correlationId of the RPCRequest that was sent
     */
    private void addOnRPCResponseListener(OnRPCResponseListener listener, int correlationId) {
        synchronized (ON_UPDATE_LISTENER_LOCK) {
            if (rpcResponseListeners != null
                    && listener != null) {
                listener.onStart(correlationId);
                rpcResponseListeners.put(correlationId, listener);
            }
        }
    }

    private HashMap<Integer, OnRPCResponseListener> getResponseListeners() {
        synchronized (ON_UPDATE_LISTENER_LOCK) {
            return this.rpcResponseListeners;
        }
    }

    /**
     * Retrieves the auth token, if any, that was attached to the StartServiceACK for the RPC
     * service from the module. For example, this should be used to login to a user account.
     *
     * @return the string representation of the auth token
     */
    public String getAuthToken() {
        return this.authToken;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean onRPCNotificationReceived(RPCNotification notification) {
        if (notification == null) {
            DebugTool.logError(TAG, "onRPCNotificationReceived - Notification was null");
            return false;
        }
        DebugTool.logInfo(TAG, "onRPCNotificationReceived - " + notification.getFunctionName());

        //Before updating any listeners, make sure to do any final updates to the notification RPC now
        if (FunctionID.ON_HMI_STATUS.toString().equals(notification.getFunctionName())) {
            OnHMIStatus onHMIStatus = (OnHMIStatus) notification;
            onHMIStatus.setFirstRun(firstTimeFull);
            if (onHMIStatus.getHmiLevel() == HMILevel.HMI_FULL) {
                firstTimeFull = false;
            }
        }

        synchronized (ON_NOTIFICATION_LISTENER_LOCK) {
            CopyOnWriteArrayList<OnRPCNotificationListener> listeners = rpcNotificationListeners.get(FunctionID.getFunctionId(notification.getFunctionName()));
            if (listeners != null && listeners.size() > 0) {
                for (OnRPCNotificationListener listener : listeners) {
                    listener.onNotified(notification);
                }
                return true;
            }
            return false;
        }
    }

    /**
     * This will add a listener for the specific type of notification. As of now it will only allow
     * a single listener per notification function id
     *
     * @param notificationId The notification type that this listener is designated for
     * @param listener       The listener that will be called when a notification of the provided type is received
     */
    private void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
        synchronized (ON_NOTIFICATION_LISTENER_LOCK) {
            if (notificationId != null && listener != null) {
                if (!rpcNotificationListeners.containsKey(notificationId.getId())) {
                    rpcNotificationListeners.put(notificationId.getId(), new CopyOnWriteArrayList<OnRPCNotificationListener>());
                }
                rpcNotificationListeners.get(notificationId.getId()).add(listener);
            }
        }
    }

    private boolean removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
        synchronized (ON_NOTIFICATION_LISTENER_LOCK) {
            if (rpcNotificationListeners != null
                    && notificationId != null
                    && listener != null
                    && rpcNotificationListeners.containsKey(notificationId.getId())) {
                return rpcNotificationListeners.get(notificationId.getId()).remove(listener);
            }
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean onRPCRequestReceived(RPCRequest request) {
        if (request == null) {
            DebugTool.logError(TAG, "onRPCRequestReceived - request was null");
            return false;
        }
        DebugTool.logInfo(TAG, "onRPCRequestReceived - " + request.getFunctionName());

        synchronized (ON_REQUEST_LISTENER_LOCK) {
            CopyOnWriteArrayList<OnRPCRequestListener> listeners = rpcRequestListeners.get(FunctionID.getFunctionId(request.getFunctionName()));
            if (listeners != null && listeners.size() > 0) {
                for (OnRPCRequestListener listener : listeners) {
                    listener.onRequest(request);
                }
                return true;
            }
            return false;
        }
    }

    /**
     * This will add a listener for the specific type of request. As of now it will only allow
     * a single listener per request function id
     *
     * @param requestId The request type that this listener is designated for
     * @param listener  The listener that will be called when a request of the provided type is received
     */
    private void addOnRPCRequestListener(FunctionID requestId, OnRPCRequestListener listener) {
        synchronized (ON_REQUEST_LISTENER_LOCK) {
            if (requestId != null && listener != null) {
                if (!rpcRequestListeners.containsKey(requestId.getId())) {
                    rpcRequestListeners.put(requestId.getId(), new CopyOnWriteArrayList<OnRPCRequestListener>());
                }
                rpcRequestListeners.get(requestId.getId()).add(listener);
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean removeOnRPCRequestListener(FunctionID requestId, OnRPCRequestListener listener) {
        synchronized (ON_REQUEST_LISTENER_LOCK) {
            if (rpcRequestListeners != null
                    && requestId != null
                    && listener != null
                    && rpcRequestListeners.containsKey(requestId.getId())) {
                return rpcRequestListeners.get(requestId.getId()).remove(listener);
            }
        }
        return false;
    }

    /* *******************************************************************************************************
     **************************************** RPC LISTENERS !! END !! ****************************************
     *********************************************************************************************************/

    private void sendRPCMessagePrivate(RPCMessage message, boolean isInternalMessage) {
        try {
            if (!isInternalMessage && message.getMessageType().equals(RPCMessage.KEY_REQUEST)) {
                RPCRequest request = (RPCRequest) message;
                OnRPCResponseListener listener = request.getOnRPCResponseListener();

                // Test for illegal correlation ID
                if (request.getCorrelationID() == REGISTER_APP_INTERFACE_CORRELATION_ID || request.getCorrelationID() == UNREGISTER_APP_INTERFACE_CORRELATION_ID || request.getCorrelationID() == PoliciesFetcher.POLICIES_CORRELATION_ID) {
                    if (listener != null) {
                        GenericResponse response = new GenericResponse(false, Result.REJECTED);
                        response.setInfo("Invalid correlation ID. The correlation ID, " + request.getCorrelationID() + " , is a reserved correlation ID.");
                        request.getOnRPCResponseListener().onResponse(request.getCorrelationID(), response);
                    }
                    return;
                }

                // Prevent developer from sending RAI or UAI manually
                if (request.getFunctionName().equals(FunctionID.REGISTER_APP_INTERFACE.toString()) || request.getFunctionName().equals(FunctionID.UNREGISTER_APP_INTERFACE.toString())) {
                    if (listener != null) {
                        GenericResponse response = new GenericResponse(false, Result.REJECTED);
                        response.setInfo("The RPCRequest, " + message.getFunctionName() + ", is un-allowed to be sent manually by the developer.");
                        request.getOnRPCResponseListener().onResponse(request.getCorrelationID(), response);
                    }
                    return;
                }
            }

            //FIXME this is temporary until the next major release of the library where OK is removed
            if (message.getMessageType().equals(RPCMessage.KEY_REQUEST)) {
                RPCRequest request = (RPCRequest) message;
                if (FunctionID.SUBSCRIBE_BUTTON.toString().equals(request.getFunctionName())
                        || FunctionID.UNSUBSCRIBE_BUTTON.toString().equals(request.getFunctionName())
                        || FunctionID.BUTTON_PRESS.toString().equals(request.getFunctionName())) {

                    ButtonName buttonName = (ButtonName) request.getObject(ButtonName.class, SubscribeButton.KEY_BUTTON_NAME);


                    if (rpcSpecVersion != null) {
                        if (rpcSpecVersion.getMajor() < 5) {

                            if (ButtonName.PLAY_PAUSE.equals(buttonName)) {
                                request.setParameters(SubscribeButton.KEY_BUTTON_NAME, ButtonName.OK);
                            }
                        } else { //Newer than version 5.0.0
                            if (ButtonName.OK.equals(buttonName)) {
                                RPCRequest request2 = new RPCRequest(request);
                                request2.setParameters(SubscribeButton.KEY_BUTTON_NAME, ButtonName.PLAY_PAUSE);
                                request2.setOnRPCResponseListener(request.getOnRPCResponseListener());
                                sendRPCMessagePrivate(request2, true);
                                return;
                            }
                        }
                    }

                }
            }

            message.format(rpcSpecVersion, true);
            byte[] msgBytes = JsonRPCMarshaller.marshall(message, (byte) getProtocolVersion().getMajor());

            final ProtocolMessage pm = new ProtocolMessage();
            pm.setData(msgBytes);
            if (session != null) {
                pm.setSessionID((byte) session.getSessionId());
            }

            pm.setMessageType(MessageType.RPC);
            pm.setSessionType(SessionType.RPC);
            pm.setFunctionID(FunctionID.getFunctionId(message.getFunctionName()));

            if (encryptionLifecycleManager != null && encryptionLifecycleManager.isEncryptionReady() && encryptionLifecycleManager.getRPCRequiresEncryption(message.getFunctionID())) {
                pm.setPayloadProtected(true);
            } else {
                pm.setPayloadProtected(message.isPayloadProtected());
            }
            if (pm.getPayloadProtected() && (encryptionLifecycleManager == null || !encryptionLifecycleManager.isEncryptionReady())) {
                String errorInfo = "Trying to send an encrypted message and there is no secured service";
                if (message.getMessageType().equals((RPCMessage.KEY_REQUEST))) {
                    RPCRequest request = (RPCRequest) message;
                    OnRPCResponseListener listener = ((RPCRequest) message).getOnRPCResponseListener();
                    if (listener != null) {
                        GenericResponse response = new GenericResponse(false, Result.ABORTED);
                        response.setInfo(errorInfo);
                        request.getOnRPCResponseListener().onResponse(request.getCorrelationID(), response);
                    }
                }
                DebugTool.logWarning(TAG, errorInfo);
                return;
            }

            if (RPCMessage.KEY_REQUEST.equals(message.getMessageType())) { // Request Specifics
                pm.setRPCType((byte) 0x00);
                Integer corrId = ((RPCRequest) message).getCorrelationID();
                if (corrId == null) {
                    DebugTool.logError(TAG, "No correlation ID attached to request. Not sending");
                    return;
                } else {
                    pm.setCorrID(corrId);

                    OnRPCResponseListener listener = ((RPCRequest) message).getOnRPCResponseListener();
                    if (listener != null) {
                        addOnRPCResponseListener(listener, corrId);
                    }
                }
                // HAX: Issue #1690, Ford Sync bug returning incorrect display capabilities (https://github.com/smartdevicelink/sdl_java_suite/issues/1690). Save the next desired layout type to the update capabilities when the SetDisplayLayout response is received
                if (FunctionID.SET_DISPLAY_LAYOUT.toString().equals(message.getFunctionName())) {
                    lastDisplayLayoutRequestTemplate = ((SetDisplayLayout)message).getDisplayLayout();
                }
            } else if (RPCMessage.KEY_RESPONSE.equals(message.getMessageType())) { // Response Specifics
                RPCResponse response = (RPCResponse) message;
                pm.setRPCType((byte) 0x01);
                if (response.getCorrelationID() == null) {
                    //Log error here
                    //throw new SdlException("CorrelationID cannot be null. RPC: " + response.getFunctionName(), SdlExceptionCause.INVALID_ARGUMENT);
                    DebugTool.logError(TAG, "No correlation ID attached to response. Not sending");
                    return;
                } else {
                    pm.setCorrID(response.getCorrelationID());
                }
            } else if (message.getMessageType().equals(RPCMessage.KEY_NOTIFICATION)) { // Notification Specifics
                pm.setRPCType((byte) 0x02);
            }

            if (message.getBulkData() != null) {
                pm.setBulkData(message.getBulkData());
            }

            if (message.getFunctionName().equalsIgnoreCase(FunctionID.PUT_FILE.name())) {
                pm.setPriorityCoefficient(1);
            }

            session.sendMessage(pm);

        } catch (OutOfMemoryError e) {
            DebugTool.logError(TAG,"Error attempting to send RPC message.", e);
        }
    }

    // HAX: Issue #1690, Ford Sync bug returning incorrect display capabilities (https://github.com/smartdevicelink/sdl_java_suite/issues/1690). Use the initial capabilities from RAIR instead of the incorrect ones that are included in SetDisplayLayoutResponse.
    void fixIncorrectDisplayCapabilities(RPCMessage rpc) {
        if (RPCMessage.KEY_RESPONSE.equals(rpc.getMessageType()) && rpc.getFunctionName().equals(FunctionID.SET_DISPLAY_LAYOUT.toString()) &&
                initialMediaCapabilities != null && PredefinedLayout.MEDIA.toString().equals(lastDisplayLayoutRequestTemplate)) {

            SetDisplayLayoutResponse setDisplayLayoutResponse = (SetDisplayLayoutResponse) rpc;
            setDisplayLayoutResponse.setDisplayCapabilities(initialMediaCapabilities);
        }
    }

    /* *******************************************************************************************************
     **************************************** ISdlSessionListener START **************************************
     *********************************************************************************************************/

    final ISdlSessionListener sdlSessionListener = new ISdlSessionListener() {

        @Override
        public void onTransportDisconnected(String info, boolean availablePrimary, BaseTransportConfig transportConfig) {
            BaseLifecycleManager.this.onTransportDisconnected(info, availablePrimary, transportConfig);
        }

        @Override
        public void onRPCMessageReceived(RPCMessage rpc) {
            //Incoming message
            if (rpc != null) {
                String messageType = rpc.getMessageType();
                DebugTool.logInfo(TAG, "RPC received - " + messageType);

                rpc.format(rpcSpecVersion, true);

                fixIncorrectDisplayCapabilities(rpc);

                BaseLifecycleManager.this.onRPCReceived(rpc);

                if (RPCMessage.KEY_RESPONSE.equals(messageType)) {

                    onRPCResponseReceived((RPCResponse) rpc);

                } else if (RPCMessage.KEY_NOTIFICATION.equals(messageType)) {
                    FunctionID functionID = rpc.getFunctionID();
                    if ((FunctionID.ON_BUTTON_PRESS.equals(functionID)) || FunctionID.ON_BUTTON_EVENT.equals(functionID)) {
                        RPCNotification notificationCompat = handleButtonNotificationFormatting(rpc);
                        if (notificationCompat != null) {
                            onRPCNotificationReceived((notificationCompat));
                        }
                    }

                    onRPCNotificationReceived((RPCNotification) rpc);

                } else if (RPCMessage.KEY_REQUEST.equals(messageType)) {

                    onRPCRequestReceived((RPCRequest) rpc);

                }
            } else {
                DebugTool.logWarning(TAG, "Shouldn't be here");
            }
        }


        @Override
        public void onSessionStarted(int sessionID, Version version, SystemInfo systemInfo) {
            DebugTool.logInfo(TAG, "on protocol session started");
            if (minimumProtocolVersion != null && minimumProtocolVersion.isNewerThan(version) == 1) {
                DebugTool.logWarning(TAG, String.format("Disconnecting from head unit, the configured minimum protocol version %s is greater than the supported protocol version %s", minimumProtocolVersion, getProtocolVersion()));
                session.endService(SessionType.RPC);
                clean();
                onClose("Protocol version not supported: " + version, null, SdlDisconnectedReason.MINIMUM_PROTOCOL_VERSION_HIGHER_THAN_SUPPORTED);
                return;
            }

            if (systemInfo != null && lifecycleListener != null) {
                didCheckSystemInfo = true;
                boolean validSystemInfo = lifecycleListener.onSystemInfoReceived(systemInfo);
                if (!validSystemInfo) {
                    DebugTool.logWarning(TAG, "Disconnecting from head unit, the system info was not accepted.");
                    session.endService(SessionType.RPC);
                    clean();
                    onClose("System not supported", null, SdlDisconnectedReason.DEFAULT);
                    return;
                }
                //If the vehicle is acceptable, init security lib
                setSecurityLibraryIfAvailable(systemInfo.getVehicleType());
            }

            if (appConfig != null) {
                appConfig.prepare();

                SdlMsgVersion sdlMsgVersion = new SdlMsgVersion();
                sdlMsgVersion.setMajorVersion(MAX_SUPPORTED_RPC_VERSION.getMajor());
                sdlMsgVersion.setMinorVersion(MAX_SUPPORTED_RPC_VERSION.getMinor());
                sdlMsgVersion.setPatchVersion(MAX_SUPPORTED_RPC_VERSION.getPatch());

                RegisterAppInterface rai = new RegisterAppInterface(sdlMsgVersion,
                        appConfig.getAppName(), appConfig.isMediaApp(), appConfig.getLanguageDesired(),
                        appConfig.getHmiDisplayLanguageDesired(), appConfig.getAppID());
                rai.setCorrelationID(REGISTER_APP_INTERFACE_CORRELATION_ID);

                rai.setTtsName(appConfig.getTtsName());
                rai.setNgnMediaScreenAppName(appConfig.getNgnMediaScreenAppName());
                rai.setVrSynonyms(appConfig.getVrSynonyms());
                rai.setAppHMIType(appConfig.getAppType());
                rai.setDayColorScheme(appConfig.getDayColorScheme());
                rai.setNightColorScheme(appConfig.getNightColorScheme());
                rai.setHashID(appConfig.getResumeHash());

                //Add device/system info in the future

                sendRPCMessagePrivate(rai, true);
            } else {
                DebugTool.logError(TAG, "App config was null, soo...");
            }
        }

        @Override
        public void onSessionEnded(int sessionID) {
            DebugTool.logInfo(TAG, "on protocol session ended");
        }

        @Override
        public void onAuthTokenReceived(String token, int sessionID) {
            BaseLifecycleManager.this.authToken = token;
        }
    };

    /* *******************************************************************************************************
     *************************************** ISdlConnectionListener END ************************************
     *********************************************************************************************************/


    /* *******************************************************************************************************
     ******************************************** ISdl - START ***********************************************
     *********************************************************************************************************/

    final ISdl internalInterface = new ISdl() {
        @Override
        public void start() {
            BaseLifecycleManager.this.start();
        }

        @Override
        public void stop() {
            BaseLifecycleManager.this.stop();
        }

        @Override
        public boolean isConnected() {
            synchronized (BaseLifecycleManager.this) {
                if (BaseLifecycleManager.this.session != null) {
                    return BaseLifecycleManager.this.session.getIsConnected();
                }
            }
            return false;
        }

        @Override
        public void addServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
            synchronized (BaseLifecycleManager.this) {
                if(BaseLifecycleManager.this.session != null ){
                    BaseLifecycleManager.this.session.addServiceListener(serviceType, sdlServiceListener);
                }
            }
        }

        @Override
        public void removeServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
            synchronized (BaseLifecycleManager.this) {
                if (BaseLifecycleManager.this.session != null) {
                    BaseLifecycleManager.this.session.removeServiceListener(serviceType, sdlServiceListener);
                }
            }
        }

        @Override
        public void startVideoService(VideoStreamingParameters parameters, boolean encrypted, boolean afterPendingRestart) {
            BaseLifecycleManager.this.startVideoService(encrypted, parameters, afterPendingRestart);
        }

        @Override
        public void startAudioService(boolean encrypted) {
            BaseLifecycleManager.this.startAudioService(encrypted);
        }

        @Override
        public void sendRPC(RPCMessage message) {
            if (isConnected()) {
                BaseLifecycleManager.this.sendRPCMessagePrivate(message, false);
            }
        }

        @Override
        public void sendRPCs(List<? extends RPCMessage> rpcs, OnMultipleRequestListener listener) {
            BaseLifecycleManager.this.sendRPCs(rpcs, listener);
        }

        @Override
        public void sendSequentialRPCs(List<? extends RPCMessage> rpcs, OnMultipleRequestListener listener) {
            BaseLifecycleManager.this.sendSequentialRPCs(rpcs, listener);
        }

        @Override
        public void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
            BaseLifecycleManager.this.addOnRPCNotificationListener(notificationId, listener);
        }

        @Override
        public boolean removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
            return BaseLifecycleManager.this.removeOnRPCNotificationListener(notificationId, listener);
        }

        @Override
        public void addOnRPCRequestListener(FunctionID notificationId, OnRPCRequestListener listener) {
            BaseLifecycleManager.this.addOnRPCRequestListener(notificationId, listener);
        }

        @Override
        public boolean removeOnRPCRequestListener(FunctionID notificationId, OnRPCRequestListener listener) {
            return BaseLifecycleManager.this.removeOnRPCRequestListener(notificationId, listener);
        }

        @Override
        public void addOnRPCListener(FunctionID responseId, OnRPCListener listener) {
            BaseLifecycleManager.this.addRpcListener(responseId, listener);
        }

        @Override
        public boolean removeOnRPCListener(FunctionID responseId, OnRPCListener listener) {
            return BaseLifecycleManager.this.removeOnRPCListener(responseId, listener);
        }

        @Override
        public RegisterAppInterfaceResponse getRegisterAppInterfaceResponse() {
            return raiResponse;
        }

        @Override
        public boolean isTransportForServiceAvailable(SessionType serviceType) {
            synchronized (BaseLifecycleManager.this) {
                if (BaseLifecycleManager.this.session != null) {
                    return BaseLifecycleManager.this.session.isTransportForServiceAvailable(serviceType);
                }
            }
            return false;
        }

        @NonNull
        @Override
        public SdlMsgVersion getSdlMsgVersion() {
            SdlMsgVersion msgVersion;
            if(rpcSpecVersion != null) {
                msgVersion = new SdlMsgVersion(rpcSpecVersion.getMajor(), rpcSpecVersion.getMinor());
                msgVersion.setPatchVersion(rpcSpecVersion.getPatch());
            } else {
                msgVersion = new SdlMsgVersion(1,0);
            }

            return msgVersion;
        }

        @NonNull
        @Override
        public Version getProtocolVersion() {
            return BaseLifecycleManager.this.getProtocolVersion();
        }

        @Override
        public long getMtu(SessionType serviceType) {
            synchronized (BaseLifecycleManager.this) {
                if (BaseLifecycleManager.this.session != null) {
                    return BaseLifecycleManager.this.session.getMtu(serviceType);
                }
            }
            return SdlProtocolBase.V1_V2_MTU_SIZE;
        }

        @Override
        public void startRPCEncryption() {
            BaseLifecycleManager.this.startRPCEncryption();
        }

        @Override
        public Taskmaster getTaskmaster() {
            return BaseLifecycleManager.this.getTaskmaster();
        }

        @Override
        public SystemCapabilityManager getSystemCapabilityManager() {
            return BaseLifecycleManager.this.systemCapabilityManager;
        }

        @Override
        public PermissionManager getPermissionManager() {
            return null;
        }
    };

    /* *******************************************************************************************************
     ********************************************* ISdl - END ************************************************
     *********************************************************************************************************/

    /**
     * Temporary method to bridge the new PLAY_PAUSE and OKAY button functionality with the old
     * OK button name. This should be removed during the next major release
     *
     * @param notification an RPC message object that should be either an ON_BUTTON_EVENT or ON_BUTTON_PRESS otherwise
     *                     it will be ignored
     */
    private RPCNotification handleButtonNotificationFormatting(RPCMessage notification) {
        if (FunctionID.ON_BUTTON_EVENT.toString().equals(notification.getFunctionName())
                || FunctionID.ON_BUTTON_PRESS.toString().equals(notification.getFunctionName())) {

            ButtonName buttonName = (ButtonName) notification.getObject(ButtonName.class, OnButtonEvent.KEY_BUTTON_NAME);
            ButtonName compatBtnName = null;

            if (rpcSpecVersion != null && rpcSpecVersion.getMajor() >= 5) {
                if (ButtonName.PLAY_PAUSE.equals(buttonName)) {
                    compatBtnName = ButtonName.OK;
                }
            } else { // rpc spec version is either null or less than 5
                if (ButtonName.OK.equals(buttonName)) {
                    compatBtnName = ButtonName.PLAY_PAUSE;
                }
            }

            try {
                if (compatBtnName != null) { //There is a button name that needs to be swapped out
                    RPCNotification notification2;
                    //The following is done because there is currently no way to make a deep copy
                    //of an RPC. Since this code will be removed, it's ugliness is borderline acceptable.
                    if (notification instanceof OnButtonEvent) {
                        OnButtonEvent onButtonEvent = new OnButtonEvent();
                        onButtonEvent.setButtonEventMode(((OnButtonEvent) notification).getButtonEventMode());
                        onButtonEvent.setCustomButtonID(((OnButtonEvent) notification).getCustomButtonID());
                        notification2 = onButtonEvent;
                    } else if (notification instanceof OnButtonPress) {
                        OnButtonPress onButtonPress = new OnButtonPress();
                        onButtonPress.setButtonPressMode(((OnButtonPress) notification).getButtonPressMode());
                        onButtonPress.setCustomButtonID(((OnButtonPress) notification).getCustomButtonID());
                        notification2 = onButtonPress;
                    } else {
                        return null;
                    }

                    notification2.setParameters(OnButtonEvent.KEY_BUTTON_NAME, compatBtnName);
                    return notification2;
                }
            } catch (Exception e) {
                //Should never get here
            }
        }
        return null;
    }

    void clean() {
        firstTimeFull = true;
        currentHMIStatus = null;
        lastDisplayLayoutRequestTemplate = null;
        initialMediaCapabilities = null;
        if (rpcListeners != null) {
            rpcListeners.clear();
        }
        if (rpcResponseListeners != null) {
            rpcResponseListeners.clear();
        }
        if (rpcNotificationListeners != null) {
            rpcNotificationListeners.clear();
        }
        if (rpcRequestListeners != null) {
            rpcRequestListeners.clear();
        }
        if (session != null && session.getIsConnected()) {
            session.close();
        }
        if (encryptionLifecycleManager != null) {
            encryptionLifecycleManager.dispose();
        }
        if (taskmaster != null) {
            taskmaster.shutdown();
            taskmaster = null;
        }
    }

    /**
     * Sets the security libraries and a callback to notify caller when there is update to encryption service
     *
     * @param secList  The list of security class(es)
     * @param listener The callback object
     */
    public void setSdlSecurity(@NonNull List<Class<? extends SdlSecurityBase>> secList, ServiceEncryptionListener listener) {
        this._secList = secList;
        this.encryptionLifecycleManager = new EncryptionLifecycleManager(internalInterface, listener);
    }

    /**
     * Using the vehicle type information, specifically the make, the library will attempt to init
     * the security library that is associated with that OEM.
     * @param vehicleType type of vehicle that is currently connected
     */
    private void setSecurityLibraryIfAvailable(VehicleType vehicleType) {
        if (_secList == null) {
            return;
        }

        if (vehicleType == null) {
            return;
        }

        String make = vehicleType.getMake();
        if (make == null) {
            return;
        }

        setSdlSecurityStaticVars();

        SdlSecurityBase sec;

        for (Class<? extends SdlSecurityBase> cls : _secList) {
            try {
                sec = cls.newInstance();
            } catch (Exception e) {
                continue;
            }

            if ((sec != null) && (sec.getMakeList() != null)) {
                if (sec.getMakeList().contains(make)) {
                    sec.setAppId(appConfig.getAppID());
                    if (session != null) {
                        session.setSdlSecurity(sec);
                        sec.handleSdlSession(session);
                    }
                    return;
                }
            }
        }
    }

    /* *******************************************************************************************************
     ********************************** Platform specific methods - START *************************************
     *********************************************************************************************************/

    void initialize() {
        this.rpcListeners = new HashMap<>();
        this.rpcResponseListeners = new HashMap<>();
        this.rpcNotificationListeners = new HashMap<>();
        this.rpcRequestListeners = new HashMap<>();
        this.systemCapabilityManager = new SystemCapabilityManager(internalInterface);
        setupInternalRpcListeners();
    }


    abstract void cycle(SdlDisconnectedReason disconnectedReason);

    void onTransportDisconnected(String info, boolean availablePrimary, BaseTransportConfig transportConfig) {
    }

    void startVideoService(boolean encrypted, VideoStreamingParameters parameters, boolean afterPendingRestart) {
    }

    void startAudioService(boolean encrypted) {
    }

    void setSdlSecurityStaticVars() {
    }

    /* *******************************************************************************************************
     ********************************** Platform specific methods - End *************************************
     *********************************************************************************************************/

    /* *******************************************************************************************************
     ****************************** Inner Classes and Interfaces - Start *************************************
     *********************************************************************************************************/

    public interface LifecycleListener {
        void onConnected(LifecycleManager lifeCycleManager);

        void onClosed(LifecycleManager lifeCycleManager, String info, Exception e, SdlDisconnectedReason reason);

        void onServiceStarted(SessionType sessionType);

        void onServiceEnded(SessionType sessionType);

        void onError(LifecycleManager lifeCycleManager, String info, Exception e);

        boolean onSystemInfoReceived(SystemInfo systemInfo);
    }

    public static class AppConfig {
        private String appID, appName, ngnMediaScreenAppName, resumeHash;
        private Vector<TTSChunk> ttsName;
        private Vector<String> vrSynonyms;
        private boolean isMediaApp = false;
        private Language languageDesired, hmiDisplayLanguageDesired;
        private Vector<AppHMIType> appType;
        private TemplateColorScheme dayColorScheme, nightColorScheme;
        private Version minimumProtocolVersion;
        private Version minimumRPCVersion;

        private void prepare() {
            if (getNgnMediaScreenAppName() == null) {
                setNgnMediaScreenAppName(getAppName());
            }

            if (getLanguageDesired() == null) {
                setLanguageDesired(Language.EN_US);
            }

            if (getHmiDisplayLanguageDesired() == null) {
                setHmiDisplayLanguageDesired(Language.EN_US);
            }

            if (getVrSynonyms() == null) {
                setVrSynonyms(new Vector<String>());
                getVrSynonyms().add(getAppName());
            }
        }

        public String getAppID() {
            return appID;
        }

        public void setAppID(String appID) {
            this.appID = appID;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getNgnMediaScreenAppName() {
            return ngnMediaScreenAppName;
        }

        public void setNgnMediaScreenAppName(String ngnMediaScreenAppName) {
            this.ngnMediaScreenAppName = ngnMediaScreenAppName;
        }

        public Vector<TTSChunk> getTtsName() {
            return ttsName;
        }

        public void setTtsName(Vector<TTSChunk> ttsName) {
            this.ttsName = ttsName;
        }

        public Vector<String> getVrSynonyms() {
            return vrSynonyms;
        }

        public void setVrSynonyms(Vector<String> vrSynonyms) {
            this.vrSynonyms = vrSynonyms;
        }

        public boolean isMediaApp() {
            return isMediaApp;
        }

        public void setMediaApp(boolean mediaApp) {
            isMediaApp = mediaApp;
        }

        public Language getLanguageDesired() {
            return languageDesired;
        }

        public void setLanguageDesired(Language languageDesired) {
            this.languageDesired = languageDesired;
        }

        public Language getHmiDisplayLanguageDesired() {
            return hmiDisplayLanguageDesired;
        }

        public void setHmiDisplayLanguageDesired(Language hmiDisplayLanguageDesired) {
            this.hmiDisplayLanguageDesired = hmiDisplayLanguageDesired;
        }

        public Vector<AppHMIType> getAppType() {
            return appType;
        }

        public void setAppType(Vector<AppHMIType> appType) {
            this.appType = appType;
        }

        public String getResumeHash() {
            return this.resumeHash;
        }

        public void setResumeHash(String resumeHash) {
            this.resumeHash = resumeHash;
        }

        public TemplateColorScheme getDayColorScheme() {
            return dayColorScheme;
        }

        public void setDayColorScheme(TemplateColorScheme dayColorScheme) {
            this.dayColorScheme = dayColorScheme;
        }

        public TemplateColorScheme getNightColorScheme() {
            return nightColorScheme;
        }

        public void setNightColorScheme(TemplateColorScheme nightColorScheme) {
            this.nightColorScheme = nightColorScheme;
        }

        public Version getMinimumProtocolVersion() {
            return minimumProtocolVersion;
        }

        /**
         * Sets the minimum protocol version that will be permitted to connect.
         * If the protocol version of the head unit connected is below this version,
         * the app will disconnect with an EndService protocol message and will not register.
         *
         * @param minimumProtocolVersion a Version object with the minimally accepted Protocol version
         */
        public void setMinimumProtocolVersion(Version minimumProtocolVersion) {
            this.minimumProtocolVersion = minimumProtocolVersion;
        }

        public Version getMinimumRPCVersion() {
            return minimumRPCVersion;
        }

        /**
         * The minimum RPC version that will be permitted to connect.
         * If the RPC version of the head unit connected is below this version, an UnregisterAppInterface will be sent.
         *
         * @param minimumRPCVersion a Version object with the minimally accepted RPC spec version
         */
        public void setMinimumRPCVersion(Version minimumRPCVersion) {
            this.minimumRPCVersion = minimumRPCVersion;
        }
    }

    /* *******************************************************************************************************
     ****************************** Inner Classes and Interfaces - End ***************************************
     *********************************************************************************************************/
}
