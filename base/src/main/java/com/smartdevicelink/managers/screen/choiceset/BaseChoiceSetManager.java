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

import android.support.annotation.NonNull;

import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.file.FileManager;
import com.smartdevicelink.managers.screen.choiceset.operations.CheckChoiceVROptionalInterface;
import com.smartdevicelink.managers.screen.choiceset.operations.CheckChoiceVROptionalOperation;
import com.smartdevicelink.managers.screen.choiceset.operations.DeleteChoicesOperation;
import com.smartdevicelink.managers.screen.choiceset.operations.PreloadChoicesOperation;
import com.smartdevicelink.managers.screen.choiceset.operations.PresentChoiceSetOperation;
import com.smartdevicelink.managers.screen.choiceset.operations.PresentKeyboardOperation;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.interfaces.ISdl;
import com.smartdevicelink.proxy.interfaces.OnSystemCapabilityListener;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.KeyboardProperties;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.KeyboardLayout;
import com.smartdevicelink.proxy.rpc.enums.KeypressMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.SystemContext;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.util.DebugTool;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * <strong>ChoiceSetManager</strong> <br>
 * <p>
 * Note: This class must be accessed through the SdlManager. Do not instantiate it by itself. <br>
 */
abstract class BaseChoiceSetManager extends BaseSubManager {

    // additional state
    private static final int CHECKING_VOICE = 0xA0;

    private OnRPCNotificationListener hmiListener;
    private OnSystemCapabilityListener displayListener;

    private final WeakReference<FileManager> fileManager;
    private KeyboardProperties keyboardConfiguration;
    private HMILevel currentHMILevel;
    private SystemContext currentSystemContext;
    private DisplayCapabilities displayCapabilities;

    private HashSet<ChoiceCell> preloadedChoices, pendingPreloadChoices;
    private ChoiceSet pendingPresentationSet;
    private Boolean isVROptional;
    // We will pass operations into this to be completed
    private PausableThreadPoolExecutor executor;
    private LinkedBlockingQueue<Runnable> operationQueue;
    private Future pendingPresentOperation;

    private int nextChoiceId;
    private int choiceCellIdMin = 1;

