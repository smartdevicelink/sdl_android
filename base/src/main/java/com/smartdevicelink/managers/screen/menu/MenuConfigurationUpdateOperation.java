/*
 * Copyright (c) 2021 Livio, Inc.
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

package com.smartdevicelink.managers.screen.menu;

import com.livio.taskmaster.Task;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.MenuLayout;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Bilal Alsharifi on 1/21/21.
 */
class MenuConfigurationUpdateOperation extends Task {
    private static final String TAG = "MenuConfigurationUpdateOperation";
    private final WeakReference<ISdl> internalInterface;
    private final List<MenuLayout> availableMenuLayouts;
    private final MenuConfiguration updatedMenuConfiguration;
    private final CompletionListener completionListener;

    MenuConfigurationUpdateOperation(ISdl internalInterface, WindowCapability windowCapability, MenuConfiguration menuConfiguration, CompletionListener completionListener) {
        super(TAG);
        this.internalInterface = new WeakReference<>(internalInterface);
        this.availableMenuLayouts = windowCapability != null ? windowCapability.getMenuLayoutsAvailable() : null;
        this.updatedMenuConfiguration = menuConfiguration;
        this.completionListener = completionListener;
    }

    @Override
    public void onExecute() {
        start();
    }

    private void start() {
        if (getState() == Task.CANCELED) {
            return;
        }

        sendSetGlobalProperties(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                finishOperation(success);
            }
        });
    }

    private void sendSetGlobalProperties(final CompletionListener listener) {
        if (internalInterface.get() == null) {
            listener.onComplete(false);
            return;
        }

        SdlMsgVersion sdlMsgVersion = internalInterface.get().getSdlMsgVersion();
        if (sdlMsgVersion == null) {
            DebugTool.logError(TAG, "SDL Message Version is null. Cannot set Menu Configuration");
            listener.onComplete(false);
            return;
        }

        if (sdlMsgVersion.getMajorVersion() < 6) {
            DebugTool.logWarning(TAG, "Menu configurations is only supported on head units with RPC spec version 6.0.0 or later. Currently connected head unit RPC spec version is: " + sdlMsgVersion.getMajorVersion() + "." + sdlMsgVersion.getMinorVersion() + "." + sdlMsgVersion.getPatchVersion());
            listener.onComplete(false);
            return;
        }

        if (updatedMenuConfiguration.getMenuLayout() == null) {
            DebugTool.logInfo(TAG, "Menu Layout is null, not sending setGlobalProperties");
            listener.onComplete(false);
            return;
        }

        if (availableMenuLayouts == null) {
            DebugTool.logWarning(TAG, "Could not set the main menu configuration. Which menu layouts can be used is not available");
            listener.onComplete(false);
            return;
        } else if (!availableMenuLayouts.contains(updatedMenuConfiguration.getMenuLayout()) || !availableMenuLayouts.contains(updatedMenuConfiguration.getSubMenuLayout())) {
            DebugTool.logError(TAG, String.format("One or more of the set menu layouts are not available on this system. The menu configuration will not be set. Available menu layouts: %s, set menu layouts: %s", availableMenuLayouts, updatedMenuConfiguration));
            listener.onComplete(false);
            return;
        }

        SetGlobalProperties setGlobalProperties = new SetGlobalProperties();
        setGlobalProperties.setMenuLayout(updatedMenuConfiguration.getMenuLayout());
        setGlobalProperties.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getSuccess()) {
                    DebugTool.logInfo(TAG, "Menu Configuration successfully set: " + updatedMenuConfiguration.toString());
                } else {
                    DebugTool.logError(TAG, "onError: " + response.getResultCode() + " | Info: " + response.getInfo());
                }
                listener.onComplete(response.getSuccess());
            }
        });
        if (internalInterface.get() != null) {
            internalInterface.get().sendRPC(setGlobalProperties);
        }
    }

    private void finishOperation(boolean success) {
        if (completionListener != null) {
            completionListener.onComplete(success);
        }
        onFinished();
    }
}
