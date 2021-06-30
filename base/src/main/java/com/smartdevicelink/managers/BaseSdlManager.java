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
package com.smartdevicelink.managers;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.file.FileManagerConfig;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.managers.lifecycle.LifecycleManager;
import com.smartdevicelink.managers.lifecycle.SystemCapabilityManager;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.managers.screen.ScreenManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistration;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.SetAppIcon;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.TemplateColorScheme;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.security.SdlSecurityBase;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.SystemInfo;
import com.smartdevicelink.util.Version;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract class BaseSdlManager {

    static final String TAG = "BaseSdlManager";
    final Object STATE_LOCK = new Object();
    int state = -1;
    String appId, appName, shortAppName, resumeHash;
    boolean isMediaApp;
    Language hmiLanguage;
    Language language;
    Vector<AppHMIType> hmiTypes;
    BaseTransportConfig transport;
    Vector<String> vrSynonyms;
    Vector<TTSChunk> ttsChunks;
    TemplateColorScheme dayColorScheme, nightColorScheme;
    Version minimumProtocolVersion;
    Version minimumRPCVersion;
    Queue<RPCNotification> queuedNotifications = null;
    OnRPCNotificationListener queuedNotificationListener = null;
    Map<FunctionID, OnRPCNotificationListener> onRPCNotificationListeners;
    SdlArtwork appIcon;
    SdlManagerListener managerListener;
    List<Class<? extends SdlSecurityBase>> sdlSecList;
    ServiceEncryptionListener serviceEncryptionListener;
    FileManagerConfig fileManagerConfig;
    int changeRegistrationRetry = 0;
    static final int MAX_RETRY = 3;

    // Managers
    LifecycleManager lifecycleManager;
    PermissionManager permissionManager;
    FileManager fileManager;
    ScreenManager screenManager;

    // INTERNAL INTERFACE
    /**
     * This is from the LifeCycleManager directly. In the future if there is a reason to be a man in the middle
     * the SdlManager could create it's own, however right now it was only a duplication of logic tied to the LCM.
     */
    ISdl _internalInterface;

    // Initialize with anonymous lifecycleListener
    final LifecycleManager.LifecycleListener lifecycleListener = new LifecycleManager.LifecycleListener() {
        @Override
        public void onConnected(LifecycleManager lifeCycleManager) {
            DebugTool.logInfo(TAG, "Proxy is connected. Now initializing.");
            synchronized (this) {
                changeRegistrationRetry = 0;
                checkLifecycleConfiguration();
                initialize();
            }
        }

        @Override
        public void onServiceStarted(SessionType sessionType) {

        }

        @Override
        public void onServiceEnded(SessionType sessionType) {

        }

        @Override
        public void onClosed(LifecycleManager lifeCycleManager, String info, Exception e, SdlDisconnectedReason reason) {
            DebugTool.logInfo(TAG, "Proxy is closed.");
            if (reason == null || !reason.equals(SdlDisconnectedReason.LANGUAGE_CHANGE)) {
                dispose();
            }
        }

        @Override
        public void onError(LifecycleManager lifeCycleManager, String info, Exception e) {
            if (info != null) {
                DebugTool.logError(TAG, "Error raised: " + info);
            }

            if (e != null) {
                DebugTool.logError(TAG, "Exception raised: " + e.getMessage());
            }
            
            if (managerListener != null) {
                managerListener.onError(null, info, e);
            }
        }

        @Override
        public boolean onSystemInfoReceived(SystemInfo systemInfo) {
            if (managerListener != null) {
                return managerListener.onSystemInfoReceived(systemInfo);
            } else {
                return true;
            }
        }
    };

    // Sub manager listener
    final CompletionListener subManagerListener = new CompletionListener() {
        @Override
        public synchronized void onComplete(boolean success) {
            if (!success) {
                DebugTool.logError(TAG, "Sub manager failed to initialize");
            }
            checkState();
        }
    };

    // ABSTRACT METHODS
    abstract void retryChangeRegistration();

    abstract void checkState();

    abstract void initialize();

    public abstract void dispose();

    protected void checkLifecycleConfiguration() {
        final Language actualLanguage = this.getRegisterAppInterfaceResponse().getLanguage();
        final Language actualHMILanguage = this.getRegisterAppInterfaceResponse().getHmiDisplayLanguage();

        if ((actualLanguage != null && !actualLanguage.equals(language)) || (actualHMILanguage != null && !actualHMILanguage.equals(hmiLanguage))) {

            final LifecycleConfigurationUpdate lcu = managerListener.managerShouldUpdateLifecycle(actualLanguage, actualHMILanguage);

            ChangeRegistration changeRegistration;
            changeRegistration = new ChangeRegistration(actualLanguage, actualHMILanguage);

            if (lcu != null) {
                changeRegistration.setAppName(lcu.getAppName());
                changeRegistration.setNgnMediaScreenAppName(lcu.getShortAppName());
                changeRegistration.setTtsName(lcu.getTtsName());
                changeRegistration.setVrSynonyms(lcu.getVoiceRecognitionCommandNames());
                changeRegistration.setOnRPCResponseListener(new OnRPCResponseListener() {
                    @Override
                    public void onResponse(int correlationId, RPCResponse response) {
                        if (response.getSuccess()) {
                            try {
                                DebugTool.logInfo(TAG, response.serializeJSON().toString());
                            } catch (JSONException e) {
                                DebugTool.logError(TAG, "Error attempting to serialize ChangeRegistrationResponse", e);
                            }

                            // go through and change sdlManager properties that were changed via the LCU update
                            hmiLanguage = actualHMILanguage;
                            language = actualLanguage;

                            if (lcu.getAppName() != null) {
                                appName = lcu.getAppName();
                            }

                            if (lcu.getShortAppName() != null) {
                                shortAppName = lcu.getShortAppName();
                            }

                            if (lcu.getTtsName() != null) {
                                ttsChunks = lcu.getTtsName();
                            }

                            if (lcu.getVoiceRecognitionCommandNames() != null) {
                                vrSynonyms = lcu.getVoiceRecognitionCommandNames();
                            }
                        } else {
                            DebugTool.logError(TAG, "Change Registration onError: " + response.getResultCode() + " | Info: " + response.getInfo());
                            retryChangeRegistration();
                        }
                    }
                });
                this.sendRPC(changeRegistration);
            }
        }
    }

    /**
     * Get the current state for the SdlManager
     *
     * @return int value that represents the current state
     * @see BaseSubManager
     */
    public int getState() {
        synchronized (STATE_LOCK) {
            return state;
        }
    }

    protected void transitionToState(int state) {
        synchronized (STATE_LOCK) {
            this.state = state;
        }
    }

    void checkSdlManagerState() {
        if (getState() != BaseSubManager.READY && getState() != BaseSubManager.LIMITED) {
            DebugTool.logError(TAG, "SdlManager is not ready for use, be sure to initialize with start() method, implement callback, and use SubManagers in the SdlManager's callback");
        }
    }

    void initNotificationQueue() {
        //Setup the notification queue
        if (onRPCNotificationListeners != null) {
            Set<FunctionID> functionIDSet = onRPCNotificationListeners.keySet();
            if (functionIDSet != null && !functionIDSet.isEmpty()) {
                queuedNotifications = new ConcurrentLinkedQueue<>();
                queuedNotificationListener = new OnRPCNotificationListener() {
                    @Override
                    public void onNotified(RPCNotification notification) {
                        queuedNotifications.add(notification);
                    }
                };
                for (FunctionID functionID : functionIDSet) {
                    addOnRPCNotificationListener(functionID, queuedNotificationListener);
                }
            }
        }
    }

    void handleQueuedNotifications() {
        //Handle queued notifications and add the listeners
        if (onRPCNotificationListeners != null) {
            Set<FunctionID> functionIDSet = onRPCNotificationListeners.keySet();
            if (queuedNotifications != null && queuedNotifications.size() > 0) {
                for (RPCNotification notification : queuedNotifications) {
                    try {
                        OnRPCNotificationListener listener = onRPCNotificationListeners.get(notification.getFunctionID());
                        if (listener != null) {
                            listener.onNotified(notification);
                        }
                    } catch (Exception e) {
                        DebugTool.logError(TAG, "Error going through queued notifications", e);
                    }
                }
            }

            //Swap queued listener for developer's listeners
            if (functionIDSet != null && !functionIDSet.isEmpty()) {
                for (FunctionID functionID : functionIDSet) {
                    //Remove the old queue listener
                    removeOnRPCNotificationListener(functionID, queuedNotificationListener);
                    //Add the developer listener
                    addOnRPCNotificationListener(functionID, onRPCNotificationListeners.get(functionID));
                }
            }
            //Set variables to null that are no longer needed
            queuedNotifications = null;
            queuedNotificationListener = null;
        }
    }

    /**
     * Starts up a SdlManager, and calls provided callback called once all BaseSubManagers are done setting up
     */
    public void start() {
        LifecycleManager.AppConfig appConfig = new LifecycleManager.AppConfig();
        appConfig.setAppName(appName);
        //short app name
        appConfig.setMediaApp(isMediaApp);
        appConfig.setHmiDisplayLanguageDesired(hmiLanguage);
        appConfig.setLanguageDesired(hmiLanguage);
        appConfig.setAppType(hmiTypes);
        appConfig.setVrSynonyms(vrSynonyms);
        appConfig.setTtsName(ttsChunks);
        appConfig.setDayColorScheme(dayColorScheme);
        appConfig.setNightColorScheme(nightColorScheme);
        appConfig.setAppID(appId);
        appConfig.setMinimumProtocolVersion(minimumProtocolVersion);
        appConfig.setMinimumRPCVersion(minimumRPCVersion);
        appConfig.setResumeHash(resumeHash);

        lifecycleManager = new LifecycleManager(appConfig, transport, lifecycleListener);
        _internalInterface = lifecycleManager.getInternalInterface((SdlManager) BaseSdlManager.this);

        if (sdlSecList != null && !sdlSecList.isEmpty()) {
            lifecycleManager.setSdlSecurity(sdlSecList, serviceEncryptionListener);
        }

        //Setup the notification queue
        initNotificationQueue();
    }

    void onReady() {
        // Set the app icon
        if (BaseSdlManager.this.appIcon != null && BaseSdlManager.this.appIcon.getName() != null) {
            if (fileManager != null && fileManager.getState() == BaseSubManager.READY && !fileManager.hasUploadedFile(BaseSdlManager.this.appIcon)) {
                fileManager.uploadArtwork(BaseSdlManager.this.appIcon, new CompletionListener() {
                    @Override
                    public void onComplete(boolean success) {
                        if (success) {
                            SetAppIcon msg = new SetAppIcon(BaseSdlManager.this.appIcon.getName());
                            _internalInterface.sendRPC(msg);
                        }
                    }
                });
            } else {
                SetAppIcon msg = new SetAppIcon(BaseSdlManager.this.appIcon.getName());
                _internalInterface.sendRPC(msg);
            }
        }
    }

    // PROTECTED GETTERS
    protected String getAppName() {
        return appName;
    }

    protected String getAppId() {
        return appId;
    }

    protected String getShortAppName() {
        return shortAppName;
    }

    protected Version getMinimumProtocolVersion() {
        return minimumProtocolVersion;
    }

    protected Version getMinimumRPCVersion() {
        return minimumRPCVersion;
    }

    protected Language getHmiLanguage() {
        return hmiLanguage;
    }

    protected Language getLanguage() {
        return language;
    }

    protected TemplateColorScheme getDayColorScheme() {
        return dayColorScheme;
    }

    protected TemplateColorScheme getNightColorScheme() {
        return nightColorScheme;
    }

    protected Vector<AppHMIType> getAppTypes() {
        return hmiTypes;
    }

    protected Vector<String> getVrSynonyms() {
        return vrSynonyms;
    }

    protected Vector<TTSChunk> getTtsChunks() {
        return ttsChunks;
    }

    protected BaseTransportConfig getTransport() {
        return transport;
    }

    protected FileManagerConfig getFileManagerConfig() {
        return fileManagerConfig;
    }

    // MANAGER GETTERS

    /**
     * Gets the PermissionManager. <br>
     * <strong>Note: PermissionManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a PermissionManager object
     */
    public PermissionManager getPermissionManager() {
        if (permissionManager.getState() != BaseSubManager.READY && permissionManager.getState() != BaseSubManager.LIMITED) {
            DebugTool.logError(TAG, "PermissionManager should not be accessed because it is not in READY/LIMITED state");
        }
        checkSdlManagerState();
        return permissionManager;
    }

    /**
     * Gets the FileManager. <br>
     * <strong>Note: FileManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a FileManager object
     */
    public FileManager getFileManager() {
        if (fileManager.getState() != BaseSubManager.READY && fileManager.getState() != BaseSubManager.LIMITED) {
            DebugTool.logError(TAG, "FileManager should not be accessed because it is not in READY/LIMITED state");
        }
        checkSdlManagerState();
        return fileManager;
    }

    /**
     * Gets the ScreenManager. <br>
     * <strong>Note: ScreenManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a ScreenManager object
     */
    public ScreenManager getScreenManager() {
        if (screenManager.getState() != BaseSubManager.READY && screenManager.getState() != BaseSubManager.LIMITED) {
            DebugTool.logError(TAG, "ScreenManager should not be accessed because it is not in READY/LIMITED state");
        }
        checkSdlManagerState();
        return screenManager;
    }

    /**
     * Gets the SystemCapabilityManager. <br>
     * <strong>Note: SystemCapabilityManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a SystemCapabilityManager object
     */
    public SystemCapabilityManager getSystemCapabilityManager() {
        return lifecycleManager.getSystemCapabilityManager((SdlManager) this);
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
        if (lifecycleManager != null) {
            return lifecycleManager.getRegisterAppInterfaceResponse();
        }
        return null;
    }

    /**
     * Get the current OnHMIStatus
     *
     * @return OnHMIStatus object represents the current OnHMIStatus
     */
    public OnHMIStatus getCurrentHMIStatus() {
        if (this.lifecycleManager != null) {
            return lifecycleManager.getCurrentHMIStatus();
        }
        return null;
    }

    /**
     * Retrieves the auth token, if any, that was attached to the StartServiceACK for the RPC
     * service from the module. For example, this should be used to login to a user account.
     *
     * @return the string representation of the auth token
     */
    public String getAuthToken() {
        return this.lifecycleManager.getAuthToken();
    }

    // SENDING REQUESTS

    /**
     * Send RPC Message <br>
     *
     * @param message RPCMessage
     */
    public void sendRPC(RPCMessage message) {
        _internalInterface.sendRPC(message);
    }

    /**
     * Takes a list of RPCMessages and sends it to SDL in a synchronous fashion. Responses are captured through callback on OnMultipleRequestListener.
     * For sending requests asynchronously, use sendRPCs <br>
     *
     * <strong>NOTE: This will override any listeners on individual RPCs</strong><br>
     *
     * <strong>ADDITIONAL NOTE: This only takes the type of RPCRequest for now, notifications and responses will be thrown out</strong>
     *
     * @param rpcs     is the list of RPCMessages being sent
     * @param listener listener for updates and completions
     */
    public void sendSequentialRPCs(final List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener) {
        List<RPCRequest> rpcRequestList = new ArrayList<>();
        for (int i = 0; i < rpcs.size(); i++) {
            if (rpcs.get(i) instanceof RPCRequest) {
                rpcRequestList.add((RPCRequest) rpcs.get(i));
            }
        }

        if (rpcRequestList.size() > 0) {
            _internalInterface.sendSequentialRPCs(rpcRequestList, listener);
        }
    }

    /**
     * Takes a list of RPCMessages and sends it to SDL. Responses are captured through callback on OnMultipleRequestListener.
     * For sending requests synchronously, use sendSequentialRPCs <br>
     *
     * <strong>NOTE: This will override any listeners on individual RPCs</strong> <br>
     *
     * <strong>ADDITIONAL NOTE: This only takes the type of RPCRequest for now, notifications and responses will be thrown out</strong>
     *
     * @param rpcs     is the list of RPCMessages being sent
     * @param listener listener for updates and completions
     */
    public void sendRPCs(List<? extends RPCMessage> rpcs, final OnMultipleRequestListener listener) {
        List<RPCRequest> rpcRequestList = new ArrayList<>();
        for (int i = 0; i < rpcs.size(); i++) {
            if (rpcs.get(i) instanceof RPCRequest) {
                rpcRequestList.add((RPCRequest) rpcs.get(i));
            }
        }

        if (rpcRequestList.size() > 0) {
            _internalInterface.sendRPCs(rpcRequestList, listener);
        }
    }

    /**
     * Add an OnRPCNotificationListener
     *
     * @param listener listener that will be called when a notification is received
     */
    public void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
        _internalInterface.addOnRPCNotificationListener(notificationId, listener);
    }

    /**
     * Remove an OnRPCNotificationListener
     *
     * @param listener listener that was previously added
     */
    public void removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
        _internalInterface.removeOnRPCNotificationListener(notificationId, listener);
    }

    /**
     * Add an OnRPCRequestListener
     *
     * @param listener listener that will be called when a request is received
     */
    public void addOnRPCRequestListener(FunctionID requestId, OnRPCRequestListener listener) {
        _internalInterface.addOnRPCRequestListener(requestId, listener);
    }

    /**
     * Remove an OnRPCRequestListener
     *
     * @param listener listener that was previously added
     */
    public void removeOnRPCRequestListener(FunctionID requestId, OnRPCRequestListener listener) {
        _internalInterface.removeOnRPCRequestListener(requestId, listener);
    }

    // BUILDER
    public static class Builder {
        final SdlManager sdlManager;

        Builder(@NonNull final String appId, @NonNull final String appName, @NonNull final SdlManagerListener listener) {
            sdlManager = new SdlManager();
            setAppId(appId);
            setAppName(appName);
            setManagerListener(listener);
        }

        /**
         * Sets the App ID
         *
         * @param appId String representation of the App ID retrieved from the SDL Developer Portal
         */
        public Builder setAppId(@NonNull final String appId) {
            sdlManager.appId = appId;
            return this;
        }

        /**
         * Sets the Resumption Hash ID
         *
         * @param resumeHash String representation of the Hash ID Used to resume the application
         */
        public Builder setResumeHash(final String resumeHash) {
            sdlManager.resumeHash = resumeHash;
            return this;
        }

        /**
         * Sets the Application Name
         *
         * @param appName String that will be associated as the app's name
         */
        public Builder setAppName(@NonNull final String appName) {
            sdlManager.appName = appName;
            return this;
        }

        /**
         * Sets the Short Application Name
         *
         * @param shortAppName a shorter representation of the app's name for smaller displays
         */
        public Builder setShortAppName(final String shortAppName) {
            sdlManager.shortAppName = shortAppName;
            return this;
        }

        /**
         * Sets the minimum protocol version that will be permitted to connect.
         * If the protocol version of the head unit connected is below this version,
         * the app will disconnect with an EndService protocol message and will not register.
         *
         * @param minimumProtocolVersion the minimum Protocol spec version that should be accepted
         */
        public Builder setMinimumProtocolVersion(final Version minimumProtocolVersion) {
            sdlManager.minimumProtocolVersion = minimumProtocolVersion;
            return this;
        }

        /**
         * The minimum RPC version that will be permitted to connect.
         * If the RPC version of the head unit connected is below this version, an UnregisterAppInterface will be sent.
         *
         * @param minimumRPCVersion the minimum RPC spec version that should be accepted
         */
        public Builder setMinimumRPCVersion(final Version minimumRPCVersion) {
            sdlManager.minimumRPCVersion = minimumRPCVersion;
            return this;
        }

        /**
         * Sets the Language of the App
         *
         * @param hmiLanguage the desired language to be used on the display/HMI of the connected module
         */
        public Builder setLanguage(final Language hmiLanguage) {
            sdlManager.hmiLanguage = hmiLanguage;
            sdlManager.language = hmiLanguage;
            return this;
        }

        /**
         * Sets the TemplateColorScheme for daytime
         *
         * @param dayColorScheme color scheme that will be used (if supported) when the display is in a "Day Mode" or
         *                       similar. Should comprise of colors that contrast well during the day under sunlight.
         */
        public Builder setDayColorScheme(final TemplateColorScheme dayColorScheme) {
            sdlManager.dayColorScheme = dayColorScheme;
            return this;
        }

        /**
         * Sets the TemplateColorScheme for nighttime
         *
         * @param nightColorScheme color scheme that will be used (if supported) when the display is in a "Night Mode"
         *                         or similar. Should comprise of colors that contrast well during the night and are not
         *                         brighter than average.
         */
        public Builder setNightColorScheme(final TemplateColorScheme nightColorScheme) {
            sdlManager.nightColorScheme = nightColorScheme;
            return this;
        }

        /**
         * Sets the icon for the app on head unit / In-Vehicle-Infotainment system <br>
         *
         * @param sdlArtwork the icon that will be used to represent this application on the connected module
         */
        public Builder setAppIcon(final SdlArtwork sdlArtwork) {
            sdlManager.appIcon = sdlArtwork;
            return this;
        }

        /**
         * Sets the vector of AppHMIType <br>
         * <strong>Note: This should be an ordered list from most -> least relevant</strong>
         *
         * @param hmiTypes HMI types that represent this application. For example, if the app is a music player, the
         *                 MEDIA HMIType should be included.
         */
        public Builder setAppTypes(final Vector<AppHMIType> hmiTypes) {
            sdlManager.hmiTypes = hmiTypes;

            if (hmiTypes != null) {
                sdlManager.isMediaApp = hmiTypes.contains(AppHMIType.MEDIA);
            }

            return this;
        }

        /**
         * Sets the FileManagerConfig for the session.<br>
         * <strong>Note: If not set, the default configuration value of 1 will be set for
         * artworkRetryCount and fileRetryCount in FileManagerConfig</strong>
         *
         * @param fileManagerConfig - configuration options
         */
        public Builder setFileManagerConfig(final FileManagerConfig fileManagerConfig) {
            sdlManager.fileManagerConfig = fileManagerConfig;
            return this;
        }

        /**
         * Sets the voice recognition synonyms that can be used to identify this application.
         *
         * @param vrSynonyms a vector of Strings that can be associated with this app. For example the app's name should
         *                   be included as well as any phonetic spellings of the app name that might help the on-board
         *                   VR system associated a users spoken word with the supplied synonyms.
         */
        public Builder setVrSynonyms(final Vector<String> vrSynonyms) {
            sdlManager.vrSynonyms = vrSynonyms;
            return this;
        }

        /**
         * Sets the Text-To-Speech Name of the application. These TTSChunks might be used by the module as an audio
         * representation of the app's name.
         *
         * @param ttsChunks the TTS chunks that can represent this app's name
         */
        public Builder setTtsName(final Vector<TTSChunk> ttsChunks) {
            sdlManager.ttsChunks = ttsChunks;
            return this;
        }

        /**
         * This Object type may change with the transport refactor
         * Sets the BaseTransportConfig
         *
         * @param transport the type of transport that should be used for this SdlManager instance.
         */
        public Builder setTransportType(@NonNull BaseTransportConfig transport) {
            sdlManager.transport = transport;
            return this;
        }

        /**
         * Sets the security libraries and a callback to notify caller when there is update to encryption service
         *
         * @param secList  The list of security class(es)
         * @param listener The callback object
         */
        public Builder setSdlSecurity(@NonNull List<Class<? extends SdlSecurityBase>> secList, ServiceEncryptionListener listener) {
            sdlManager.sdlSecList = secList;
            sdlManager.serviceEncryptionListener = listener;
            return this;
        }

        /**
         * Set the SdlManager Listener
         *
         * @param listener the listener
         */
        public Builder setManagerListener(@NonNull final SdlManagerListener listener) {
            sdlManager.managerListener = listener;
            return this;
        }

        /**
         * Set RPCNotification listeners. SdlManager will preload these listeners before any RPCs are sent/received.
         *
         * @param listeners a map of listeners that will be called when a notification is received.
         *                  Key represents the FunctionID of the notification and value represents the listener
         */
        public Builder setRPCNotificationListeners(Map<FunctionID, OnRPCNotificationListener> listeners) {
            sdlManager.onRPCNotificationListeners = listeners;
            return this;
        }

        public SdlManager build() {
            if (sdlManager.appName == null) {
                throw new IllegalArgumentException("You must specify an app name by calling setAppName");
            }

            if (sdlManager.appId == null) {
                throw new IllegalArgumentException("You must specify an app ID by calling setAppId");
            }

            if (sdlManager.managerListener == null) {
                throw new IllegalArgumentException("You must set a SdlManagerListener object");
            }

            if (sdlManager.hmiTypes == null) {
                Vector<AppHMIType> hmiTypesDefault = new Vector<>();
                hmiTypesDefault.add(AppHMIType.DEFAULT);
                sdlManager.hmiTypes = hmiTypesDefault;
                sdlManager.isMediaApp = false;
            }
            if (sdlManager.fileManagerConfig == null) {
                //if FileManagerConfig is not set use default
                sdlManager.fileManagerConfig = new FileManagerConfig();
            }

            if (sdlManager.hmiLanguage == null) {
                sdlManager.hmiLanguage = Language.EN_US;
                sdlManager.language = Language.EN_US;
            }

            if (sdlManager.minimumProtocolVersion == null) {
                sdlManager.minimumProtocolVersion = new Version("1.0.0");
            }

            if (sdlManager.minimumRPCVersion == null) {
                sdlManager.minimumRPCVersion = new Version("1.0.0");
            }

            sdlManager.transitionToState(BaseSubManager.SETTING_UP);

            return sdlManager;
        }
    }

    /**
     * Start a secured RPC service
     */
    public void startRPCEncryption() {
        if (lifecycleManager != null) {
            lifecycleManager.startRPCEncryption();
        }
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    void set_internalInterface(ISdl _internalInterface) {
        this._internalInterface = _internalInterface;
    }
}