    BaseChoiceSetManager(@NonNull ISdl internalInterface, @NonNull FileManager fileManager) {
        super(internalInterface);

        // capabilities
        currentSystemContext = SystemContext.SYSCTXT_MAIN;
        currentHMILevel = HMILevel.HMI_NONE;
        addListeners();

        // setting/instantiating class vars
        this.fileManager = new WeakReference<>(fileManager);
        preloadedChoices = new HashSet<>();
        pendingPreloadChoices = new HashSet<>();
        nextChoiceId = choiceCellIdMin;
        isVROptional = false;
        keyboardConfiguration = defaultKeyboardConfiguration();
        operationQueue = new LinkedBlockingQueue<>();
        executor = new PausableThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 10, TimeUnit.SECONDS, operationQueue);
        executor.pause(); // pause until HMI ready
    }

    @Override
    public void start(CompletionListener listener){
        transitionToState(CHECKING_VOICE);
        checkVoiceOptional();
        super.start(listener);
    }

    @Override
    public void dispose(){

        // cancel the operations
        operationQueue.clear();
        executor.shutdownNow();

        currentHMILevel = null;
        currentSystemContext = null;
        displayCapabilities = null;

        pendingPresentationSet = null;
        pendingPresentOperation = null;
        isVROptional = true;
        nextChoiceId = choiceCellIdMin;

        // remove listeners
        internalInterface.removeOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);
        internalInterface.removeOnSystemCapabilityListener(SystemCapabilityType.DISPLAY, displayListener);

        super.dispose();
    }

    private void checkVoiceOptional(){

        CheckChoiceVROptionalOperation checkChoiceVR = new CheckChoiceVROptionalOperation(internalInterface, new CheckChoiceVROptionalInterface() {
            @Override
            public void onCheckChoiceVROperationComplete(boolean vrOptional) {
                isVROptional = vrOptional;
                transitionToState(READY);
                DebugTool.logInfo("VR is optional: "+ isVROptional);
            }

            @Override
            public void onError(String error) {
                // At this point, there were errors trying to send a test CICS
                // If we reach this state, we cannot use the manager
                DebugTool.logError(error);
                transitionToState(ERROR);
                // checking VR will always be first in the queue.
                // If pre-load operations were added while this was in progress
                // clear it from the queue onError.
                operationQueue.clear();
            }
        });
        executor.submit(checkChoiceVR);
    }

    public void preloadChoices(List<ChoiceCell> choices, final CompletionListener listener){

        final HashSet<ChoiceCell> choicesToUpload = choicesToBeUploadedWithArray(choices);
        choicesToUpload.removeAll(preloadedChoices);
        choicesToUpload.removeAll(pendingPreloadChoices);

        if (choicesToUpload.size() == 0){
            if (listener != null){
                listener.onComplete(true);
            }
            return;
        }

        updateIdsOnChoices(choicesToUpload);

        // Add the preload cells to the pending preload choices
        pendingPreloadChoices.addAll(choicesToUpload);

        if (fileManager.get() != null) {
            PreloadChoicesOperation preloadChoicesOperation = new PreloadChoicesOperation(internalInterface, fileManager.get(), displayCapabilities, isVROptional, choicesToUpload, new CompletionListener() {
                @Override
                public void onComplete(boolean success) {
                    if (success){
                        preloadedChoices.addAll(choicesToUpload);
                        pendingPreloadChoices.removeAll(choicesToUpload);
                        if (listener != null){
                            listener.onComplete(true);
                        }
                    }else {
                        DebugTool.logError("There was an error pre loading choice cells");
                        if (listener != null){
                            listener.onComplete(false);
                        }
                    }
                }
            });

            executor.submit(preloadChoicesOperation);
        } else {
            DebugTool.logError("File Manager was null in preload choice operation");
        }
    }

    public void deleteChoices(List<ChoiceCell> choices){

        if (!isReady()){ return; }

        // Find cells to be deleted that are already uploaded or are pending upload
        final HashSet<ChoiceCell> cellsToBeDeleted = choicesToBeDeletedWithArray(choices);
        HashSet<ChoiceCell> cellsToBeRemovedFromPending = choicesToBeRemovedFromPendingWithArray(choices);
        // If choices are deleted that are already uploaded or pending and are used by a pending presentation, cancel it and send an error
        HashSet<ChoiceCell> pendingPresentationChoices = new HashSet<>();
        if (pendingPresentationSet != null && pendingPresentationSet.getChoices() != null) {
            pendingPresentationChoices.addAll(pendingPresentationSet.getChoices());
        }

        if (pendingPresentOperation != null && !pendingPresentOperation.isCancelled() && !pendingPresentOperation.isDone() && (cellsToBeDeleted.retainAll(pendingPresentationChoices) || cellsToBeRemovedFromPending.retainAll(pendingPresentationChoices))){
            pendingPresentOperation.cancel(true);
            DebugTool.logWarning("Attempting to delete choice cells while there is a pending presentation operation. Pending presentation cancelled.");
            pendingPresentOperation = null;
        }

        // Remove cells from pending and delete choices
        pendingPresentationChoices.removeAll(cellsToBeRemovedFromPending);
        for (Runnable operation : operationQueue){
            if (!(operation instanceof PreloadChoicesOperation)){ continue; }
            ((PreloadChoicesOperation) operation).removeChoicesFromUpload(cellsToBeRemovedFromPending);
        }

        // Find Choices to delete
        if (cellsToBeDeleted.size() == 0){
            DebugTool.logInfo("Cells to be deleted size == 0");
            return;
        }
        findIdsOnChoices(cellsToBeDeleted);

        DeleteChoicesOperation deleteChoicesOperation = new DeleteChoicesOperation(internalInterface, cellsToBeDeleted, new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (!success){
                    DebugTool.logError("Failed to delete choices");
                    return;
                }
                preloadedChoices.removeAll(cellsToBeDeleted);
            }
        });
        executor.submit(deleteChoicesOperation);
    }

    public void presentChoiceSet(final ChoiceSet choiceSet, final InteractionMode mode, final KeyboardListener keyboardListener){

        if (!isReady()){ return; }

        if (choiceSet == null) {
            DebugTool.logWarning("Attempted to present a null choice set. Ignoring request");
            return;
        }
        // Perform additional checks against the ChoiceSet
        if (!setUpChoiceSet(choiceSet)){ return; }

        if (this.pendingPresentationSet != null && pendingPresentOperation != null){
            pendingPresentOperation.cancel(true);
            DebugTool.logWarning("Presenting a choice set while one is currently presented. Cancelling previous and continuing");
        }

        this.pendingPresentationSet = choiceSet;
        preloadChoices(this.pendingPresentationSet.getChoices(), new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (!success){
                    choiceSet.getChoiceSetSelectionListener().onError("There was an error pre-loading choice set choices");
                }
            }
        });

        findIdsOnChoiceSet(pendingPresentationSet);
        // Pass back the information to the developer
        ChoiceSetSelectionListener privateChoiceListener = new ChoiceSetSelectionListener() {
            @Override
            public void onChoiceSelected(ChoiceCell choiceCell, TriggerSource triggerSource, int rowIndex) {
                if (pendingPresentationSet.getChoiceSetSelectionListener() != null){
                    pendingPresentationSet.getChoiceSetSelectionListener().onChoiceSelected(choiceCell, triggerSource,rowIndex);
                }
                pendingPresentationSet = null;
                pendingPresentOperation = null;
            }

            @Override
            public void onError(String error) {
                if (pendingPresentationSet.getChoiceSetSelectionListener() != null){
                    pendingPresentationSet.getChoiceSetSelectionListener().onError(error);
                }
                pendingPresentationSet = null;
                pendingPresentOperation = null;
            }
        };

        PresentChoiceSetOperation presentOp;

        if (keyboardListener == null){
            // Non-searchable choice set
            DebugTool.logInfo("Creating non-searchable choice set");
            presentOp = new PresentChoiceSetOperation(internalInterface, pendingPresentationSet, mode, null, null, privateChoiceListener);
        } else {
            // Searchable choice set
            DebugTool.logInfo("Creating searchable choice set");
            presentOp = new PresentChoiceSetOperation(internalInterface, pendingPresentationSet, mode, keyboardConfiguration, keyboardListener, privateChoiceListener);
        }

        pendingPresentOperation = executor.submit(presentOp);
    }


    public void presentKeyboard(String initialText, KeyboardProperties customKeyboardConfig, KeyboardListener listener){

        if (!isReady()){ return; }

        if (pendingPresentationSet != null && pendingPresentOperation != null){
            pendingPresentOperation.cancel(true);
            pendingPresentationSet = null;
            DebugTool.logWarning("There is a current or pending choice set, cancelling and continuing.");
        }

        if (customKeyboardConfig == null){
            customKeyboardConfig = defaultKeyboardConfiguration();
        }

        // Present a keyboard with the choice set that we used to test VR's optional state
        DebugTool.logInfo("Presenting Keyboard - Choice Set Manager");
        PresentKeyboardOperation keyboardOp = new PresentKeyboardOperation(internalInterface, keyboardConfiguration, initialText, customKeyboardConfig, listener);
        pendingPresentOperation = executor.submit(keyboardOp);
    }

    public void setKeyboardConfiguration(KeyboardProperties keyboardConfiguration){
        if (keyboardConfiguration == null){
            this.keyboardConfiguration = defaultKeyboardConfiguration();
        } else{
            KeyboardProperties properties = new KeyboardProperties();
            properties.setLanguage((keyboardConfiguration.getLanguage() == null ? Language.EN_US : keyboardConfiguration.getLanguage()));
            properties.setKeyboardLayout((keyboardConfiguration.getKeyboardLayout() == null ? KeyboardLayout.QWERTZ : keyboardConfiguration.getKeyboardLayout()));
            properties.setKeypressMode(KeypressMode.RESEND_CURRENT_ENTRY);
            properties.setLimitedCharacterList(keyboardConfiguration.getLimitedCharacterList());
            properties.setAutoCompleteText(keyboardConfiguration.getAutoCompleteText());
            this.keyboardConfiguration = properties;
        }
    }

    // GETTERS

    public HashSet<ChoiceCell> getPreloadedChoices(){
        return this.preloadedChoices;
    }

    // CHOICE SET MANAGEMENT HELPERS

    private HashSet<ChoiceCell> choicesToBeUploadedWithArray(List<ChoiceCell> choices){
        HashSet<ChoiceCell> choicesSet = new HashSet<>(choices);
        choicesSet.removeAll(this.preloadedChoices);
        return choicesSet;
    }

    private HashSet<ChoiceCell> choicesToBeDeletedWithArray(List<ChoiceCell> choices){
        HashSet<ChoiceCell> choicesSet = new HashSet<>(choices);
        choicesSet.retainAll(this.preloadedChoices);
        return choicesSet;
    }

    private HashSet<ChoiceCell> choicesToBeRemovedFromPendingWithArray(List<ChoiceCell> choices){
        HashSet<ChoiceCell> choicesSet = new HashSet<>(choices);
        choicesSet.retainAll(this.pendingPreloadChoices);
        return choicesSet;
    }

    private void updateIdsOnChoices(HashSet<ChoiceCell> choices){
        for (ChoiceCell cell : choices){
            cell.setChoiceId(this.nextChoiceId);
            this.nextChoiceId++;
        }
    }

    private void findIdsOnChoiceSet(ChoiceSet choiceSet){
        findIdsOnChoices(new HashSet<>(choiceSet.getChoices()));
    }

    private void findIdsOnChoices(HashSet<ChoiceCell> choices){
        for (ChoiceCell cell : choices){
            ChoiceCell uploadCell = null;
            if (pendingPreloadChoices.contains(cell)){
                uploadCell = findIfPresent(cell, pendingPreloadChoices);
            }else if (preloadedChoices.contains(cell)){
                uploadCell = findIfPresent(cell, preloadedChoices);
            }
            if (uploadCell != null ){
                cell.setChoiceId(uploadCell.getChoiceId());
            }
        }
    }

    private ChoiceCell findIfPresent(ChoiceCell cell, HashSet<ChoiceCell> set){
        if (set.contains(cell)) {
            for (ChoiceCell setCell : set) {
                if (setCell.equals(cell))
                    return setCell;
            }
        }
        return null;
    }

    // LISTENERS

    private void addListeners(){
        // DISPLAY CAPABILITIES - via SCM
        displayListener = new OnSystemCapabilityListener() {
            @Override
            public void onCapabilityRetrieved(Object capability) {
                displayCapabilities = (DisplayCapabilities) capability;
            }

            @Override
            public void onError(String info) {
                DebugTool.logError("Unable to retrieve display capabilities. Many things will probably break. Info: "+ info);
            }
        };
        internalInterface.getCapability(SystemCapabilityType.DISPLAY, displayListener);

        // HMI UPDATES
        hmiListener = new OnRPCNotificationListener() {
            @Override
            public void onNotified(RPCNotification notification) {
                OnHMIStatus hmiStatus = (OnHMIStatus) notification;
                HMILevel oldHMILevel = currentHMILevel;
                currentHMILevel = hmiStatus.getHmiLevel();

                if (currentHMILevel.equals(HMILevel.HMI_NONE)){
                    executor.pause();
                }

                if (oldHMILevel.equals(HMILevel.HMI_NONE) && !currentHMILevel.equals(HMILevel.HMI_NONE)){
                    executor.resume();
                }

                currentSystemContext = hmiStatus.getSystemContext();

                if (currentSystemContext.equals(SystemContext.SYSCTXT_HMI_OBSCURED) || currentSystemContext.equals(SystemContext.SYSCTXT_ALERT)){
                    executor.pause();
                }

                if (currentSystemContext.equals(SystemContext.SYSCTXT_MAIN) && !currentHMILevel.equals(HMILevel.HMI_NONE)){
                    executor.resume();
                }

            }
        };
        internalInterface.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);
    }

    // ADDITIONAL HELPERS

    private boolean setUpChoiceSet(ChoiceSet choiceSet) {

        List<ChoiceCell> choices = choiceSet.getChoices();

        // Choices are not optional here
        if (choices == null) {
            DebugTool.logError("Cannot initiate a choice set with no choices");
            return false;
        }

        HashSet<String> choiceTextSet = new HashSet<>();
        HashSet<String> uniqueVoiceCommands = new HashSet<>();
        int allVoiceCommandsCount = 0;
        int choiceCellWithVoiceCommandCount = 0;

        for (ChoiceCell cell : choices) {

            choiceTextSet.add(cell.getText());

            if (cell.getVoiceCommands() != null) {
                uniqueVoiceCommands.addAll(cell.getVoiceCommands());
                choiceCellWithVoiceCommandCount += 1;
                allVoiceCommandsCount += cell.getVoiceCommands().size();
            }
        }

        // Cell text MUST be unique
        if (choiceTextSet.size() < choices.size()) {
            DebugTool.logError("Attempted to create a choice set with duplicate cell text. Cell text must be unique. The choice set will not be set.");
            return false;
        }

        // All or none of the choices MUST have VR Commands
        if (choiceCellWithVoiceCommandCount > 0 && choiceCellWithVoiceCommandCount < choices.size()) {
            DebugTool.logError("If using voice recognition commands, all of the choice set cells must have unique VR commands. There are " + uniqueVoiceCommands.size() + " cells with unique voice commands and " + choices.size() + " total cells. The choice set will not be set.");
            return false;
        }

        // All VR Commands MUST be unique
        if (uniqueVoiceCommands.size() < allVoiceCommandsCount) {
            DebugTool.logError("If using voice recognition commands, all VR commands must be unique. There are " + uniqueVoiceCommands.size() + " unique VR commands and " + allVoiceCommandsCount + " VR commands. The choice set will not be set.");
            return false;
        }
        return true;
    }


    private KeyboardProperties defaultKeyboardConfiguration(){
        KeyboardProperties defaultProperties = new KeyboardProperties();
        defaultProperties.setLanguage(Language.EN_US);
        defaultProperties.setKeyboardLayout(KeyboardLayout.QWERTY);
        defaultProperties.setKeypressMode(KeypressMode.RESEND_CURRENT_ENTRY);
        return defaultProperties;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isReady(){
        if (getState() != READY){
            DebugTool.logInfo("Choice Manager In Not-Ready State: "+ getState());
            return false;
        }
        return true;
    }
}