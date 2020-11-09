package com.smartdevicelink.managers.screen.alert;

import android.util.Log;

import androidx.annotation.NonNull;

import com.livio.taskmaster.Queue;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.ISdl;
import com.smartdevicelink.managers.lifecycle.OnSystemCapabilityListener;
import com.smartdevicelink.managers.lifecycle.SystemCapabilityManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.DisplayCapability;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.WindowCapability;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.PredefinedWindows;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.SystemContext;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.util.DebugTool;

import java.util.List;


public class BaseAlertManager extends BaseSubManager {

    private static final String TAG = "BaseAlertManager";
    Queue transactionQueue;
    OnRPCNotificationListener hmiListener;
    WindowCapability defaultMainWindowCapability;
    HMILevel currentHMILevel;
    private OnSystemCapabilityListener onDisplaysCapabilityListener;

    public BaseAlertManager(@NonNull ISdl internalInterface) {
        super(internalInterface);
        addListeners();
        this.transactionQueue = newTransactionQueue();
    }
    private Queue newTransactionQueue() {
        Queue queue = internalInterface.getTaskmaster().createQueue("AlertManager", 4, false);
        queue.pause();
        return queue;
    }

    public void presentAlert(AlertView alert, CompletionListener listener) {
        if (getState() == ERROR) {
            DebugTool.logWarning(TAG, "Alert Manager In Error State");
            return;
        }
        Log.i("Julian", "presentAlert: herere 1");
        PresentAlertOperation operation = new PresentAlertOperation(internalInterface, alert, listener);
        transactionQueue.add(operation, false);

    }

    // Suspend the queue if the WindowCapabilities are null
    // OR if the HMI level is NONE since we want to delay sending RPCs until we're in non-NONE
    private void updateTransactionQueueSuspended() {
        if (HMILevel.HMI_NONE.equals(currentHMILevel)) {
            DebugTool.logInfo(TAG, String.format("Suspending the transaction queue. Current HMI level is NONE: %b, window capabilities are null: %b", HMILevel.HMI_NONE.equals(currentHMILevel), defaultMainWindowCapability == null));
            transactionQueue.pause();
        } else {
            DebugTool.logInfo(TAG, "Starting the transaction queue");
            transactionQueue.resume();
        }
    }
    @Override
    public void start(CompletionListener listener) {
        transitionToState(READY);
        super.start(listener);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void addListeners() {
        hmiListener = new OnRPCNotificationListener() {
            @Override
            public void onNotified(RPCNotification notification) {
                OnHMIStatus onHMIStatus = (OnHMIStatus) notification;
/*                if (onHMIStatus.getWindowID() != null && onHMIStatus.getWindowID() != PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                    return;
                }*/
                currentHMILevel = onHMIStatus.getHmiLevel();
                updateTransactionQueueSuspended();
            }
        };
        internalInterface.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, hmiListener);



/*        onDisplaysCapabilityListener = new OnSystemCapabilityListener() {
            @Override
            public void onCapabilityRetrieved(Object capability) {
                // instead of using the parameter it's more safe to use the convenience method
                List<DisplayCapability> capabilities = SystemCapabilityManager.convertToList(capability, DisplayCapability.class);
                if (capabilities == null || capabilities.size() == 0) {
                    DebugTool.logError(TAG, "TextAndGraphic Manager - Capabilities sent here are null or empty");
                    defaultMainWindowCapability = null;
                } else {
                    DisplayCapability display = capabilities.get(0);
                    for (WindowCapability windowCapability : display.getWindowCapabilities()) {
                        int currentWindowID = windowCapability.getWindowID() != null ? windowCapability.getWindowID() : PredefinedWindows.DEFAULT_WINDOW.getValue();
                        if (currentWindowID == PredefinedWindows.DEFAULT_WINDOW.getValue()) {
                            // Check if the window capability is equal to the one we already have. If it is, abort.
                            if (defaultMainWindowCapability != null && defaultMainWindowCapability.getStore().equals(windowCapability.getStore())) {
                                return;
                            }
                            defaultMainWindowCapability = windowCapability;
                        }
                    }
                }
                // Update the queue's suspend state
                updateTransactionQueueSuspended();
            }
        };*/


    }
}