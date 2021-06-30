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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.managers.audio.AudioStreamManager;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.lifecycle.ISessionListener;
import com.smartdevicelink.managers.lifecycle.SystemCapabilityManager;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.lockscreen.LockScreenManager;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.managers.screen.ScreenManager;
import com.smartdevicelink.managers.video.VideoStreamManager;
import com.smartdevicelink.protocol.ISdlServiceListener;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.rpc.RegisterAppInterfaceResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCRequestListener;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.TransportRecord;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.Version;

import java.util.List;

/**
 * <strong>SDLManager</strong> <br>
 * <p>
 * This is the main point of contact between an application and SDL <br>
 * <p>
 * It is broken down to these areas: <br>
 * <p>
 * 1. SDLManagerBuilder <br>
 * 2. ISdl Interface along with its overridden methods - This can be passed into attached managers <br>
 * 3. Sending Requests <br>
 * 4. Helper methods
 */
public class SdlManager extends BaseSdlManager {
    private Context context;
    private LockScreenConfig lockScreenConfig;

    // Managers
    private LockScreenManager lockScreenManager;
    private VideoStreamManager videoStreamManager;
    private AudioStreamManager audioStreamManager;

    /**
     * Starts up a SdlManager, and calls provided callback called once all BaseSubManagers are done setting up
     */
    @Override
    public void start() {
        if (lifecycleManager == null) {
            if (transport != null && transport.getTransportType() == TransportType.MULTIPLEX) {
                //Do the thing
                MultiplexTransportConfig multiplexTransportConfig = (MultiplexTransportConfig) (transport);
                final MultiplexTransportConfig.TransportListener devListener = multiplexTransportConfig.getTransportListener();
                multiplexTransportConfig.setTransportListener(new MultiplexTransportConfig.TransportListener() {
                    @Override
                    public void onTransportEvent(List<TransportRecord> connectedTransports, boolean audioStreamTransportAvail, boolean videoStreamTransportAvail) {

                        //Pass to sub managers that need it
                        if (videoStreamManager != null) {
                            videoStreamManager.handleTransportUpdated(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
                        }

                        if (audioStreamManager != null) {
                            audioStreamManager.handleTransportUpdated(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
                        }
                        //If the developer supplied a listener to start, it is time to call that
                        if (devListener != null) {
                            devListener.onTransportEvent(connectedTransports, audioStreamTransportAvail, videoStreamTransportAvail);
                        }
                    }
                });

                //If the requires audio support has not been set, it should be set to true if the
                //app is a media app, and false otherwise
                if (multiplexTransportConfig.requiresAudioSupport() == null) {
                    multiplexTransportConfig.setRequiresAudioSupport(isMediaApp);
                }
            }

            super.start();

            lifecycleManager.setContext(context);
            lifecycleManager.start();
        }
    }

    @Override
    protected void initialize() {
        // Instantiate sub managers
        this.permissionManager = new PermissionManager(_internalInterface);
        this.fileManager = new FileManager(_internalInterface, context, fileManagerConfig);
        if (lockScreenConfig.getDisplayMode() != LockScreenConfig.DISPLAY_MODE_NEVER) {
            this.lockScreenManager = new LockScreenManager(lockScreenConfig, context, _internalInterface);
        }
        this.screenManager = new ScreenManager(_internalInterface, this.fileManager);
        if (getAppTypes().contains(AppHMIType.NAVIGATION) || getAppTypes().contains(AppHMIType.PROJECTION)) {
            this.videoStreamManager = new VideoStreamManager(_internalInterface);
        } else {
            this.videoStreamManager = null;
        }
        if (getAppTypes().contains(AppHMIType.NAVIGATION) || getAppTypes().contains(AppHMIType.PROJECTION)) {
            this.audioStreamManager = new AudioStreamManager(_internalInterface, context);
        } else {
            this.audioStreamManager = null;
        }

        // Start sub managers
        this.permissionManager.start(subManagerListener);
        this.fileManager.start(subManagerListener);
        if (lockScreenConfig.getDisplayMode() != LockScreenConfig.DISPLAY_MODE_NEVER) {
            this.lockScreenManager.start(subManagerListener);
        }
        this.screenManager.start(subManagerListener);
    }

    @Override
    void checkState() {
        if (permissionManager != null && fileManager != null && screenManager != null && (lockScreenConfig.getDisplayMode() == LockScreenConfig.DISPLAY_MODE_NEVER || lockScreenManager != null)) {
            if (permissionManager.getState() == BaseSubManager.READY && fileManager.getState() == BaseSubManager.READY && screenManager.getState() == BaseSubManager.READY && (lockScreenConfig.getDisplayMode() == LockScreenConfig.DISPLAY_MODE_NEVER || lockScreenManager.getState() == BaseSubManager.READY)) {
                DebugTool.logInfo(TAG, "Starting sdl manager, all sub managers are in ready state");
                transitionToState(BaseSubManager.READY);
                handleQueuedNotifications();
                notifyDevListener(null);
                onReady();
            } else if (permissionManager.getState() == BaseSubManager.ERROR && fileManager.getState() == BaseSubManager.ERROR && screenManager.getState() == BaseSubManager.ERROR && (lockScreenConfig.getDisplayMode() == LockScreenConfig.DISPLAY_MODE_NEVER || lockScreenManager.getState() == BaseSubManager.ERROR)) {
                String info = "ERROR starting sdl manager, all sub managers are in error state";
                DebugTool.logError(TAG, info);
                transitionToState(BaseSubManager.ERROR);
                notifyDevListener(info);
            } else if (permissionManager.getState() == BaseSubManager.SETTING_UP || fileManager.getState() == BaseSubManager.SETTING_UP || screenManager.getState() == BaseSubManager.SETTING_UP || (lockScreenConfig.getDisplayMode() != LockScreenConfig.DISPLAY_MODE_NEVER && lockScreenManager != null && lockScreenManager.getState() == BaseSubManager.SETTING_UP)) {
                DebugTool.logInfo(TAG, "SETTING UP sdl manager, some sub managers are still setting up");
                transitionToState(BaseSubManager.SETTING_UP);
                // No need to notify developer here!
            } else {
                DebugTool.logWarning(TAG, "LIMITED starting sdl manager, some sub managers are in error or limited state and the others finished setting up");
                transitionToState(BaseSubManager.LIMITED);
                handleQueuedNotifications();
                notifyDevListener(null);
                onReady();
            }
        } else {
            // We should never be here, but somehow one of the sub-sub managers is null
            String info = "ERROR one of the sdl sub managers is null";
            DebugTool.logError(TAG, info);
            transitionToState(BaseSubManager.ERROR);
            notifyDevListener(info);
        }
    }

    private void notifyDevListener(String info) {
        if (managerListener != null) {
            if (getState() == BaseSubManager.ERROR) {
                managerListener.onError(null, info, null);
            } else {
                managerListener.onStart();
            }
        }
    }

    @Override
    void retryChangeRegistration() {
        changeRegistrationRetry++;
        if (changeRegistrationRetry < MAX_RETRY) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkLifecycleConfiguration();
                    DebugTool.logInfo(TAG, "Retry Change Registration Count: " + changeRegistrationRetry);
                }
            }, 3000);
        }
    }

    /**
     * Dispose SdlManager and clean its resources
     * <strong>Note: new instance of SdlManager should be created on every connection. SdlManager cannot be reused after getting disposed.</strong>
     */
    @SuppressLint("NewApi")
    @Override
    public synchronized void dispose() {
        if (this.permissionManager != null) {
            this.permissionManager.dispose();
        }

        if (this.fileManager != null) {
            this.fileManager.dispose();
        }

        if (this.lockScreenManager != null) {
            this.lockScreenManager.dispose();
        }

        if (this.screenManager != null) {
            this.screenManager.dispose();
        }

        if (this.videoStreamManager != null) {
            this.videoStreamManager.dispose();
        }

        // SuppressLint("NewApi") is used because audioStreamManager is only available on android >= jelly bean
        if (this.audioStreamManager != null) {
            this.audioStreamManager.dispose();
        }

        if (this.lifecycleManager != null) {
            this.lifecycleManager.stop();
        }

        if (managerListener != null) {
            managerListener.onDestroy();
            managerListener = null;
        }

        transitionToState(BaseSubManager.SHUTDOWN);
    }

    // MANAGER GETTERS

    /**
     * Gets the VideoStreamManager. <br>
     * The VideoStreamManager returned will only be not null if the registered app type is
     * either NAVIGATION or PROJECTION. Once the VideoStreamManager is retrieved, its start()
     * method will need to be called before use.
     * <br><br><strong>Note: VideoStreamManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a VideoStreamManager object attached to this SdlManager instance
     */
    public @Nullable
    VideoStreamManager getVideoStreamManager() {
        checkSdlManagerState();
        return videoStreamManager;
    }

    /**
     * Gets the AudioStreamManager. <br>
     * The AudioStreamManager returned will only be not null if the registered app type is
     * either NAVIGATION or PROJECTION. Once the AudioStreamManager is retrieved, its start()
     * method will need to be called before use.
     * <br><strong>Note: AudioStreamManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a AudioStreamManager object
     */
    public @Nullable
    AudioStreamManager getAudioStreamManager() {
        checkSdlManagerState();
        return audioStreamManager;
    }

    /**
     * Gets the LockScreenManager. <br>
     * <strong>Note: LockScreenManager should be used only after SdlManager.start() CompletionListener callback is completed successfully.</strong>
     *
     * @return a LockScreenManager object
     */
    public LockScreenManager getLockScreenManager() {
        if (lockScreenManager.getState() != BaseSubManager.READY && lockScreenManager.getState() != BaseSubManager.LIMITED) {
            DebugTool.logError(TAG, "LockScreenManager should not be accessed because it is not in READY/LIMITED state");
        }
        checkSdlManagerState();
        return lockScreenManager;
    }

    public void setSessionListener(ISessionListener sessionListener)
    {
        lifecycleManager.setSessionListener(sessionListener);
    }

    // PROTECTED GETTERS
    protected LockScreenConfig getLockScreenConfig() {
        return lockScreenConfig;
    }

    // BUILDER
    public static class Builder extends BaseSdlManager.Builder {
        /**
         * Builder for the SdlManager. Parameters in the constructor are required.
         *
         * @param context  the current context
         * @param appId    the app's ID
         * @param appName  the app's name
         * @param listener a SdlManagerListener object
         */
        public Builder(@NonNull Context context, @NonNull final String appId, @NonNull final String appName, @NonNull final SdlManagerListener listener) {
            super(appId, appName, listener);
            setContext(context);
        }

        /**
         * Builder for the SdlManager. Parameters in the constructor are required.
         *
         * @param context  the current context
         * @param appId    the app's ID
         * @param appName  the app's name
         * @param listener a SdlManagerListener object
         */
        public Builder(@NonNull Context context, @NonNull final String appId, @NonNull final String appName, @NonNull BaseTransportConfig transport, @NonNull final SdlManagerListener listener) {
            super(appId, appName, listener);
            setContext(context);
            setTransportType(transport);
        }

        /**
         * Sets the LockScreenConfig for the session. <br>
         * <strong>Note: If not set, the default configuration will be used.</strong>
         *
         * @param lockScreenConfig - configuration options
         */
        public Builder setLockScreenConfig(final LockScreenConfig lockScreenConfig) {
            sdlManager.lockScreenConfig = lockScreenConfig;
            return this;
        }

        /**
         * Sets the Context
         *
         * @param context the current context
         */
        public Builder setContext(Context context) {
            sdlManager.context = context;
            return this;
        }

        /**
         * Build SdlManager ang get it ready to be started
         * <strong>Note: new instance of SdlManager should be created on every connection. SdlManager cannot be reused after getting disposed.</strong>
         *
         * @return SdlManager instance that is ready to be started
         */
        public SdlManager build() {
            if (sdlManager.transport == null) {
                throw new IllegalArgumentException("You must set a transport type object");
            }

            if (sdlManager.lockScreenConfig == null) {
                // if lock screen params are not set, use default
                sdlManager.lockScreenConfig = new LockScreenConfig();
            }

            super.build();

            return sdlManager;
        }
    }

     ISdl _internalInterface = new ISdl() {
        @Override
        public void start() {
            lifecycleManager.getInternalInterface(SdlManager.this).start();
        }

        @Override
        public void stop() {
            lifecycleManager.getInternalInterface(SdlManager.this).start();
        }

        @Override
        public boolean isConnected() {
            return lifecycleManager.getInternalInterface(SdlManager.this).isConnected();
        }

        @Override
        public void addServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
            lifecycleManager.getInternalInterface(SdlManager.this).addServiceListener(serviceType, sdlServiceListener);
        }

        @Override
        public void removeServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener) {
            lifecycleManager.getInternalInterface(SdlManager.this).removeServiceListener(serviceType, sdlServiceListener);
        }

        @Override
        public void startVideoService(VideoStreamingParameters parameters, boolean encrypted, boolean withPendingRestart) {
            lifecycleManager.getInternalInterface(SdlManager.this).startVideoService(parameters, encrypted, withPendingRestart);
        }

        @Override
        public void startAudioService(boolean encrypted) {
            lifecycleManager.getInternalInterface(SdlManager.this).startAudioService(encrypted);
        }

        @Override
        public void sendRPC(RPCMessage message) {
            lifecycleManager.getInternalInterface(SdlManager.this).sendRPC(message);
        }

        @Override
        public void sendRPCs(List<? extends RPCMessage> rpcs, OnMultipleRequestListener listener) {
            lifecycleManager.getInternalInterface(SdlManager.this).sendRPCs(rpcs, listener);
        }

        @Override
        public void sendSequentialRPCs(List<? extends RPCMessage> rpcs, OnMultipleRequestListener listener) {
            lifecycleManager.getInternalInterface(SdlManager.this).sendSequentialRPCs(rpcs, listener);
        }

        @Override
        public void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
            lifecycleManager.getInternalInterface(SdlManager.this).addOnRPCNotificationListener(notificationId, listener);
        }

        @Override
        public boolean removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener) {
            return  lifecycleManager.getInternalInterface(SdlManager.this).removeOnRPCNotificationListener(notificationId, listener);
        }

        @Override
        public void addOnRPCRequestListener(FunctionID functionID, OnRPCRequestListener listener) {
            lifecycleManager.getInternalInterface(SdlManager.this).addOnRPCRequestListener(functionID, listener);
        }

        @Override
        public boolean removeOnRPCRequestListener(FunctionID functionID, OnRPCRequestListener listener) {
            return  lifecycleManager.getInternalInterface(SdlManager.this).removeOnRPCRequestListener(functionID, listener);
        }

        @Override
        public void addOnRPCListener(FunctionID responseId, OnRPCListener listener) {
            lifecycleManager.getInternalInterface(SdlManager.this).addOnRPCListener(responseId, listener);
        }

        @Override
        public boolean removeOnRPCListener(FunctionID responseId, OnRPCListener listener) {
            return  lifecycleManager.getInternalInterface(SdlManager.this).removeOnRPCListener(responseId, listener);
        }

        @Override
        public RegisterAppInterfaceResponse getRegisterAppInterfaceResponse() {
            return  lifecycleManager.getInternalInterface(SdlManager.this).getRegisterAppInterfaceResponse();
        }

        @Override
        public boolean isTransportForServiceAvailable(SessionType serviceType) {
            return  lifecycleManager.getInternalInterface(SdlManager.this).isTransportForServiceAvailable(serviceType);
        }

        @NonNull
        @Override
        public SdlMsgVersion getSdlMsgVersion() {
            return  lifecycleManager.getInternalInterface(SdlManager.this).getSdlMsgVersion();
        }

        @NonNull
        @Override
        public Version getProtocolVersion() {
            return  lifecycleManager.getInternalInterface(SdlManager.this).getProtocolVersion();
        }

         @Override
         public long getMtu(SessionType serviceType) {
             return lifecycleManager.getInternalInterface(SdlManager.this).getMtu(serviceType);
         }

        @Override
        public void startRPCEncryption() {
            lifecycleManager.getInternalInterface(SdlManager.this).startRPCEncryption();
        }

        @Override
        public Taskmaster getTaskmaster() {
            return  lifecycleManager.getInternalInterface(SdlManager.this).getTaskmaster();
        }

        @Override
        public SystemCapabilityManager getSystemCapabilityManager() {
            return  lifecycleManager.getInternalInterface(SdlManager.this).getSystemCapabilityManager();
        }

        @Override
        public PermissionManager getPermissionManager() {
            return permissionManager;
        }
    };
}
