/*
 *  Copyright (c) 2019. Livio, Inc.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following
 *  disclaimer in the documentation and/or other materials provided with the
 *  distribution.
 *
 *  Neither the name of the Livio Inc. nor the names of its contributors
 *  may be used to endorse or promote products derived from this software
 *  without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package com.smartdevicelink;

import android.util.Log;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.managers.screen.choiceset.ChoiceCell;
import com.smartdevicelink.managers.screen.choiceset.ChoiceSet;
import com.smartdevicelink.managers.screen.choiceset.ChoiceSetSelectionListener;
import com.smartdevicelink.managers.screen.menu.MenuCell;
import com.smartdevicelink.managers.screen.menu.MenuSelectionListener;
import com.smartdevicelink.managers.screen.menu.VoiceCommand;
import com.smartdevicelink.managers.screen.menu.VoiceCommandSelectionListener;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.rpc.*;
import com.smartdevicelink.proxy.rpc.enums.*;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.util.DebugTool;

import java.util.*;

public class SdlService {


    private static final String TAG 					= "SDL Service";

    private static final String APP_NAME 				= "Hello Sdl";
    private static final String APP_NAME_ES             = "Hola Sdl";
    private static final String APP_NAME_FR             = "Bonjour Sdl";
    private static final String APP_ID 					= "8678309";

    private static final String ICON_FILENAME 			= "hello_sdl_icon.png";
    private static final String SDL_IMAGE_FILENAME  	= "sdl_full_image.png";

    private static final String WELCOME_SHOW 			= "Welcome to HelloSDL";
    private static final String WELCOME_SPEAK 			= "Welcome to Hello S D L";

    private static final String TEST_COMMAND_NAME 		= "Test Command";

    private static final String IMAGE_DIR =             "assets/images/";



    // variable to create and call functions of the SyncProxy
    private SdlManager sdlManager = null;
    private List<ChoiceCell> choiceCellList;

    private SdlServiceCallback callback;


    public SdlService(BaseTransportConfig config, SdlServiceCallback callback){
        this.callback = callback;
        buildSdlManager(config);
    }

    public void start() {
        DebugTool.logInfo("SdlService start() ");
        if(sdlManager != null){
            sdlManager.start();
        }
    }

    public void stop() {
        if (sdlManager != null) {
            sdlManager.dispose();
            sdlManager = null;
        }
    }

    private void buildSdlManager(BaseTransportConfig transport) {
        // This logic is to select the correct transport and security levels defined in the selected build flavor
        // Build flavors are selected by the "build variants" tab typically located in the bottom left of Android Studio
        // Typically in your app, you will only set one of these.
        if (sdlManager == null) {
            DebugTool.logInfo("Creating SDL Manager");

            //FIXME add the transport type
            // The app type to be used
            Vector<AppHMIType> appType = new Vector<>();
            appType.add(AppHMIType.MEDIA);

            // The manager listener helps you know when certain events that pertain to the SDL Manager happen
            // Here we will listen for ON_HMI_STATUS and ON_COMMAND notifications
            SdlManagerListener listener = new SdlManagerListener() {
                @Override
                public void onStart(SdlManager sdlManager) {
                    DebugTool.logInfo("SdlManager onStart");
                }

                @Override
                public void onDestroy(SdlManager sdlManager) {
                    DebugTool.logInfo("SdlManager onDestroy ");
                    SdlService.this.sdlManager = null;
                    if(SdlService.this.callback != null){
                        SdlService.this.callback.onEnd();
                    }
                }

                @Override
                public void onError(SdlManager sdlManager, String info, Exception e) {
                }

                @Override
                public LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language){
                    String appName;
                    switch (language) {
                        case ES_MX:
                            appName = APP_NAME_ES;
                            break;
                        case FR_CA:
                            appName = APP_NAME_FR;
                            break;
                        default:
                            return null;
                    }

                    return new LifecycleConfigurationUpdate(appName,null,TTSChunkFactory.createSimpleTTSChunks(appName), null);
                }

                @Override
                public LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language, Language language1) {
                    return null;
                }
            };


            HashMap<FunctionID,OnRPCNotificationListener> notificationListenerHashMap = new HashMap<FunctionID,OnRPCNotificationListener>();
            notificationListenerHashMap.put(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
                @Override
                public void onNotified(RPCNotification notification) {
                    OnHMIStatus status = (OnHMIStatus) notification;
                    if (status.getHmiLevel() == HMILevel.HMI_FULL && ((OnHMIStatus) notification).getFirstRun()) {
                        setVoiceCommands();
                        sendMenus();
                        performWelcomeSpeak();
                        performWelcomeShow();
                        preloadChoices();
                    }
                }
            });

            // Create App Icon, this is set in the SdlManager builder
            SdlArtwork appIcon = new SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, IMAGE_DIR+"sdl_s_green.png", true);

            // The manager builder sets options for your session
            SdlManager.Builder builder = new SdlManager.Builder(APP_ID, APP_NAME, listener);
            builder.setAppTypes(appType);
            builder.setTransportType(transport);
            builder.setAppIcon(appIcon);
            builder.setRPCNotificationListeners(notificationListenerHashMap);
            sdlManager = builder.build();
        }
    }

    /**
     * Send some voice commands
     */
    private void setVoiceCommands(){

        List<String> list1 = Collections.singletonList("Command One");
        List<String> list2 = Collections.singletonList("Command two");

        VoiceCommand voiceCommand1 = new VoiceCommand(list1, new VoiceCommandSelectionListener() {
            @Override
            public void onVoiceCommandSelected() {
                Log.i(TAG, "Voice Command 1 triggered");
            }
        });

        VoiceCommand voiceCommand2 = new VoiceCommand(list2, new VoiceCommandSelectionListener() {
            @Override
            public void onVoiceCommandSelected() {
                Log.i(TAG, "Voice Command 2 triggered");
            }
        });

        sdlManager.getScreenManager().setVoiceCommands(Arrays.asList(voiceCommand1,voiceCommand2));
    }

    /**
     *  Add menus for the app on SDL.
     */
    private void sendMenus(){

        // some arts
        SdlArtwork livio = new SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, IMAGE_DIR+"sdl_s_green.png", true);

        // some voice commands
        List<String> voice2 = Collections.singletonList("Cell two");

        MenuCell mainCell1 = new MenuCell("Test Cell 1 (speak)", livio, null, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i(TAG, "Test cell 1 triggered. Source: "+ trigger.toString());
                showTest();
            }
        });

        MenuCell mainCell2 = new MenuCell("Test Cell 2", null, voice2, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i(TAG, "Test cell 2 triggered. Source: "+ trigger.toString());
            }
        });

        // SUB MENU

        MenuCell subCell1 = new MenuCell("SubCell 1",null, null, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i(TAG, "Sub cell 1 triggered. Source: "+ trigger.toString());
            }
        });

        MenuCell subCell2 = new MenuCell("SubCell 2",null, null, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i(TAG, "Sub cell 2 triggered. Source: "+ trigger.toString());
            }
        });

        // sub menu parent cell
        MenuCell mainCell3 = new MenuCell("Test Cell 3 (sub menu)", null, Arrays.asList(subCell1,subCell2));

        MenuCell mainCell4 = new MenuCell("Show Perform Interaction", null, null, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                showPerformInteraction();
            }
        });

        MenuCell mainCell5 = new MenuCell("Clear the menu",null, null, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i(TAG, "Clearing Menu. Source: "+ trigger.toString());
                // Clear this thing
                sdlManager.getScreenManager().setMenu(Collections.<MenuCell>emptyList());
                showAlert("Menu Cleared");
            }
        });

        // Send the entire menu off to be created
        sdlManager.getScreenManager().setMenu(Arrays.asList(mainCell1, mainCell2, mainCell3, mainCell4, mainCell5));
    }

    /**
     * Will speak a sample welcome message
     */
    private void performWelcomeSpeak(){
        sdlManager.sendRPC(new Speak(TTSChunkFactory.createSimpleTTSChunks(WELCOME_SPEAK)));
    }

    /**
     * Use the Screen Manager to set the initial screen text and set the image.
     * Because we are setting multiple items, we will call beginTransaction() first,
     * and finish with commit() when we are done.
     */
    private void performWelcomeShow() {
        sdlManager.getScreenManager().beginTransaction();
        sdlManager.getScreenManager().setTextField1(APP_NAME);
        sdlManager.getScreenManager().setTextField2(WELCOME_SHOW);
        sdlManager.getScreenManager().setPrimaryGraphic(new SdlArtwork(SDL_IMAGE_FILENAME, FileType.GRAPHIC_PNG, IMAGE_DIR+"sdl.png", true));
        sdlManager.getScreenManager().commit(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                if (success){
                    Log.i(TAG, "welcome show successful");
                }
            }
        });
    }

    /**
     * Will show a sample test message on screen as well as speak a sample test message
     */
    private void showTest(){
        sdlManager.getScreenManager().beginTransaction();
        sdlManager.getScreenManager().setTextField1("Test Cell 1 has been selected");
        sdlManager.getScreenManager().setTextField2("");
        sdlManager.getScreenManager().commit(null);

        sdlManager.sendRPC(new Speak(TTSChunkFactory.createSimpleTTSChunks(TEST_COMMAND_NAME)));
    }

    private void showAlert(String text){
        Alert alert = new Alert();
        alert.setAlertText1(text);
        alert.setDuration(5000);
        sdlManager.sendRPC(alert);
    }

    public interface SdlServiceCallback{
        void onEnd();
    }

    // Choice Set

    private void preloadChoices(){
        ChoiceCell cell1 = new ChoiceCell("Item 1");
        ChoiceCell cell2 = new ChoiceCell("Item 2");
        ChoiceCell cell3 = new ChoiceCell("Item 3");
        choiceCellList = new ArrayList<>(Arrays.asList(cell1,cell2,cell3));
        sdlManager.getScreenManager().preloadChoices(choiceCellList, null);
    }

    private void showPerformInteraction(){
        if (choiceCellList != null) {
            ChoiceSet choiceSet = new ChoiceSet("Choose an Item from the list", choiceCellList, new ChoiceSetSelectionListener() {
                @Override
                public void onChoiceSelected(ChoiceCell choiceCell, TriggerSource triggerSource, int rowIndex) {
                    showAlert(choiceCell.getText() + " was selected");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "There was an error showing the perform interaction: "+ error);
                }
            });
            sdlManager.getScreenManager().presentChoiceSet(choiceSet, InteractionMode.MANUAL_ONLY);
        }
    }
}