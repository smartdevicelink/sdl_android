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
 *
 * Created by brettywhite on 2019-06-11.
 */

package com.smartdevicelink.managers.screen.choiceset;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.livio.taskmaster.Queue;
import com.livio.taskmaster.Task;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.ChoiceSetCompletionListener;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.ManagerUtility;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.lifecycle.OnSystemCapabilityListener;
import com.smartdevicelink.managers.lifecycle.SystemCapabilityManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.DisplayCapability;
import com.smartdevicelink.proxy.rpc.KeyboardCapabilities;
import com.smartdevicelink.proxy.rpc.KeyboardLayoutCapability;
import com.smartdevicelink.proxy.rpc.KeyboardProperties;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.ImageFieldName;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.KeyboardLayout;
import com.smartdevicelink.proxy.rpc.enums.KeypressMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.PredefinedWindows;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.SystemContext;
import com.smartdevicelink.proxy.rpc.enums.TextFieldName;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * <strong>ChoiceSetManager</strong> <br>
 * <p>
 * Note: This class must be accessed through the SdlManager. Do not instantiate it by itself. <br>
 */
abstract class BaseChoiceSetManager extends BaseSubManager {
    private static final String TAG = "BaseChoiceSetManager";
    // additional state
    private static final int CHECKING_VOICE = 0xA0;
    private KeyboardProperties keyboardConfiguration;
    final WeakReference<FileManager> fileManager;

    OnRPCNotificationListener hmiListener;
    OnSystemCapabilityListener onDisplayCapabilityListener;
    HMILevel currentHMILevel;
    WindowCapability defaultMainWindowCapability;
    String displayName;
    SystemContext currentSystemContext;
    HashSet<ChoiceCell> preloadedChoices, pendingPreloadChoices;
    ChoiceSet pendingPresentationSet;

    // We will pass operations into this to be completed
    final Queue transactionQueue;
    Task pendingPresentOperation;

    PresentKeyboardOperation currentlyPresentedKeyboardOperation;

    int nextChoiceId;
    int nextCancelId;
    final int choiceCellIdMin = 1;
    private final int choiceCellCancelIdMin = 101;
    private final int choiceCellCancelIdMax = 200;
    boolean isVROptional;

    BaseChoiceSetManager(@NonNull ISdl internalInterface, @NonNull FileManager fileManager) {
        super(internalInterface);

        // prepare operations queue
        transactionQueue = internalInterface.getTaskmaster().createQueue("ChoiceSetManagerQueue", 1, false);
        transactionQueue.pause(); // pause until HMI ready

        // capabilities
        currentSystemContext = SystemContext.SYSCTXT_MAIN;
        currentHMILevel = HMILevel.HMI_NONE;
        keyboardConfiguration = defaultKeyboardConfiguration();
        addListeners();

        // setting/instantiating class vars
        this.fileManager = new WeakReference<>(fileManager);
        preloadedChoices = new HashSet<>();
        pendingPreloadChoices = new HashSet<>();
        nextChoiceId = choiceCellIdMin;
        nextCancelId = choiceCellCancelIdMin;
        isVROptional = false;

        currentlyPresentedKeyboardOperation = null;
    }

    @Override
    public void start(CompletionListener listener) {
        transitionToState(CHECKING_VOICE);
        checkVoiceOptional();
        super.start(listener);
    }

    @Override
    public void dispose() {

        // cancel the operations
        transactionQueue.close();

        currentHMILevel = null;
        currentSystemContext = null;
        defaultMainWindowCapability = null;

        pendingPresentationSet = null;
        pendingPresentOperation = null;
        isVROptional = false;
        nextChoiceId = choiceCellIdMin;
        nextCancelId = choiceCellCancelIdMin;

        // remove listeners
        internalInterface.removeOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);
        if (internalInterface.getSystemCapabilityManager() != null) {
            internalInterface.getSystemCapabilityManager().removeOnSystemCapabilityListener(SystemCapabilityType.DISPLAYS, onDisplayCapabilityListener);
        }

