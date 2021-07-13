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

package com.smartdevicelink.managers.screen.menu;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.R;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.MenuLayout;
import com.smartdevicelink.proxy.rpc.enums.SystemContext;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.util.Version;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.smartdevicelink.managers.screen.menu.BaseMenuManager.menuCellIdMin;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.ADD;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.DELETE;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.KEEP;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * the Algorithm specific tests are defined based on: https://github.com/smartdevicelink/sdl_evolution/blob/master/proposals/0210-mobile-dynamic-menu-cell-updating.md
 */
@RunWith(AndroidJUnit4.class)
public class MenuManagerTests {

    private OnRPCNotificationListener onHMIStatusListener, commandListener;
    private MenuManager menuManager;
    private List<MenuCell> cells;
    private MenuCell mainCell1, mainCell4;

    // SETUP / HELPERS

    @Before
    public void setUp() throws Exception {

        cells = createTestCells();

        final ISdl internalInterface = mock(ISdl.class);
        FileManager fileManager = mock(FileManager.class);

        when(internalInterface.getSdlMsgVersion()).thenReturn(new SdlMsgVersion(new Version(6, 0, 0)));


        // When internalInterface.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, OnRPCNotificationListener) is called
        // inside MenuManager's constructor, then keep a reference to the OnRPCNotificationListener so we can trigger it later
        // to emulate what Core does when it sends OnHMIStatus notification
        Answer<Void> onHMIStatusAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                onHMIStatusListener = (OnRPCNotificationListener) args[1];
                return null;
            }
        };
        doAnswer(onHMIStatusAnswer).when(internalInterface).addOnRPCNotificationListener(eq(FunctionID.ON_HMI_STATUS), any(OnRPCNotificationListener.class));

        Answer<Void> onCommandAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                commandListener = (OnRPCNotificationListener) args[1];
                return null;
            }
        };
        doAnswer(onCommandAnswer).when(internalInterface).addOnRPCNotificationListener(eq(FunctionID.ON_COMMAND), any(OnRPCNotificationListener.class));

        // When internalInterface.sendRPCs() is called, call listener.onFinished() to fake the response
        final Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                List<RPCMessage> rpcs  = (List<RPCMessage>) args[0];
                OnMultipleRequestListener listener = (OnMultipleRequestListener) args[1];

                for (RPCMessage rpcMessage : rpcs) {
                    RPCRequest request = (RPCRequest) rpcMessage;
                    RPCResponse response = new RPCResponse(request.getFunctionID().toString());
                    response.setCorrelationID(request.getCorrelationID());
                    response.setSuccess(true);
                    listener.onResponse(request.getCorrelationID(), response);
                }

                listener.onFinished();
                return null;
            }
        };
        doAnswer(answer).when(internalInterface).sendRPCs(any(List.class), any(OnMultipleRequestListener.class));

        Answer<Void> setGlobalPropertiesAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RPCRequest request = (RPCRequest) args[0];
                RPCResponse response = new RPCResponse(FunctionID.SET_GLOBAL_PROPERTIES.toString());
                response.setSuccess(true);
                request.getOnRPCResponseListener().onResponse(0, response);
                return null;
            }
        };
        doAnswer(setGlobalPropertiesAnswer).when(internalInterface).sendRPC(any(SetGlobalProperties.class));

        // Create MenuManager
        Taskmaster taskmaster = new Taskmaster.Builder().build();
        taskmaster.start();
        when(internalInterface.getTaskmaster()).thenReturn(taskmaster);
        menuManager = new MenuManager(internalInterface, fileManager);

        // Check some stuff during setup
        assertEquals(HMILevel.HMI_NONE, menuManager.currentHMILevel);
        assertEquals(BaseSubManager.SETTING_UP, menuManager.getState());
        assertEquals(SystemContext.SYSCTXT_MAIN, menuManager.currentSystemContext);
        assertEquals(DynamicMenuUpdatesMode.ON_WITH_COMPAT_MODE, menuManager.dynamicMenuUpdatesMode);
        assertEquals(menuCellIdMin, menuManager.lastMenuId);
        assertTrue(menuManager.menuCells.isEmpty());
        assertTrue(menuManager.currentMenuCells.isEmpty());
        assertNull(menuManager.menuConfiguration.getMenuLayout());
        assertNull(menuManager.menuConfiguration.getSubMenuLayout());
        assertNotNull(menuManager.hmiListener);
        assertNotNull(menuManager.commandListener);
        assertNotNull(menuManager.onDisplaysCapabilityListener);
    }

    @Test
    public void testStartMenuManager() {
        menuManager.start(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                assertTrue(success);
                // Make sure the state has changed, as the Screen Manager is dependant on it
                assertEquals(BaseSubManager.READY, menuManager.getState());
            }
        });
    }

    @Test
    public void testHMINotReady() {
        menuManager.setMenuCells(cells);
        assertEquals(HMILevel.HMI_NONE, menuManager.currentHMILevel);
        assertTrue(menuManager.currentMenuCells.isEmpty());

        // The Menu Manager should send new menu once HMI full occurs
        sendFakeCoreOnHMIFullNotifications();

        // Listener should be triggered - which sets new HMI level and should proceed to send our pending update
        assertEquals(HMILevel.HMI_FULL, menuManager.currentHMILevel);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(cells, menuManager.currentMenuCells);
    }

    @Test
    public void testUpdatingOldWay() {
        // Force Menu Manager to use the old way of deleting / sending all
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_OFF);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_OFF);
        // when we only send one command to update, we should only be returned one add command
        List<MenuCell> newArray = Arrays.asList(mainCell1, mainCell4);
        assertEquals(MenuReplaceUtilities.allCommandsForCells(newArray, menuManager.fileManager.get(), menuManager.windowCapability, MenuLayout.LIST).size(), 4); // 1 root cells, 1 sub menu root cell, 2 sub menu cells
        menuManager.currentHMILevel = HMILevel.HMI_FULL;
        menuManager.setMenuCells(newArray);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        for (MenuCell cell : menuManager.currentMenuCells) {
            // grab 2 of our newly updated cells - 1 root and 1 sub cell, and make sure they can get triggered
            if (cell.getTitle().equalsIgnoreCase("Test Cell 1")) {
                // Fake onCommand - we want to make sure that we can pass back onCommand events to our root Menu Cell
                OnCommand onCommand = new OnCommand();
                onCommand.setCmdID(cell.getCellId());
                onCommand.setTriggerSource(TriggerSource.TS_MENU); // these are menu commands
                commandListener.onNotified(onCommand); // send off the notification

                // verify the mock listener has only been hit once for a root cell
                verify(cell.getMenuSelectionListener(), times(1)).onTriggered(TriggerSource.TS_MENU);
            }

            if (cell.getTitle().equalsIgnoreCase("SubCell 2")) {
                // Fake onCommand - we want to make sure that we can pass back onCommand events to our sub Menu Cell
                OnCommand onCommand2 = new OnCommand();
                onCommand2.setCmdID(cell.getCellId());
                onCommand2.setTriggerSource(TriggerSource.TS_MENU); // these are menu commands
                commandListener.onNotified(onCommand2); // send off the notification

                // verify the mock listener has only been hit once for a sub cell
                verify(cell.getMenuSelectionListener(), times(1)).onTriggered(TriggerSource.TS_MENU);
            }
        }
    }

    @Test
    public void testAlgorithmTest1() {
        // Force Menu Manager to use the new way
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_ON);

        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu1();
        List<MenuCell> newMenu = createDynamicMenu1New();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 4);

        // this happens in the menu manager but lets make sure its behaving
        DynamicMenuUpdateRunScore runScore = DynamicMenuUpdateAlgorithm.compareOldMenuCells(oldMenu, newMenu);

        List<MenuCellState> oldMenuStatus = Arrays.asList(KEEP, KEEP, KEEP, KEEP);
        List<MenuCellState> newMenuStatus = Arrays.asList(KEEP, KEEP, KEEP, KEEP, ADD);

        assertEquals(1, runScore.getScore());
        assertEquals(runScore.getOldStatus(), oldMenuStatus);
        assertEquals(runScore.getUpdatedStatus(), newMenuStatus);

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(5, menuManager.currentMenuCells.size());
        List<MenuCell> oldKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getOldStatus(), KEEP);
        List<MenuCell> newKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getUpdatedStatus(), KEEP);
        assertEquals(4, oldKeeps.size());
        assertEquals(4, newKeeps.size());
    }

    @Test
    public void testAlgorithmTest2() {

        // Force Menu Manager to use the new way
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_ON);

        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu2();
        List<MenuCell> newMenu = createDynamicMenu2New();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(4, menuManager.currentMenuCells.size());

        // this happens in the menu manager but lets make sure its behaving
        DynamicMenuUpdateRunScore runScore = DynamicMenuUpdateAlgorithm.compareOldMenuCells(oldMenu, newMenu);

        List<MenuCellState> oldMenuScore = Arrays.asList(KEEP, KEEP, KEEP, DELETE);
        List<MenuCellState> newMenuScore = Arrays.asList(KEEP, KEEP, KEEP);

        assertEquals(runScore.getScore(), 0);
        assertEquals(runScore.getOldStatus(), oldMenuScore);
        assertEquals(runScore.getUpdatedStatus(), newMenuScore);

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(3, menuManager.currentMenuCells.size());
        List<MenuCell> oldKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getOldStatus(), KEEP);
        List<MenuCell> newKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getUpdatedStatus(), KEEP);
        assertEquals(3, oldKeeps.size());
        assertEquals(3, newKeeps.size());
    }

    @Test
    public void testAlgorithmTest3() {

        // Force Menu Manager to use the new way
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_ON);

        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu3();
        List<MenuCell> newMenu = createDynamicMenu3New();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 3);

        // this happens in the menu manager but lets make sure its behaving
        DynamicMenuUpdateRunScore runScore = DynamicMenuUpdateAlgorithm.compareOldMenuCells(oldMenu, newMenu);

        List<MenuCellState> oldMenuStatus = Arrays.asList(DELETE, DELETE, DELETE);
        List<MenuCellState> newMenuStatus = Arrays.asList(ADD, ADD, ADD);

        assertEquals(runScore.getScore(), 3);
        assertEquals(runScore.getOldStatus(), oldMenuStatus);
        assertEquals(runScore.getUpdatedStatus(), newMenuStatus);

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 3);
        List<MenuCell> oldKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getOldStatus(), KEEP);
        List<MenuCell> newKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getUpdatedStatus(), KEEP);
        assertEquals(0, newKeeps.size());
        assertEquals(0, oldKeeps.size());
    }

    @Test
    public void testAlgorithmTest4() {

        // Force Menu Manager to use the new way
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_ON);

        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu4();
        List<MenuCell> newMenu = createDynamicMenu4New();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 4);

        // this happens in the menu manager but lets make sure its behaving
        DynamicMenuUpdateRunScore runScore = DynamicMenuUpdateAlgorithm.compareOldMenuCells(oldMenu, newMenu);

        List<MenuCellState> oldMenuStatus = Arrays.asList(KEEP, DELETE, KEEP, DELETE);
        List<MenuCellState> newMenuStatus = Arrays.asList(ADD, KEEP, ADD, KEEP);

        assertEquals(runScore.getScore(), 2);
        assertEquals(runScore.getOldStatus(), oldMenuStatus);
        assertEquals(runScore.getUpdatedStatus(), newMenuStatus);

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 4);
        List<MenuCell> oldKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getOldStatus(), KEEP);
        List<MenuCell> newKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getUpdatedStatus(), KEEP);
        assertEquals(2, newKeeps.size());
        assertEquals(2, oldKeeps.size());
    }

    @Test
    public void testAlgorithmTest5() {

        // Force Menu Manager to use the new way
        menuManager.setDynamicUpdatesMode(DynamicMenuUpdatesMode.FORCE_ON);
        assertEquals(menuManager.dynamicMenuUpdatesMode, DynamicMenuUpdatesMode.FORCE_ON);

        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu5();
        List<MenuCell> newMenu = createDynamicMenu5New();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 4);

        // this happens in the menu manager but lets make sure its behaving
        DynamicMenuUpdateRunScore runScore = DynamicMenuUpdateAlgorithm.compareOldMenuCells(oldMenu, newMenu);

        List<MenuCellState> oldMenuStatus = Arrays.asList(DELETE, KEEP, KEEP, KEEP);
        List<MenuCellState> newMenuStatus = Arrays.asList(KEEP, KEEP, KEEP, ADD);

        assertEquals(runScore.getScore(), 1);
        assertEquals(runScore.getOldStatus(), oldMenuStatus);
        assertEquals(runScore.getUpdatedStatus(), newMenuStatus);

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.currentMenuCells.size(), 4);
        List<MenuCell> oldKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getOldStatus(), KEEP);
        List<MenuCell> newKeeps = filterMenuCellsWithStatusList(menuManager.currentMenuCells, runScore.getUpdatedStatus(), KEEP);
        assertEquals(3, newKeeps.size());
        assertEquals(3, oldKeeps.size());
    }

    @Test
    public void testSettingNullMenu() {
        // Make sure we can send an empty menu with no issues
        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

       sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu1();
        List<MenuCell> newMenu = null;
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(4, menuManager.currentMenuCells.size());

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(0, menuManager.currentMenuCells.size());
    }

    @Test
    public void testClearingMenu() {
        // Make sure we can send an empty menu with no issues
        // start fresh
        menuManager.currentMenuCells = new ArrayList<>();
        menuManager.menuCells = new ArrayList<>();

        sendFakeCoreOnHMIFullNotifications();

        // send new cells. They should set the old way
        List<MenuCell> oldMenu = createDynamicMenu1();
        List<MenuCell> newMenu = Collections.emptyList();
        menuManager.setMenuCells(oldMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(4, menuManager.currentMenuCells.size());

        menuManager.setMenuCells(newMenu);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(0 , menuManager.currentMenuCells.size());
    }

    @Test
    public void testOpeningMainMenu() {
        // call open Menu
        MenuManager mockMenuManager = mock(MenuManager.class);
        mockMenuManager.openMenu();
        verify(mockMenuManager, Mockito.times(1)).openMenu();
    }

    @Test
    public void testOpeningSubMenuNullCells() {
        // call open Menu
        MenuManager mockMenuManager = mock(MenuManager.class);
        MenuCell cell = mock(MenuCell.class);
        mockMenuManager.currentMenuCells = null;
        assertFalse(mockMenuManager.openSubMenu(cell));
    }

    @Test
    public void testOpeningSubMenu() {
        // call open Menu
        List<MenuCell> testCells = createTestCells();
        menuManager.setMenuCells(testCells);

        sendFakeCoreOnHMIFullNotifications();

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        // Has to get success response to be true
        MenuCell submenu = testCells.get(3);
        assertTrue(menuManager.openSubMenu(submenu));
    }

    @Test
    public void testSetMenuConfiguration() {
        sendFakeCoreOnHMIFullNotifications();
        menuManager.windowCapability = new WindowCapability();
        menuManager.windowCapability.setMenuLayoutsAvailable(Arrays.asList(MenuLayout.LIST, MenuLayout.TILES));

        MenuConfiguration menuConfigurationTest = new MenuConfiguration(MenuLayout.LIST, MenuLayout.LIST);
        menuManager.setMenuConfiguration(menuConfigurationTest);

        // Sleep to give time to Taskmaster to run the operations
        sleep();

        assertEquals(menuManager.menuConfiguration, menuConfigurationTest);
    }

    // HELPERS

    // Emulate what happens when Core sends OnHMIStatus notification
    private void sendFakeCoreOnHMIFullNotifications() {
        OnHMIStatus onHMIStatusFakeNotification = new OnHMIStatus();
        onHMIStatusFakeNotification.setHmiLevel(HMILevel.HMI_FULL);
        onHMIStatusFakeNotification.setSystemContext(SystemContext.SYSCTXT_MAIN);
        onHMIStatusListener.onNotified(onHMIStatusFakeNotification);
    }

    // CREATING CELLS FOR TEST CASES

    private List<MenuCell> createTestCells() {

        // menu cell mock listener
        MenuSelectionListener menuSelectionListener1 = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListener2 = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListener3 = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerSub1 = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerSub2 = mock(MenuSelectionListener.class);

        // some arts
        SdlArtwork livio = new SdlArtwork("livio", FileType.GRAPHIC_PNG, R.drawable.sdl_lockscreen_icon, false);

        // some menu cells
        List<String> voice2 = Collections.singletonList("Cell two");

        mainCell1 = new MenuCell("Test Cell 1", livio, null, menuSelectionListener1);
        MenuCell mainCell2 = new MenuCell("Test Cell 2", livio, voice2, menuSelectionListener2);
        MenuCell mainCell3 = new MenuCell("Test Cell 3", null, null, menuSelectionListener3);

        // SUB MENU
        MenuCell subCell1 = new MenuCell("SubCell 1", null, null, menuSelectionListenerSub1);
        MenuCell subCell2 = new MenuCell("SubCell 2", null, null, menuSelectionListenerSub2);

        mainCell4 = new MenuCell("Test Cell 4", null, livio, Arrays.asList(subCell1, subCell2)); // sub menu parent cell
        mainCell4.setCellId(4);

        return Arrays.asList(mainCell1, mainCell2, mainCell3, mainCell4);
    }

    private List<MenuCell> createDynamicMenu1() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(A, B, C, D);

    }

    private List<MenuCell> createDynamicMenu1New() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerE = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        MenuCell E = new MenuCell("E", null, null, menuSelectionListenerE);

        return Arrays.asList(A, B, C, D, E);

    }

    private List<MenuCell> createDynamicMenu2() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(A, B, C, D);

    }

    private List<MenuCell> createDynamicMenu2New() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        return Arrays.asList(A, B, C);

    }

    private List<MenuCell> createDynamicMenu3() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        return Arrays.asList(A, B, C);

    }

    private List<MenuCell> createDynamicMenu3New() {

        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerE = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerF = mock(MenuSelectionListener.class);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        MenuCell E = new MenuCell("E", null, null, menuSelectionListenerE);

        MenuCell F = new MenuCell("F", null, null, menuSelectionListenerF);

        return Arrays.asList(D, E, F);

    }

    private List<MenuCell> createDynamicMenu4() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(A, B, C, D);

    }

    private List<MenuCell> createDynamicMenu4New() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(B, A, D, C);

    }

    private List<MenuCell> createDynamicMenu5() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(A, B, C, D);

    }

    private List<MenuCell> createDynamicMenu5New() {

        MenuSelectionListener menuSelectionListenerA = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerB = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerC = mock(MenuSelectionListener.class);
        MenuSelectionListener menuSelectionListenerD = mock(MenuSelectionListener.class);

        MenuCell A = new MenuCell("A", null, null, menuSelectionListenerA);

        MenuCell B = new MenuCell("B", null, null, menuSelectionListenerB);

        MenuCell C = new MenuCell("C", null, null, menuSelectionListenerC);

        MenuCell D = new MenuCell("D", null, null, menuSelectionListenerD);

        return Arrays.asList(B, C, D, A);

    }

    private List<MenuCell> filterMenuCellsWithStatusList(List<MenuCell> menuCells, List<DynamicMenuUpdateAlgorithm.MenuCellState> statusList, DynamicMenuUpdateAlgorithm.MenuCellState menuCellState) {
        List<MenuCell> filteredCells = new ArrayList<>();
        for (int index = 0; index < statusList.size(); index++) {
            if (statusList.get(index).equals(menuCellState)) {
                filteredCells.add(menuCells.get(index));
            }
        }
        return filteredCells;
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
