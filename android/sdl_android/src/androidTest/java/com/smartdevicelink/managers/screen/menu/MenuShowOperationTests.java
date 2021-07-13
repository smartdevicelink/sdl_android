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
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.ShowAppMenu;
import com.smartdevicelink.proxy.rpc.enums.MenuLayout;
import com.smartdevicelink.test.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Random;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class MenuShowOperationTests {
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
    public void testOpenMainMenu() {
        final ISdl internalInterface = mock(ISdl.class);
        MenuCell menuCell = null;
        Integer menuIdToAssert = menuCell != null ? menuCell.getCellId() : null;
        Answer<Void> showAppMenuAnswer = createShowAppMenuAnswer(true, menuIdToAssert);
        doAnswer(showAppMenuAnswer).when(internalInterface).sendRPC(any(ShowAppMenu.class));
        MenuShowOperation operation = new MenuShowOperation(internalInterface, menuCell);
        transactionQueue.add(operation, false);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        verify(internalInterface, Mockito.times(1)).sendRPC(any(ShowAppMenu.class));
    }

    @Test
    public void testOpenSubMenu() {
        final ISdl internalInterface = mock(ISdl.class);
        MenuCell menuCell = new MenuCell(TestValues.GENERAL_STRING, MenuLayout.TILES, null, null);
        menuCell.setCellId(TestValues.GENERAL_INT);
        Integer menuIdToAssert = menuCell != null ? menuCell.getCellId() : null;
        Answer<Void> showAppMenuAnswer = createShowAppMenuAnswer(true, menuIdToAssert);
        doAnswer(showAppMenuAnswer).when(internalInterface).sendRPC(any(ShowAppMenu.class));
        MenuShowOperation operation = new MenuShowOperation(internalInterface, menuCell);
        transactionQueue.add(operation, false);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        verify(internalInterface, Mockito.times(1)).sendRPC(any(ShowAppMenu.class));
    }

    private Answer<Void> createShowAppMenuAnswer(final boolean success, final Integer menuIdToAssert){
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                final RPCRequest request = (RPCRequest) args[0];
                assertOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ShowAppMenu showAppMenu = (ShowAppMenu) request;
                        assertEquals(showAppMenu.getMenuID(),  menuIdToAssert);
                    }
                });
                RPCResponse response = new RPCResponse(request.getFunctionID().toString());
                response.setSuccess(success);
                request.getOnRPCResponseListener().onResponse(request.getCorrelationID(), response);
                return null;
            }
        };
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Asserts on Taskmaster threads will fail silently so we need to do the assertions on main thread if the code is triggered from Taskmaster
    private void assertOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
