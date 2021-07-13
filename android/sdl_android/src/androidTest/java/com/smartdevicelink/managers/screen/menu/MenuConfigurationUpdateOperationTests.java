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

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.livio.taskmaster.Queue;
import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.MenuLayout;
import com.smartdevicelink.util.Version;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Random;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MenuConfigurationUpdateOperationTests {

    private Handler mainHandler;
    private Taskmaster taskmaster;
    private Queue transactionQueue;

    @Before
    public void setUp() throws Exception {
        mainHandler = new Handler(getInstrumentation().getTargetContext().getMainLooper());
        taskmaster = new Taskmaster.Builder().build();
        taskmaster.start();
        transactionQueue = taskmaster.createQueue("MenuManager", new Random().nextInt(), false);
    }

    @Test
    public void testSuccess() {
        final ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(7, 0, 0)));
        doAnswer(createSetGlobalPropertiesAnswer(true)).when(internalInterface).sendRPC(any(SetGlobalProperties.class));
        WindowCapability windowCapability = createWindowCapability(true, true);
        MenuConfiguration menuConfiguration = new MenuConfiguration(MenuLayout.LIST, MenuLayout.LIST);
        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(final boolean success) {
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        assertTrue(success);
                        verify(internalInterface, Mockito.times(1)).sendRPC(any(SetGlobalProperties.class));
                    }
                });
            }
        });
        transactionQueue.add(operation, false);
    }

    @Test
    public void testFailsRPCVersionOld() {
        final ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(5, 0, 0)));
        doAnswer(createSetGlobalPropertiesAnswer(true)).when(internalInterface).sendRPC(any(SetGlobalProperties.class));
        WindowCapability windowCapability = createWindowCapability(true, true);
        MenuConfiguration menuConfiguration = new MenuConfiguration(MenuLayout.LIST, MenuLayout.LIST);
        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(final boolean success) {
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(success);
                        verify(internalInterface, Mockito.times(0)).sendRPC(any(SetGlobalProperties.class));
                    }
                });
            }
        });
        transactionQueue.add(operation, false);
    }

    @Test
    public void testFailsMenuLayoutNotSet() {
        final ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(7, 0, 0)));
        doAnswer(createSetGlobalPropertiesAnswer(true)).when(internalInterface).sendRPC(any(SetGlobalProperties.class));
        WindowCapability windowCapability = createWindowCapability(true, true);
        MenuConfiguration menuConfiguration = new MenuConfiguration(null, MenuLayout.LIST);
        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(final boolean success) {
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(success);
                        verify(internalInterface, Mockito.times(0)).sendRPC(any(SetGlobalProperties.class));
                    }
                });
            }
        });
        transactionQueue.add(operation, false);
    }

    @Test
    public void testFailsMenuLayoutsAvailableEmpty() {
        final ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(7, 0, 0)));
        doAnswer(createSetGlobalPropertiesAnswer(true)).when(internalInterface).sendRPC(any(SetGlobalProperties.class));
        WindowCapability windowCapability = createWindowCapability(true, true);
        MenuConfiguration menuConfiguration = new MenuConfiguration(null, null);
        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(final boolean success) {
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(success);
                        verify(internalInterface, Mockito.times(0)).sendRPC(any(SetGlobalProperties.class));
                    }
                });
            }
        });
        transactionQueue.add(operation, false);
    }

    @Test
    public void testFailsRPCNotSent() {
        final ISdl internalInterface = mock(ISdl.class);
        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(7, 0, 0)));
        doAnswer(createSetGlobalPropertiesAnswer(false)).when(internalInterface).sendRPC(any(SetGlobalProperties.class));
        WindowCapability windowCapability = createWindowCapability(true, true);
        MenuConfiguration menuConfiguration = new MenuConfiguration(MenuLayout.LIST, MenuLayout.LIST);
        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(final boolean success) {
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(success);
                        verify(internalInterface, Mockito.times(1)).sendRPC(any(SetGlobalProperties.class));
                    }
                });
            }
        });
        transactionQueue.add(operation, false);
    }

    private Answer<Void> createSetGlobalPropertiesAnswer(final boolean success){
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RPCRequest request = (RPCRequest) args[0];
                RPCResponse response = new RPCResponse(request.getFunctionID().toString());
                response.setSuccess(success);
                request.getOnRPCResponseListener().onResponse(request.getCorrelationID(), response);
                return null;
            }
        };
    }

    private WindowCapability createWindowCapability (boolean supportsList, boolean supportsTile) {
        WindowCapability windowCapability = new WindowCapability();
        windowCapability.setMenuLayoutsAvailable(new ArrayList<MenuLayout>());
        if (supportsList) {
            windowCapability.getMenuLayoutsAvailable().add(MenuLayout.LIST);
        }
        if (supportsTile) {
            windowCapability.getMenuLayoutsAvailable().add(MenuLayout.TILES);
        }
        return windowCapability;
    }

    // Asserts on Taskmaster threads will fail silently so we need to do the assertions on main thread if the code is triggered from Taskmaster
    private void assertOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
