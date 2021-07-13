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

import androidx.annotation.NonNull;

import com.livio.taskmaster.Queue;
import com.livio.taskmaster.Task;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.OnSystemCapabilityListener;
import com.smartdevicelink.managers.lifecycle.SystemCapabilityManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.DisplayCapability;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.DisplayType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.PredefinedWindows;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.SystemContext;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

abstract class BaseMenuManager extends BaseSubManager {
    private static final String TAG = "BaseMenuManager";
    static final int menuCellIdMin = 1;
    static final int parentIdNotFound = 2000000000;

    final WeakReference<FileManager> fileManager;
    List<MenuCell> currentMenuCells;
    List<MenuCell> menuCells;
    DynamicMenuUpdatesMode dynamicMenuUpdatesMode;
    MenuConfiguration currentMenuConfiguration;
    MenuConfiguration menuConfiguration;
    private String displayType;
    HMILevel currentHMILevel;
    SystemContext currentSystemContext;
    OnRPCNotificationListener hmiListener;
    OnRPCNotificationListener commandListener;
    OnSystemCapabilityListener onDisplaysCapabilityListener;
    WindowCapability windowCapability;
    private Queue transactionQueue;
    int lastMenuId;

    BaseMenuManager(@NonNull ISdl internalInterface, @NonNull FileManager fileManager) {
        super(internalInterface);

        this.lastMenuId = menuCellIdMin;
        this.menuConfiguration = new MenuConfiguration(null, null);
        this.menuCells = new ArrayList<>();
        this.currentMenuCells = new ArrayList<>();
        this.dynamicMenuUpdatesMode = DynamicMenuUpdatesMode.ON_WITH_COMPAT_MODE;
        this.transactionQueue = newTransactionQueue();
        this.fileManager = new WeakReference<>(fileManager);
        this.currentSystemContext = SystemContext.SYSCTXT_MAIN;
        this.currentHMILevel = HMILevel.HMI_NONE;
        addListeners();
    }

    @Override
    public void start(CompletionListener listener) {
        transitionToState(READY);
        super.start(listener);
    }

    @Override
    public void dispose() {
        lastMenuId = menuCellIdMin;
        menuCells = new ArrayList<>();
        currentMenuCells = new ArrayList<>();
        currentHMILevel = HMILevel.HMI_NONE;
        currentSystemContext = SystemContext.SYSCTXT_MAIN;
        dynamicMenuUpdatesMode = DynamicMenuUpdatesMode.ON_WITH_COMPAT_MODE;
        windowCapability = null;
        menuConfiguration = null;
        currentMenuConfiguration = null;

        // Cancel the operations
        if (transactionQueue != null) {
            transactionQueue.close();
            transactionQueue = null;
        }

        // Remove listeners
        internalInterface.removeOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);
        internalInterface.removeOnRPCNotificationListener(FunctionID.ON_COMMAND, commandListener);
        if (internalInterface.getSystemCapabilityManager() != null) {
            internalInterface.getSystemCapabilityManager().removeOnSystemCapabilityListener(SystemCapabilityType.DISPLAYS, onDisplaysCapabilityListener);
        }

