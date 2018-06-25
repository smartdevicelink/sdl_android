package com.smartdevicelink.proxy.interfaces;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;

/*
 * Copyright (c) 2017 Livio, Inc.
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
@SuppressWarnings("unused")
public interface ISdl {

    /**
     * Starts the connection with the module
     */
    void start();

    /**
     * Ends connection with the module
     */
    void stop();

    /**
     * Method to check if the session is connected
     * @return if there is a connected session
     */
    boolean isConnected();

    /**
     * Add a service listener for a specific service type
     * @param serviceType service type that the listener will be attached to
     * @param sdlServiceListener listener for events that happen to the service
     */
    void addServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener);

    /**
     * Remote a service listener for a specific service type
     * @param serviceType service type that the listener was attached to
     * @param sdlServiceListener service listener that was previously added for the service type
     */
    void removeServiceListener(SessionType serviceType, ISdlServiceListener sdlServiceListener);

    /**
     * Starts the video streaming service
     * @param parameters desired video streaming params for this sevice to be started with
     * @param encrypted flag to start this service with encryption or not
     */
    void startVideoService(VideoStreamingParameters parameters, boolean encrypted);

    /**
     * Stops the video service if open
     */
    void stopVideoService();

    /**
     * Starts the Audio streaming service
     * @param encrypted flag to start this service with encryption or not
     */
    void startAudioService(boolean encrypted);

    /**
     * Stops the audio service if open
     */
    void stopAudioService();

    /**
     * Pass an RPC message through the proxy to be sent to the connected module
     * @param message RPCRequest that should be sent to the module
     */
    void sendRPCRequest(RPCRequest message);

    /**
     * Add an OnRPCNotificationListener for specified notification
     * @param notificationId FunctionID of the notification that is to be listened for
     * @param listener listener that should be added for the notification ID
     */
    void addOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener);

    /**
     * Removes an OnRPCNotificationListener for specified notification
     * @param notificationId FunctionID of the notification that was to be listened for
     * @param listener listener that was previously added for the notification ID
     */
    boolean removeOnRPCNotificationListener(FunctionID notificationId, OnRPCNotificationListener listener);

}
