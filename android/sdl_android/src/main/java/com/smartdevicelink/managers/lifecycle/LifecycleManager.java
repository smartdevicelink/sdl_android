/*
 * Copyright (c) 2019-2020 Livio, Inc.
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

import android.content.Context;

import androidx.annotation.RestrictTo;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.exception.SdlExceptionCause;
import com.smartdevicelink.protocol.ISdlServiceListener;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.security.SdlSecurityBase;
import com.smartdevicelink.session.SdlSession;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;

/**
 * The lifecycle manager creates a central point for all SDL session logic to converge. It should only be used by
 * the library itself. Usage outside the library is not permitted and will not be protected for in the future.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LifecycleManager extends BaseLifecycleManager {
    private ISdlServiceListener videoServiceListener;
    private boolean videoServiceStartResponseReceived = false;
    private boolean videoServiceStartResponse = false;
    private WeakReference<Context> contextWeakReference;

    public LifecycleManager(AppConfig appConfig, BaseTransportConfig config, LifecycleListener listener) {
        super(appConfig, config, listener);
    }

    @Override
    void initialize() {
        super.initialize();

        if (_transportConfig != null && _transportConfig.getTransportType().equals(TransportType.MULTIPLEX)) {
            this.session = new SdlSession(sdlSessionListener, (MultiplexTransportConfig) _transportConfig);
        } else if (_transportConfig != null && _transportConfig.getTransportType().equals(TransportType.TCP)) {
            this.session = new SdlSession(sdlSessionListener, (TCPTransportConfig) _transportConfig);
        } else {
            DebugTool.logError(TAG, "Unable to create session for transport type");
        }
    }

    @Override
    void cycle(SdlDisconnectedReason disconnectedReason) {
        clean();
        initialize();
        if (!SdlDisconnectedReason.LEGACY_BLUETOOTH_MODE_ENABLED.equals(disconnectedReason) && !SdlDisconnectedReason.PRIMARY_TRANSPORT_CYCLE_REQUEST.equals(disconnectedReason)) {
            //We don't want to alert higher if we are just cycling for legacy bluetooth
            onClose("Sdl Proxy Cycled", new SdlException("Sdl Proxy Cycled", SdlExceptionCause.SDL_PROXY_CYCLED), disconnectedReason);
        }
        if (session != null) {
            try {
                session.startSession();
            } catch (SdlException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    void saveVehicleType(String address, VehicleType type) {
        AndroidTools.saveVehicleType(contextWeakReference.get(), type, address);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setContext(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    void setSdlSecurityStaticVars() {
        super.setSdlSecurityStaticVars();

        Context context = null;
        if (this.contextWeakReference != null) {
            context = contextWeakReference.get();
        }
        SdlSecurityBase.setContext(context);
    }

    @Override
    void onTransportDisconnected(String info, boolean availablePrimary, BaseTransportConfig transportConfig) {
        super.onTransportDisconnected(info, availablePrimary, transportConfig);
        if (availablePrimary) {
            _transportConfig = transportConfig;
            DebugTool.logInfo(TAG, "notifying RPC session ended, but potential primary transport available");
            cycle(SdlDisconnectedReason.PRIMARY_TRANSPORT_CYCLE_REQUEST);
        } else {
            onClose(info, null, null);
        }
    }

    /**
     * This method will try to start the video service with the requested parameters.
     * When it returns it will attempt to store the accepted parameters if available.
     *
     * @param isEncrypted if the service should be encrypted
     * @param afterPendingRestart specify true when the videoService is starting again after the display has been resized
     * @param parameters  the desired video streaming parameters
     */
    @Override
    void startVideoService(boolean isEncrypted, VideoStreamingParameters parameters, boolean afterPendingRestart) {
        if (session == null) {
            DebugTool.logWarning(TAG, "SdlSession is not created yet.");
            return;
        }
        if (!session.getIsConnected()) {
            DebugTool.logWarning(TAG, "Connection is not available.");
            return;
        }

        session.setDesiredVideoParams(parameters);
        tryStartVideoStream(isEncrypted, parameters, afterPendingRestart);
    }

    /**
     * Try to open a video service by using the video streaming parameters supplied.
     * Only information from codecs, width and height are used during video format negotiation.
     *
     * @param isEncrypted Specify true if packets on this service have to be encrypted
     * @param afterPendingRestart specify true when the videoService is starting again after the display has been resized
     * @param parameters  VideoStreamingParameters that are desired. Does not guarantee this is what will be accepted.
     */
    private void tryStartVideoStream(boolean isEncrypted, VideoStreamingParameters parameters, boolean afterPendingRestart) {
        if (session == null) {
            DebugTool.logWarning(TAG, "SdlSession is not created yet.");
            return;
        }
        if (getProtocolVersion() != null && getProtocolVersion().getMajor() >= 5 && !systemCapabilityManager.isCapabilitySupported(SystemCapabilityType.VIDEO_STREAMING)) {
            DebugTool.logWarning(TAG, "Module doesn't support video streaming.");
            return;
        }
        if (parameters == null) {
            DebugTool.logWarning(TAG, "Video parameters were not supplied.");
            return;
        }


        if (afterPendingRestart || !videoServiceStartResponseReceived || !videoServiceStartResponse //If we haven't started the service before
                || (videoServiceStartResponse && isEncrypted && !session.isServiceProtected(SessionType.NAV))) { //Or the service has been started but we'd like to start an encrypted one
            session.setDesiredVideoParams(parameters);

            videoServiceStartResponseReceived = false;
            videoServiceStartResponse = false;

            addVideoServiceListener();
            session.startService(SessionType.NAV, isEncrypted);

        }
    }

    private void addVideoServiceListener() {
        // videos may be started and stopped. Only add this once
        if (videoServiceListener == null) {

            videoServiceListener = new ISdlServiceListener() {
                @Override
                public void onServiceStarted(SdlSession session, SessionType type, boolean isEncrypted) {
                    videoServiceStartResponseReceived = true;
                    videoServiceStartResponse = true;
                }

                @Override
                public void onServiceEnded(SdlSession session, SessionType type) {
                    // reset nav flags so nav can start upon the next transport connection
                    videoServiceStartResponseReceived = false;
                    videoServiceStartResponse = false;
                }

                @Override
                public void onServiceError(SdlSession session, SessionType type, String reason) {
                    // if there is an error reset the flags so that there is a chance to restart streaming
                    videoServiceStartResponseReceived = false;
                    videoServiceStartResponse = false;
                }
            };
            session.addServiceListener(SessionType.NAV, videoServiceListener);
        }
    }

    @Override
    void startAudioService(boolean isEncrypted) {
        if (session == null) {
            DebugTool.logWarning(TAG, "SdlSession is not created yet.");
            return;
        }
        if (!session.getIsConnected()) {
            DebugTool.logWarning(TAG, "Connection is not available.");
            return;
        }
        session.startService(SessionType.PCM, isEncrypted);
    }

    public void setSessionListener(ISessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }
}