        super.dispose();
    }

    private Queue newTransactionQueue() {
        Queue queue = internalInterface.getTaskmaster().createQueue("MenuManager", 7, false);
        queue.pause();
        return queue;
    }

    // Suspend the queue if the HMI level is NONE since we want to delay sending RPCs until we're in non-NONE
    private void updateTransactionQueueSuspended() {
        if (currentHMILevel == HMILevel.HMI_NONE || currentSystemContext == SystemContext.SYSCTXT_MENU) {
            DebugTool.logInfo(TAG, String.format("Suspending the transaction queue. Current HMI level is: %s, current system context is: %s", currentHMILevel, currentSystemContext));
            transactionQueue.pause();
        } else {
            DebugTool.logInfo(TAG, "Starting the transaction queue");
            transactionQueue.resume();
        }
    }

    public void setDynamicUpdatesMode(@NonNull DynamicMenuUpdatesMode value) {
        this.dynamicMenuUpdatesMode = value;
    }

    /**
     * @return The currently set DynamicMenuUpdatesMode. It defaults to ON_WITH_COMPAT_MODE
     */
    public DynamicMenuUpdatesMode getDynamicMenuUpdatesMode() {
        return this.dynamicMenuUpdatesMode;
    }

    /**
     * Creates and sends all associated Menu RPCs
     *
     * @param cells - the menu cells that are to be sent to the head unit, including their sub-cells.
     */
    public void setMenuCells(@NonNull List<MenuCell> cells) {
        // Create a deep copy of the list so future changes by developers don't affect the algorithm logic
        final List<MenuCell> clonedCells = cloneMenuCellsList(cells);

        // HashSet order doesn't matter / doesn't allow duplicates
        HashSet<String> titleCheckSet = new HashSet<>();
        HashSet<String> allMenuVoiceCommands = new HashSet<>();
        int voiceCommandCount = 0;
        for (MenuCell cell : clonedCells) {
            titleCheckSet.add(cell.getTitle());
            if (cell.getVoiceCommands() != null) {
                allMenuVoiceCommands.addAll(cell.getVoiceCommands());
                voiceCommandCount += cell.getVoiceCommands().size();
            }
        }

        // Check for duplicate titles
        if (titleCheckSet.size() != clonedCells.size()) {
            DebugTool.logError(TAG, "Not all cell titles are unique. The menu will not be set");
            return;
        }

        // Check for duplicate voice commands
        if (allMenuVoiceCommands.size() != voiceCommandCount) {
            DebugTool.logError(TAG, "Attempted to create a menu with duplicate voice commands. Voice commands must be unique. The menu will not be set");
            return;
        }

        updateIdsOnMenuCells(clonedCells, parentIdNotFound);

        menuCells = new ArrayList<>(clonedCells);

        boolean isDynamicMenuUpdateActive = isDynamicMenuUpdateActive(dynamicMenuUpdatesMode, displayType);
        Task operation = new MenuReplaceOperation(internalInterface, fileManager.get(), windowCapability, menuConfiguration, currentMenuCells, menuCells, isDynamicMenuUpdateActive, new MenuManagerCompletionListener() {
            @Override
            public void onComplete(boolean success, List<MenuCell> currentMenuCells) {
                BaseMenuManager.this.currentMenuCells = currentMenuCells;
                updateMenuReplaceOperationsWithNewCurrentMenu();
            }
        });

        // Cancel previous MenuReplaceOperations
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuReplaceOperation) {
                task.cancelTask();
            }
        }

        transactionQueue.add(operation, false);
    }

    /**
     * Returns current list of menu cells
     *
     * @return a List of Currently set menu cells
     */
    public List<MenuCell> getMenuCells() {
        return menuCells;
    }

    private boolean openMenuPrivate(MenuCell cell) {
        MenuCell foundClonedCell = null;

        if (cell != null) {
            // We must see if we have a copy of this cell, since we clone the objects
            for (MenuCell clonedCell : currentMenuCells) {
                if (clonedCell.equals(cell) && clonedCell.getCellId() != parentIdNotFound) {
                    // We've found the correct sub menu cell
                    foundClonedCell = clonedCell;
                    break;
                }
            }
        }

        if (cell != null && (cell.getSubCells() == null || cell.getSubCells().isEmpty())) {
            DebugTool.logError(DebugTool.TAG, String.format("The cell %s does not contain any sub cells, so no submenu can be opened", cell.getTitle()));
            return false;
        } else if (cell != null && foundClonedCell == null) {
            DebugTool.logError(DebugTool.TAG, "This cell has not been sent to the head unit, so no submenu can be opened. Make sure that the cell exists in the SDLManager.menu array");
            return false;
        } else if (internalInterface.getSdlMsgVersion().getMajorVersion() < 6) {
            DebugTool.logWarning(TAG, "The openSubmenu method is not supported on this head unit.");
            return false;
        }

        // Create the operation
        MenuShowOperation operation = new MenuShowOperation(internalInterface, foundClonedCell);

        // Cancel previous open menu operations
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuShowOperation) {
                task.cancelTask();
            }
        }

        transactionQueue.add(operation, false);

        return true;
    }

    /**
     * Opens the Main Menu
     */
    public boolean openMenu() {
        return openMenuPrivate(null);
    }

    /**
     * Opens a subMenu.
     *
     * @param cell - A <Strong>SubMenu</Strong> cell whose sub menu you wish to open
     */
    public boolean openSubMenu(@NonNull MenuCell cell) {
        return openMenuPrivate(cell);
    }


    /**
     * This method is called via the screen manager to set the menuConfiguration.
     * The menuConfiguration.SubMenuLayout value will be used when a menuCell with sub-cells has a null value for SubMenuLayout
     *
     * @param menuConfiguration - The default menuConfiguration
     */
    public void setMenuConfiguration(@NonNull final MenuConfiguration menuConfiguration) {
        if (menuConfiguration.equals(this.menuConfiguration)) {
            DebugTool.logInfo(TAG, "New menu configuration is equal to existing one, will not set new configuration");
            return;
        }

        this.menuConfiguration = menuConfiguration;

        MenuConfigurationUpdateOperation operation = new MenuConfigurationUpdateOperation(internalInterface, windowCapability, menuConfiguration, new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (!success) {
                    DebugTool.logError(TAG, "Error setting new menu configuration. Will revert to old menu configuration.");
                } else {
                    BaseMenuManager.this.currentMenuConfiguration = menuConfiguration;
                    updateMenuReplaceOperationsWithNewMenuConfiguration();
                }
            }
        });

        // Cancel previous menu configuration operations
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuConfigurationUpdateOperation) {
                task.cancelTask();
            }
        }

        transactionQueue.add(operation, false);
    }

    public MenuConfiguration getMenuConfiguration() {
        return this.menuConfiguration;
    }

    private void addListeners() {
        onDisplaysCapabilityListener = new OnSystemCapabilityListener() {
            @Override
            public void onCapabilityRetrieved(Object capability) {
                // instead of using the parameter it's more safe to use the convenience method
                List<DisplayCapability> capabilities = SystemCapabilityManager.convertToList(capability, DisplayCapability.class);
                if (capabilities == null || capabilities.size() == 0) {
                    DebugTool.logError(TAG, "Capabilities sent here are null or empty");
                } else {
                    DisplayCapability display = capabilities.get(0);
                    displayType = display.getDisplayName();
                    for (WindowCapability windowCapability : display.getWindowCapabilities()) {
                        int currentWindowID = windowCapability.getWindowID() != null ? windowCapability.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
                        if (currentWindowID == PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                            BaseMenuManager.this.windowCapability = windowCapability;
                            updateMenuReplaceOperationsWithNewWindowCapability();
                        }
                    }
                }
            }

            @Override
            public void onError(String info) {
                DebugTool.logError(TAG, "Display Capability cannot be retrieved");
                windowCapability = null;
            }
        };
        if (internalInterface.getSystemCapabilityManager() != null) {
            this.internalInterface.getSystemCapabilityManager().addOnSystemCapabilityListener(SystemCapabilityType.DISPLAYS, onDisplaysCapabilityListener);
        }

        hmiListener = new OnRPCNotificationListener() {
            @Override
            public void onNotified(RPCNotification notification) {
                OnHMIStatus onHMIStatus = (OnHMIStatus) notification;
                if (onHMIStatus.getWindowID() != null && onHMIStatus.getWindowID() != PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                    return;
                }
                currentHMILevel = onHMIStatus.getHmiLevel();
                currentSystemContext = onHMIStatus.getSystemContext();
                updateTransactionQueueSuspended();
            }
        };
        internalInterface.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);

        commandListener = new OnRPCNotificationListener() {
            @Override
            public void onNotified(RPCNotification notification) {
                OnCommand onCommand = (OnCommand) notification;
                callListenerForCells(currentMenuCells, onCommand);
            }
        };
        internalInterface.addOnRPCNotificationListener(FunctionID.ON_COMMAND, commandListener);
    }

    private List<MenuCell> cloneMenuCellsList(List<MenuCell> originalList) {
        if (originalList == null) {
            return new ArrayList<>();
        }

        List<MenuCell> clone = new ArrayList<>();
        for (MenuCell menuCell : originalList) {
            clone.add(menuCell.clone());
        }
        return clone;
    }

    private boolean callListenerForCells(List<MenuCell> cells, OnCommand command) {
        if (cells == null || cells.isEmpty() || command == null) {
            return false;
        }

        for (MenuCell cell : cells) {
            if (cell.getCellId() == command.getCmdID() && cell.getMenuSelectionListener() != null) {
                cell.getMenuSelectionListener().onTriggered(command.getTriggerSource());
                return true;
            }
            if (cell.getSubCells() != null && !cell.getSubCells().isEmpty()) {
                // for each cell, if it has sub cells, recursively loop through those as well
                return callListenerForCells(cell.getSubCells(), command);
            }
        }

        return false;
    }

    private void updateMenuReplaceOperationsWithNewCurrentMenu() {
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuReplaceOperation) {
                ((MenuReplaceOperation) task).setCurrentMenu(this.currentMenuCells);
            }
        }
    }

    private void updateMenuReplaceOperationsWithNewWindowCapability() {
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuReplaceOperation) {
                ((MenuReplaceOperation) task).setWindowCapability(this.windowCapability);
            }
        }
    }

    private void updateMenuReplaceOperationsWithNewMenuConfiguration() {
        for (Task task : transactionQueue.getTasksAsList()) {
            if (task instanceof MenuReplaceOperation) {
                ((MenuReplaceOperation) task).setMenuConfiguration(currentMenuConfiguration);
            }
        }
    }

    private boolean isDynamicMenuUpdateActive(DynamicMenuUpdatesMode updateMode, String displayType) {
        if (updateMode.equals(DynamicMenuUpdatesMode.ON_WITH_COMPAT_MODE)) {
            if (displayType == null) {
                return true;
            }
            return (!displayType.equals(DisplayType.GEN3_8_INCH.toString()));
        } else if (updateMode.equals(DynamicMenuUpdatesMode.FORCE_OFF)) {
            return false;
        } else if (updateMode.equals(DynamicMenuUpdatesMode.FORCE_ON)) {
            return true;
        }

        return true;
    }

    private void updateIdsOnMenuCells(List<MenuCell> menuCells, int parentId) {
        for (MenuCell cell : menuCells) {
            cell.setCellId(lastMenuId++);
            if (parentId != parentIdNotFound) {
                cell.setParentCellId(parentId);
            }
            if (cell.getSubCells() != null && !cell.getSubCells().isEmpty()) {
                updateIdsOnMenuCells(cell.getSubCells(), cell.getCellId());
            }
        }
    }
}
