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
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.ShowAppMenu;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;

/**
 * Created by Bilal Alsharifi on 1/21/21.
 */
class MenuShowOperation extends Task {
    private static final String TAG = "MenuShowOperation";
    private final WeakReference<ISdl> internalInterface;
    private final MenuCell submenuCell;

    MenuShowOperation(ISdl internalInterface, MenuCell menuCell) {
        super(TAG);
        this.internalInterface = new WeakReference<>(internalInterface);
        this.submenuCell = menuCell;
    }

    @Override
    public void onExecute() {
        start();
    }

    private void start() {
        if (getState() == Task.CANCELED) {
            return;
        }

        Integer menuId = submenuCell != null ? submenuCell.getCellId() : null;
        sendShowAppMenu(menuId);
    }

    private void sendShowAppMenu(Integer id) {
        ShowAppMenu showAppMenu = new ShowAppMenu();
        showAppMenu.setMenuID(id);
        showAppMenu.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getSuccess()) {
                    DebugTool.logInfo(TAG, "Successfully opened application menu");
                } else {
                    DebugTool.logError(TAG, "Open Menu Request Failed. Result code: " + response.getResultCode()  + ". Info: "+ response.getInfo());
                }
                onFinished();
            }
        });

        if (internalInterface.get() != null) {
            internalInterface.get().sendRPC(showAppMenu);
        }
    }
}