        super.dispose();
    }

    private void checkVoiceOptional() {

        CheckChoiceVROptionalOperation checkChoiceVR = new CheckChoiceVROptionalOperation(internalInterface, new CheckChoiceVROptionalInterface() {
            @Override
            public void onCheckChoiceVROperationComplete(boolean vrOptional) {
                isVROptional = vrOptional;
                transitionToState(READY);
                DebugTool.logInfo(TAG, "VR is optional: " + isVROptional);
            }

            @Override
            public void onError(String error) {
                // At this point, there were errors trying to send a test PICS
                // If we reach this state, we cannot use the manager
                DebugTool.logError(TAG, error);
                transitionToState(ERROR);
                // checking VR will always be first in the queue.
                // If pre-load operations were added while this was in progress
                // clear it from the queue onError.
                transactionQueue.clear();
            }
        });
        transactionQueue.add(checkChoiceVR, false);
    }

    /**
     * Preload choices to improve performance while presenting a choice set at a later time
     *
     * @param choices  - a list of ChoiceCell objects that will be part of a choice set later
     * @param listener - a completion listener to inform when the operation is complete
     */
    public void preloadChoices(@NonNull List<ChoiceCell> choices, @Nullable final CompletionListener listener) {

        if (getState() == ERROR) {
            return;
        }

        LinkedHashSet<ChoiceCell> mutableChoicesToUpload = getChoicesToBeUploadedWithArray(choices);

        mutableChoicesToUpload.removeAll(preloadedChoices);
        mutableChoicesToUpload.removeAll(pendingPreloadChoices);

        final LinkedHashSet<ChoiceCell> choicesToUpload = (LinkedHashSet<ChoiceCell>) mutableChoicesToUpload.clone();

        if (choicesToUpload.size() == 0) {
            if (listener != null) {
                listener.onComplete(true);
            }
            return;
        }

        updateIdsOnChoices(choicesToUpload);

        // Add the preload cells to the pending preload choices
        pendingPreloadChoices.addAll(choicesToUpload);

        if (fileManager.get() != null) {
            PreloadChoicesOperation preloadChoicesOperation = new PreloadChoicesOperation(internalInterface, fileManager.get(), displayName, defaultMainWindowCapability, isVROptional, choicesToUpload, new ChoiceSetCompletionListener() {
                @Override
                public void onComplete(boolean success, HashSet<ChoiceCell> failedChoiceCells) {
                    if (success) {
                        preloadedChoices.addAll(choicesToUpload);
                        pendingPreloadChoices.removeAll(choicesToUpload);
                        if (listener != null) {
                            listener.onComplete(true);
                        }
                    } else {
                        DebugTool.logError(TAG, "There was an error pre loading choice cells");
                        choicesToUpload.removeAll(failedChoiceCells);
                        preloadedChoices.addAll(choicesToUpload);
                        pendingPreloadChoices.removeAll(choicesToUpload);
                        if (listener != null) {
                            listener.onComplete(false);
                        }
                    }
                }
            });

            transactionQueue.add(preloadChoicesOperation, false);
        } else {
            DebugTool.logError(TAG, "File Manager was null in preload choice operation");
        }
    }

    /**
     * Deletes choices that were sent previously
     *
     * @param choices - A list of ChoiceCell objects
     */
    public void deleteChoices(@NonNull List<ChoiceCell> choices) {

        if (getState() == ERROR) {
            DebugTool.logWarning(TAG, "Choice Manager In Error State");
            return;
        }

        // Find cells to be deleted that are already uploaded or are pending upload
        final HashSet<ChoiceCell> cellsToBeDeleted = choicesToBeDeletedWithArray(choices);
        HashSet<ChoiceCell> cellsToBeRemovedFromPending = choicesToBeRemovedFromPendingWithArray(choices);
        // If choices are deleted that are already uploaded or pending and are used by a pending presentation, cancel it and send an error
        HashSet<ChoiceCell> pendingPresentationChoices = new HashSet<>();
        if (pendingPresentationSet != null && pendingPresentationSet.getChoices() != null) {
            pendingPresentationChoices.addAll(pendingPresentationSet.getChoices());
        }

        if (pendingPresentOperation != null && pendingPresentOperation.getState() != Task.CANCELED && pendingPresentOperation.getState() != Task.FINISHED && (cellsToBeDeleted.retainAll(pendingPresentationChoices) || cellsToBeRemovedFromPending.retainAll(pendingPresentationChoices))) {
            pendingPresentOperation.cancelTask();
            DebugTool.logWarning(TAG, "Attempting to delete choice cells while there is a pending presentation operation. Pending presentation cancelled.");
            pendingPresentOperation = null;
        }

        // Remove cells from pending and delete choices
        pendingPresentationChoices.removeAll(cellsToBeRemovedFromPending);
        for (Task operation : transactionQueue.getTasksAsList()) {
            if (!(operation instanceof PreloadChoicesOperation)) {
                continue;
            }
            ((PreloadChoicesOperation) operation).removeChoicesFromUpload(cellsToBeRemovedFromPending);
        }

        // Find Choices to delete
        if (cellsToBeDeleted.size() == 0) {
            DebugTool.logInfo(TAG, "Cells to be deleted size == 0");
            return;
        }
        findIdsOnChoices(cellsToBeDeleted);

        DeleteChoicesOperation deleteChoicesOperation = new DeleteChoicesOperation(internalInterface, cellsToBeDeleted, new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (!success) {
                    DebugTool.logError(TAG, "Failed to delete choices");
                    return;
                }
                preloadedChoices.removeAll(cellsToBeDeleted);
            }
        });
        transactionQueue.add(deleteChoicesOperation, false);
    }

    /**
     * Presents a choice set
     *
     * @param choiceSet        - The choice set to be presented. This can include Choice Cells that were preloaded or not
     * @param mode             - The intended interaction mode
     * @param keyboardListener - A keyboard listener to capture user input
     */
    public void presentChoiceSet(@NonNull final ChoiceSet choiceSet, @Nullable final InteractionMode mode, @Nullable final KeyboardListener keyboardListener) {

        if (getState() == ERROR) {
            DebugTool.logWarning(TAG, "Choice Manager In Error State");
            return;
        }

        // Perform additional checks against the ChoiceSet
        if (!setUpChoiceSet(choiceSet)) {
            return;
        }

        if (this.pendingPresentationSet != null && pendingPresentOperation != null) {
            pendingPresentOperation.cancelTask();
            DebugTool.logWarning(TAG, "Presenting a choice set while one is currently presented. Cancelling previous and continuing");
        }

        this.pendingPresentationSet = choiceSet;
        preloadChoices(this.pendingPresentationSet.getChoices(), new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (!success) {
                    choiceSet.getChoiceSetSelectionListener().onError("There was an error pre-loading choice set choices");
                } else {
                    sendPresentOperation(keyboardListener, mode);
                }
            }
        });
    }

    private void sendPresentOperation(KeyboardListener keyboardListener, InteractionMode mode) {

        if (mode == null) {
            mode = InteractionMode.MANUAL_ONLY;
        }

        findIdsOnChoiceSet(pendingPresentationSet);

        // Pass back the information to the developer
        ChoiceSetSelectionListener privateChoiceListener = new ChoiceSetSelectionListener() {
            @Override
            public void onChoiceSelected(ChoiceCell choiceCell, TriggerSource triggerSource, int rowIndex) {
                if (pendingPresentationSet != null && pendingPresentationSet.getChoiceSetSelectionListener() != null) {
                    pendingPresentationSet.getChoiceSetSelectionListener().onChoiceSelected(choiceCell, triggerSource, rowIndex);
                }
            }

            @Override
            public void onError(String error) {
                if (pendingPresentationSet != null && pendingPresentationSet.getChoiceSetSelectionListener() != null) {
                    pendingPresentationSet.getChoiceSetSelectionListener().onError(error);
                }
            }
        };

        PresentChoiceSetOperation presentOp;

        if (keyboardListener == null) {
            // Non-searchable choice set
            DebugTool.logInfo(TAG, "Creating non-searchable choice set");
            presentOp = new PresentChoiceSetOperation(internalInterface, pendingPresentationSet, mode, null, null, privateChoiceListener, getNextCancelId());
        } else {
            // Searchable choice set
            DebugTool.logInfo(TAG, "Creating searchable choice set");
            presentOp = new PresentChoiceSetOperation(internalInterface, pendingPresentationSet, mode, keyboardConfiguration, keyboardListener, privateChoiceListener, getNextCancelId());
        }

        transactionQueue.add(presentOp, false);
        pendingPresentOperation = presentOp;
    }

    /**
     * Presents a keyboard on the Head unit to capture user input
     *
     * @param initialText          - The initial text that is used as a placeholder text. It might not work on some head units.
     * @param customKeyboardConfig - the custom keyboard configuration to be used when the keyboard is displayed
     * @param listener             - A keyboard listener to capture user input
     * @return - A unique id that can be used to cancel this keyboard. If `null`, no keyboard was created.
     */
    public Integer presentKeyboard(@NonNull String initialText, @Nullable KeyboardProperties customKeyboardConfig, @NonNull KeyboardListener listener) {
        if (initialText == null || initialText.length() == 0) {
            DebugTool.logError(TAG, "initialText cannot be an empty string.");
            return null;
        }

        if (getState() == ERROR) {
            DebugTool.logWarning(TAG, "Choice Manager In Error State");
            return null;
        }

        if (pendingPresentationSet != null && pendingPresentOperation != null) {
            pendingPresentOperation.cancelTask();
            pendingPresentationSet = null;
            DebugTool.logWarning(TAG, "There is a current or pending choice set, cancelling and continuing.");
        }

        customKeyboardConfig = createValidKeyboardConfigurationBasedOnKeyboardCapabilitiesFromConfiguration(customKeyboardConfig);
        if (customKeyboardConfig == null) {
            if (this.keyboardConfiguration != null) {
                customKeyboardConfig = this.keyboardConfiguration;
            } else {
                customKeyboardConfig = defaultKeyboardConfiguration();
            }
        }

        // Present a keyboard with the choice set that we used to test VR's optional state
        DebugTool.logInfo(TAG, "Presenting Keyboard - Choice Set Manager");
        Integer keyboardCancelID = getNextCancelId();
        PresentKeyboardOperation keyboardOp = new PresentKeyboardOperation(internalInterface, keyboardConfiguration, initialText, customKeyboardConfig, listener, keyboardCancelID);
        currentlyPresentedKeyboardOperation = keyboardOp;
        transactionQueue.add(keyboardOp, false);
        pendingPresentOperation = keyboardOp;
        return keyboardCancelID;
    }

    /**
     * Cancels the keyboard-only interface if it is currently showing. If the keyboard has not yet been sent to Core, it will not be sent.
     * <p>
     * This will only dismiss an already presented keyboard if connected to head units running SDL 6.0+.
     *
     * @param cancelID - The unique ID assigned to the keyboard, passed as the return value from `presentKeyboard`
     */
    public void dismissKeyboard(@NonNull Integer cancelID) {
        if (getState() == ERROR) {
            DebugTool.logWarning(TAG, "Choice Manager In Error State");
            return;
        }

        // First, attempt to cancel the currently executing keyboard operation (Once an operation has started it is removed from the operationQueue)
        if (currentlyPresentedKeyboardOperation != null && currentlyPresentedKeyboardOperation.getCancelID().equals(cancelID)) {
            currentlyPresentedKeyboardOperation.dismissKeyboard();
            return;
        }

        // Next, attempt to cancel keyboard operations that have not yet started
        for (Task operation : transactionQueue.getTasksAsList()) {
            if (!(operation instanceof PresentKeyboardOperation)) {
                continue;
            }

            PresentKeyboardOperation pendingOp = (PresentKeyboardOperation) operation;
            if (!(pendingOp.getCancelID().equals(cancelID))) {
                continue;
            }

            pendingOp.dismissKeyboard();
            break;
        }
    }

    /**
     * Set a custom keyboard configuration for this session. If set to null, it will reset to default keyboard configuration.
     *
     * @param keyboardConfiguration - the custom keyboard configuration to be used when the keyboard is displayed
     */
    public void setKeyboardConfiguration(@Nullable KeyboardProperties keyboardConfiguration) {
        KeyboardProperties properties = createValidKeyboardConfigurationBasedOnKeyboardCapabilitiesFromConfiguration(keyboardConfiguration);
        if (properties == null) {
            this.keyboardConfiguration = defaultKeyboardConfiguration();
        } else {
            this.keyboardConfiguration = properties;
        }
    }

    // Takes a keyboard configuration (SDLKeyboardProperties) and creates a valid version of it, if possible, based on this object's internal keyboardCapabilities
    private KeyboardProperties createValidKeyboardConfigurationBasedOnKeyboardCapabilitiesFromConfiguration(@Nullable KeyboardProperties keyboardConfiguration) {
        KeyboardCapabilities keyboardCapabilities = defaultMainWindowCapability != null ? defaultMainWindowCapability.getKeyboardCapabilities() : null;

        // If there are no keyboard capabilities, if there is no passed keyboard configuration, or if there is no layout to the passed keyboard configuration, just pass back the passed in configuration
        if (keyboardCapabilities == null || keyboardConfiguration == null || keyboardConfiguration.getKeyboardLayout() == null) {
            return keyboardConfiguration;
        }

        KeyboardLayoutCapability selectedLayoutCapability = null;
        for (KeyboardLayoutCapability layoutCapability : keyboardCapabilities.getSupportedKeyboards()) {
            if (layoutCapability.getKeyboardLayout().equals(keyboardConfiguration.getKeyboardLayout())) {
                selectedLayoutCapability = layoutCapability;
                break;
            }
        }

        if (selectedLayoutCapability == null) {
            DebugTool.logError(TAG, String.format("Configured keyboard layout is not supported: %s", keyboardConfiguration.getKeyboardLayout()));
            return null;
        }

        KeyboardProperties modifiedKeyboardConfiguration = (KeyboardProperties) keyboardConfiguration.clone();

        if (keyboardConfiguration.getCustomKeys() == null || keyboardConfiguration.getCustomKeys().isEmpty()) {
            modifiedKeyboardConfiguration.setCustomKeys(null);
        } else {
            // If there are more custom keys than are allowed for the selected keyboard layout, we need to trim the number of keys to only use the first n number of custom keys, where n is the number of allowed custom keys for that layout.
            int numConfigurableKeys = selectedLayoutCapability.getNumConfigurableKeys();
            if (keyboardConfiguration.getCustomKeys().size() > numConfigurableKeys) {
                modifiedKeyboardConfiguration.setCustomKeys(keyboardConfiguration.getCustomKeys().subList(0, numConfigurableKeys));
                DebugTool.logWarning(TAG, String.format(Locale.US, "%d custom keys set, but the selected layout: %s only supports %d. Dropping the rest.", keyboardConfiguration.getCustomKeys().size(), keyboardConfiguration.getKeyboardLayout(), numConfigurableKeys));
            }
        }

        // If the keyboard does not support masking input characters, we will remove it from the keyboard configuration
        if (!keyboardCapabilities.getMaskInputCharactersSupported()) {
            modifiedKeyboardConfiguration.setMaskInputCharacters(null);
        }

        return modifiedKeyboardConfiguration;
    }
    // GETTERS

    /**
     * @return A set of choice cells that have been preloaded to the head unit
     */
    public HashSet<ChoiceCell> getPreloadedChoices() {
        return this.preloadedChoices;
    }

    // CHOICE SET MANAGEMENT HELPERS

    HashSet<ChoiceCell> choicesToBeDeletedWithArray(List<ChoiceCell> choices) {
        HashSet<ChoiceCell> choicesSet = new HashSet<>(choices);
        choicesSet.retainAll(this.preloadedChoices);
        return choicesSet;
    }

    HashSet<ChoiceCell> choicesToBeRemovedFromPendingWithArray(List<ChoiceCell> choices) {
        HashSet<ChoiceCell> choicesSet = new HashSet<>(choices);
        choicesSet.retainAll(this.pendingPreloadChoices);
        return choicesSet;
    }

    /**
     * Checks if 2 or more cells have the same text/title. In case this condition is true, this function will handle the presented issue by adding "(count)".
     * E.g. Choices param contains 2 cells with text/title "Address" will be handled by updating the uniqueText/uniqueTitle of the second cell to "Address (2)".
     * @param choices The list of choiceCells to be uploaded.
     */
    void addUniqueNamesToCells(List<ChoiceCell> choices) {
        HashMap<String, Integer> dictCounter = new HashMap<>();

        for (ChoiceCell cell : choices) {
            String cellName = cell.getText();
            Integer counter = dictCounter.get(cellName);

            if (counter != null) {
                dictCounter.put(cellName, ++counter);
                cell.setUniqueText(cell.getText() + " (" + counter + ")");
            } else {
                dictCounter.put(cellName, 1);
            }
        }
    }

    void addUniqueNamesBasedOnStrippedCells(List<ChoiceCell> strippedCells, List<ChoiceCell> unstrippedCells) {
        if (strippedCells == null || unstrippedCells == null || strippedCells.size() != unstrippedCells.size()) {
          return;
        }
        // Tracks how many of each cell primary text there are so that we can append numbers to make each unique as necessary
        HashMap<ChoiceCell, Integer> dictCounter = new HashMap<>();
        for (int i = 0; i < strippedCells.size(); i++) {
            ChoiceCell cell = strippedCells.get(i);
            Integer counter = dictCounter.get(cell);
            if (counter != null) {
                counter++;
                dictCounter.put(cell, counter);
            } else {
                dictCounter.put(cell, 1);
            }

            counter = dictCounter.get(cell);

            if (counter > 1) {
                unstrippedCells.get(i).setUniqueText(unstrippedCells.get(i).getText() + " (" + counter + ")");
            }
        }
    }

    private List<ChoiceCell> cloneChoiceCellList(List<ChoiceCell> originalList) {
        if (originalList == null) {
            return null;
        }

        List<ChoiceCell> clone = new ArrayList<>();
        for (ChoiceCell choiceCell : originalList) {
            clone.add(choiceCell.clone());
        }
        return clone;
    }

    private LinkedHashSet<ChoiceCell> getChoicesToBeUploadedWithArray(List<ChoiceCell> choices) {
        // Clone choices
        List<ChoiceCell> choicesClone = cloneChoiceCellList(choices);
        if (choices != null && internalInterface.getSdlMsgVersion() != null
                && (internalInterface.getSdlMsgVersion().getMajorVersion() < 7
                || (internalInterface.getSdlMsgVersion().getMajorVersion() == 7 && internalInterface.getSdlMsgVersion().getMinorVersion() == 0))) {
            // If we're on < RPC 7.1, all primary texts need to be unique, so we don't need to check removed properties and duplicate cells
            addUniqueNamesToCells(choicesClone);
        } else {
            List<ChoiceCell> strippedCellsClone = removeUnusedProperties(choicesClone);
            addUniqueNamesBasedOnStrippedCells(strippedCellsClone, choicesClone);
        }
        LinkedHashSet<ChoiceCell> choiceCloneLinkedHash = new LinkedHashSet<>(choicesClone);
        choiceCloneLinkedHash.removeAll(preloadedChoices);
        return choiceCloneLinkedHash;
    }

    List<ChoiceCell> removeUnusedProperties(List<ChoiceCell> choiceCells) {
        List<ChoiceCell> strippedCellsClone = cloneChoiceCellList(choiceCells);
        //Clone Cells
        for (ChoiceCell cell : strippedCellsClone) {
            // Strip away fields that cannot be used to determine uniqueness visually including fields not supported by the HMI
            cell.setVoiceCommands(null);

            if (!hasImageFieldOfName(ImageFieldName.choiceImage)) {
                cell.setArtwork(null);
            }
            if (!hasTextFieldOfName(TextFieldName.secondaryText)) {
                cell.setSecondaryText(null);
            }
            if (!hasTextFieldOfName(TextFieldName.tertiaryText)) {
                cell.setTertiaryText(null);
            }
            if (!hasImageFieldOfName(ImageFieldName.choiceSecondaryImage)) {
                cell.setSecondaryArtwork(null);
            }
        }
        return strippedCellsClone;
    }

    void updateIdsOnChoices(LinkedHashSet<ChoiceCell> choices) {
        for (ChoiceCell cell : choices) {
            cell.setChoiceId(this.nextChoiceId);
            this.nextChoiceId++;
        }
    }

    private void findIdsOnChoiceSet(ChoiceSet choiceSet) {
        findIdsOnChoices(new HashSet<>(choiceSet.getChoices()));
    }

    private void findIdsOnChoices(HashSet<ChoiceCell> choices) {
        for (ChoiceCell cell : choices) {
            ChoiceCell uploadCell = null;
            if (pendingPreloadChoices.contains(cell)) {
                uploadCell = findIfPresent(cell, pendingPreloadChoices);
            } else if (preloadedChoices.contains(cell)) {
                uploadCell = findIfPresent(cell, preloadedChoices);
            }
            if (uploadCell != null) {
                cell.setChoiceId(uploadCell.getChoiceId());
            }
        }
    }

    ChoiceCell findIfPresent(ChoiceCell cell, HashSet<ChoiceCell> set) {
        if (set.contains(cell)) {
            for (ChoiceCell setCell : set) {
                if (setCell.equals(cell))
                    return setCell;
            }
        }
        return null;
    }

    // LISTENERS

    private void addListeners() {
        // DISPLAY/WINDOW CAPABILITIES - via SCM
        onDisplayCapabilityListener = new OnSystemCapabilityListener() {
            @Override
            public void onCapabilityRetrieved(Object capability) {
                // instead of using the parameter it's more safe to use the convenience method
                List<DisplayCapability> capabilities = SystemCapabilityManager.convertToList(capability, DisplayCapability.class);
                if (capabilities == null || capabilities.size() == 0) {
                    DebugTool.logError(TAG, "SoftButton Manager - Capabilities sent here are null or empty");
                } else {
                    DisplayCapability display = capabilities.get(0);
                    displayName = display.getDisplayName();
                    for (WindowCapability windowCapability : display.getWindowCapabilities()) {
                        int currentWindowID = windowCapability.getWindowID() != null ? windowCapability.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
                        if (currentWindowID == PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                            defaultMainWindowCapability = windowCapability;
                        }
                    }
                }
            }

            @Override
            public void onError(String info) {
                DebugTool.logError(TAG, "Unable to retrieve display capabilities. Many things will probably break. Info: " + info);
                defaultMainWindowCapability = null;
            }
        };
        if (internalInterface.getSystemCapabilityManager() != null) {
            this.internalInterface.getSystemCapabilityManager().addOnSystemCapabilityListener(SystemCapabilityType.DISPLAYS, onDisplayCapabilityListener);
        }

        // HMI UPDATES
        hmiListener = new OnRPCNotificationListener() {
            @Override
            public void onNotified(RPCNotification notification) {
                OnHMIStatus onHMIStatus = (OnHMIStatus) notification;
                if (onHMIStatus.getWindowID() != null && onHMIStatus.getWindowID() != PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                    return;
                }
                HMILevel oldHMILevel = currentHMILevel;
                currentHMILevel = onHMIStatus.getHmiLevel();

                if (currentHMILevel == HMILevel.HMI_NONE) {
                    transactionQueue.pause();
                }

                if (oldHMILevel == HMILevel.HMI_NONE && currentHMILevel != HMILevel.HMI_NONE) {
                    transactionQueue.resume();
                }

                currentSystemContext = onHMIStatus.getSystemContext();

                if (currentSystemContext == SystemContext.SYSCTXT_HMI_OBSCURED || currentSystemContext == SystemContext.SYSCTXT_ALERT) {
                    transactionQueue.pause();
                }

                if (currentSystemContext == SystemContext.SYSCTXT_MAIN && currentHMILevel != HMILevel.HMI_NONE) {
                    transactionQueue.resume();
                }

            }
        };
        internalInterface.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);
    }

    // ADDITIONAL HELPERS

    private boolean hasImageFieldOfName(ImageFieldName imageFieldName) {
        return defaultMainWindowCapability == null || ManagerUtility.WindowCapabilityUtility.hasImageFieldOfName(defaultMainWindowCapability, imageFieldName);
    }

    private boolean hasTextFieldOfName(TextFieldName textFieldName) {
        return defaultMainWindowCapability == null || ManagerUtility.WindowCapabilityUtility.hasTextFieldOfName(defaultMainWindowCapability, textFieldName);
    }

    boolean setUpChoiceSet(ChoiceSet choiceSet) {

        List<ChoiceCell> choices = choiceSet.getChoices();

        // Choices are not optional here
        if (choices == null || choices.size() == 0) {
            DebugTool.logError(TAG, "Cannot initiate a choice set with no choices");
            return false;
        }

        if (choiceSet.getTimeout() != null) {
            if (choiceSet.getTimeout() < 5 || choiceSet.getTimeout() > 100) {
                DebugTool.logWarning(TAG, "Attempted to create a choice set with a " + choiceSet.getTimeout() + " second timeout; Only 5 - 100 seconds is valid. When using the choice set manager, setTimeout() uses seconds.");
                return false;
            }
        }

        HashSet<ChoiceCell> uniqueChoiceCells = new HashSet<>();
        HashSet<String> uniqueVoiceCommands = new HashSet<>();
        int allVoiceCommandsCount = 0;
        int choiceCellWithVoiceCommandCount = 0;

        for (ChoiceCell cell : choices) {
            uniqueChoiceCells.add(cell);
            // Not using cloned cell here because we set the clone's VoiceCommands to null for visual check only
            if (cell.getVoiceCommands() != null) {
                uniqueVoiceCommands.addAll(cell.getVoiceCommands());
                choiceCellWithVoiceCommandCount += 1;
                allVoiceCommandsCount += cell.getVoiceCommands().size();
            }
        }

        if (uniqueChoiceCells.size() != choices.size()) {
            DebugTool.logError(TAG, "Attempted to create a choice set with a duplicate cell. Cell must have a unique value other than its primary text. The choice set will not be set.");
            return false;
        }

        // All or none of the choices MUST have VR Commands
        if (choiceCellWithVoiceCommandCount > 0 && choiceCellWithVoiceCommandCount < choices.size()) {
            DebugTool.logError(TAG, "If using voice recognition commands, all of the choice set cells must have unique VR commands. There are " + uniqueVoiceCommands.size() + " cells with unique voice commands and " + choices.size() + " total cells. The choice set will not be set.");
            return false;
        }

        // All VR Commands MUST be unique
        if (uniqueVoiceCommands.size() < allVoiceCommandsCount) {
            DebugTool.logError(TAG, "If using voice recognition commands, all VR commands must be unique. There are " + uniqueVoiceCommands.size() + " unique VR commands and " + allVoiceCommandsCount + " VR commands. The choice set will not be set.");
            return false;
        }
        return true;
    }

    KeyboardProperties defaultKeyboardConfiguration() {
        KeyboardProperties defaultProperties = new KeyboardProperties();
        defaultProperties.setLanguage(Language.EN_US);
        defaultProperties.setKeyboardLayout(KeyboardLayout.QWERTY);
        defaultProperties.setKeypressMode(KeypressMode.RESEND_CURRENT_ENTRY);
        return defaultProperties;
    }

    /**
     * Checks and increments the cancelID to keep in in a defined range
     *
     * @return an integer for cancelId to be sent to operations.
     */
    private int getNextCancelId() {
        if (nextCancelId >= choiceCellCancelIdMax) {
            nextCancelId = choiceCellCancelIdMin;
        } else {
            nextCancelId++;
        }
        return nextCancelId;
    }
}
