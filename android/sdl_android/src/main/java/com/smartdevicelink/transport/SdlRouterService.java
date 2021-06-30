/*
 * Copyright (c) 2018 Livio, Inc.
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

package com.smartdevicelink.transport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.smartdevicelink.R;
import com.smartdevicelink.marshal.JsonRPCMarshaller;
import com.smartdevicelink.protocol.BinaryFrameHeader;
import com.smartdevicelink.protocol.ProtocolMessage;
import com.smartdevicelink.protocol.SdlPacket;
import com.smartdevicelink.protocol.SdlPacketFactory;
import com.smartdevicelink.protocol.enums.ControlFrameTags;
import com.smartdevicelink.protocol.enums.FrameType;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.protocol.enums.MessageType;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.rpc.UnregisterAppInterface;
import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.ByteAraryMessageAssembler;
import com.smartdevicelink.transport.utl.ByteArrayMessageSpliter;
import com.smartdevicelink.transport.utl.TransportRecord;
import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.BitConverter;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.IntegrationValidator;
import com.smartdevicelink.util.SdlAppInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.smartdevicelink.transport.TransportConstants.CONNECTED_DEVICE_STRING_EXTRA_NAME;
import static com.smartdevicelink.transport.TransportConstants.FOREGROUND_EXTRA;
import static com.smartdevicelink.transport.TransportConstants.FORMED_PACKET_EXTRA_NAME;
import static com.smartdevicelink.transport.TransportConstants.HARDWARE_DISCONNECTED;
import static com.smartdevicelink.transport.TransportConstants.SDL_NOTIFICATION_CHANNEL_ID;
import static com.smartdevicelink.transport.TransportConstants.SDL_NOTIFICATION_CHANNEL_NAME;
import static com.smartdevicelink.transport.TransportConstants.SEND_PACKET_TO_APP_LOCATION_EXTRA_NAME;
import static com.smartdevicelink.transport.TransportConstants.TRANSPORT_DISCONNECTED;

/**
 * <b>This class should not be modified by anyone outside of the approved contributors of the SmartDeviceLink project.</b>
 * This service is a central point of communication between hardware and the registered clients. It will multiplex a single transport
 * to provide a connection for a theoretical infinite amount of SDL sessions.
 *
 * @author Joey Grover
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "Convert2Diamond", "deprecation"})
public class SdlRouterService extends Service {

    private static final String TAG = "Sdl Router Service";
    /**
     * <b> NOTE: DO NOT MODIFY THIS UNLESS YOU KNOW WHAT YOU'RE DOING.</b>
     */
    protected static final int ROUTER_SERVICE_VERSION_NUMBER = 15;

    private static final String ROUTER_SERVICE_PROCESS = "com.smartdevicelink.router";

    private static final int FOREGROUND_SERVICE_ID = 849;

    private static final long CLIENT_PING_DELAY = 1000;

    public static final String REGISTER_NEWER_SERVER_INSTANCE_ACTION = "com.sdl.android.newservice";

    public static final String SDL_NOTIFICATION_FAQS_PAGE = "https://smartdevicelink.com/en/guides/android/frequently-asked-questions/sdl-notifications/";

    public static final String REGISTER_WITH_ROUTER_ACTION = "com.sdl.android.register";

    /**
     * Message types sent from the BluetoothReadService Handler
     */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_LOG = 5;

    @SuppressWarnings("FieldCanBeLocal")
    private final int UNREGISTER_APP_INTERFACE_CORRELATION_ID = 65530;

    /* Bluetooth Transport */
    private MultiplexBluetoothTransport bluetoothTransport = null;
    private final Handler bluetoothHandler = new TransportHandler(this);

    /* USB Transport */
    private MultiplexUsbTransport usbTransport;
    private final Handler usbHandler = new TransportHandler(this);

    /* TCP Transport */
    private MultiplexTcpTransport tcpTransport;
    private final Handler tcpHandler = new TransportHandler(this);

    /**
     * Preference location where the service stores known SDL status based on device address
     */
    protected static final String SDL_DEVICE_STATUS_SHARED_PREFS = "sdl.device.status";
    /**
     * Preference location where generic key/values can be stored
     */
    protected static final String SDL_ROUTER_SERVICE_PREFS = "sdl.router.service.prefs";
    protected static final String KEY_AVOID_NOTIFICATION_CHANNEL_DELETE = "avoidNotificationChannelDelete";


    private static boolean connectAsClient = false;
    private static boolean closing = false;
    private static Thread.UncaughtExceptionHandler routerServiceExceptionHandler = null;

    private Handler altTransportTimerHandler, foregroundTimeoutHandler;
    private Runnable altTransportTimerRunnable, foregroundTimeoutRunnable;
    private final static int ALT_TRANSPORT_TIMEOUT_RUNNABLE = 30000, FOREGROUND_TIMEOUT = 10000;

    private boolean wrongProcess = false;
    private boolean initPassed = false;
    private boolean hasCalledStartForeground = false;
    boolean firstStart = true;

    public static HashMap<String, RegisteredApp> registeredApps;
    private SparseArray<String> bluetoothSessionMap, usbSessionMap, tcpSessionMap;
    private SparseIntArray sessionHashIdMap;
    private SparseIntArray cleanedSessionMap;
    private final Object SESSION_LOCK = new Object(), REGISTERED_APPS_LOCK = new Object(),
            PING_COUNT_LOCK = new Object(), FOREGROUND_NOTIFICATION_LOCK = new Object();

    private static Messenger altTransportService = null;

    private boolean startSequenceComplete = false;
    private VehicleType receivedVehicleType;

    private ExecutorService packetExecutor = null;
    ConcurrentHashMap<TransportType, PacketWriteTaskMaster> packetWriteTaskMasterMap = null;


    /**
     * This flag is to keep track of if we are currently acting as a foreground service
     */
    private boolean isForeground = false;

    private int cachedModuleVersion = -1;

    /**
     * Executor for making sure clients are still running during trying times
     */
    private ScheduledExecutorService clientPingExecutor = null;
    Intent pingIntent = null;
    private boolean isPingingClients = false;
    int pingCount = 0;


    /* **************************************************************************************************************************************
     ****************************************************************************************************************************************
     ***********************************************  Broadcast Receivers START  **************************************************************
     ****************************************************************************************************************************************
     ****************************************************************************************************************************************/

    /**
     * create our receiver from the router service
     */
    final BroadcastReceiver mainServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Let's grab where to reply to this intent at. We will keep it temp right now because we may have to deny registration
            String action = intent.getStringExtra(SEND_PACKET_TO_APP_LOCATION_EXTRA_NAME);
            sendBroadcast(prepareRegistrationIntent(action));
        }
    };

    private Intent prepareRegistrationIntent(String action) {
        Intent registrationIntent = new Intent();
        registrationIntent.setAction(action);
        registrationIntent.putExtra(TransportConstants.BIND_LOCATION_PACKAGE_NAME_EXTRA, this.getPackageName());
        registrationIntent.putExtra(TransportConstants.BIND_LOCATION_CLASS_NAME_EXTRA, this.getClass().getName());
        registrationIntent.setFlags((Intent.FLAG_RECEIVER_FOREGROUND));
        return registrationIntent;
    }

    private void onAppRegistered(RegisteredApp app) {
        //Log.enableDebug(receivedIntent.getBooleanExtra(LOG_BASIC_DEBUG_BOOLEAN_EXTRA, false));
        //Log.enableBluetoothTraceLogging(receivedIntent.getBooleanExtra(LOG_TRACE_BT_DEBUG_BOOLEAN_EXTRA, false));
        //Ok this is where we should do some authenticating...maybe.
        //Should we ask for all relevant data in this packet?
        if (bluetoothAvailable()) {
            if (startSequenceComplete &&
                    !connectAsClient && (bluetoothTransport == null
                    || bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_NONE)) {
                DebugTool.logError(TAG, "Serial service not initialized while registering app");
                //Maybe we should try to do a connect here instead
                DebugTool.logInfo(TAG, "Serial service being restarted");
                initBluetoothSerialService();


            }
        }

        DebugTool.logInfo(TAG, app.appId + " has just been registered with SDL Router Service");
    }

    /**
     * If the user disconnects the bluetooth device we will want to stop SDL and our current
     * connection through RFCOMM
     */
    final BroadcastReceiver mListenForDisconnect = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                DebugTool.logInfo(TAG, "Disconnect received with no action.");
            } else {
                DebugTool.logInfo(TAG, "Disconnect received. Action: " + intent.getAction());

                if (action.equalsIgnoreCase(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (bluetoothState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                        case BluetoothAdapter.STATE_ON:
                            //There is nothing to do in the case the adapter is turning on or just switched to on
                            return;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            DebugTool.logInfo(TAG, "Bluetooth is shutting off, SDL Router Service is closing.");
                            connectAsClient = false;
                            if (!shouldServiceRemainOpen(intent)) {
                                closeSelf();
                            }
                            return;
                        default:
                            break;
                    }
                }
                //Otherwise
                connectAsClient = false;
                if (legacyModeEnabled) {
                    DebugTool.logInfo(TAG, "Legacy mode enabled and bluetooth d/c'ed, restarting router service bluetooth.");
                    enableLegacyMode(false);
                    onTransportDisconnected(new TransportRecord(TransportType.BLUETOOTH, null));
                    initBluetoothSerialService();
                }
            }
        }
    };

    /* **************************************************************************************************************************************
     ***********************************************  Broadcast Receivers End  **************************************************************
     ****************************************************************************************************************************************/

    /* **************************************************************************************************************************************
     *********************************************** Handlers for bound clients **************************************************************
     ****************************************************************************************************************************************/


    /**
     * Target we publish for clients to send messages to RouterHandler.
     */
    final Messenger routerMessenger = new Messenger(new RouterHandler(this));

    /**
     * Handler of incoming messages from clients.
     */
    @SuppressWarnings("Convert2Diamond")
    static class RouterHandler extends Handler {
        final WeakReference<SdlRouterService> provider;

        public RouterHandler(SdlRouterService provider) {
            this.provider = new WeakReference<SdlRouterService>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            final Bundle receivedBundle = msg.getData();
            Bundle returnBundle;
            final SdlRouterService service = this.provider.get();

            switch (msg.what) {
                case TransportConstants.ROUTER_REQUEST_BT_CLIENT_CONNECT:
                    if (receivedBundle.getBoolean(TransportConstants.CONNECT_AS_CLIENT_BOOLEAN_EXTRA, false)
                            && !connectAsClient) {        //We check this flag to make sure we don't try to connect over and over again. On D/C we should set to false
                        //Log.d(TAG,"Attempting to connect as bt client");
                        BluetoothDevice device = receivedBundle.getParcelable(BluetoothDevice.EXTRA_DEVICE);
                        connectAsClient = true;
                        if (device == null || !service.bluetoothConnect(device)) {
                            DebugTool.logError(TAG, "Unable to connect to bluetooth device");
                            connectAsClient = false;
                        }
                    }
                    //**************** We don't break here so we can let the app register as well
                case TransportConstants.ROUTER_REGISTER_CLIENT: //msg.arg1 is appId
                    //pingClients();
                    Message message = Message.obtain();
                    message.what = TransportConstants.ROUTER_REGISTER_CLIENT_RESPONSE;
                    message.arg1 = TransportConstants.REGISTRATION_RESPONSE_SUCESS;
                    String appId = receivedBundle.getString(TransportConstants.APP_ID_EXTRA_STRING);
                    if (appId == null) {
                        appId = "" + receivedBundle.getLong(TransportConstants.APP_ID_EXTRA, -1);
                    }
                    if (appId.length() <= 0 || msg.replyTo == null) {
                        DebugTool.logWarning(TAG, "Unable to register app as no id or messenger was included");
                        if (msg.replyTo != null) {
                            message.arg1 = TransportConstants.REGISTRATION_RESPONSE_DENIED_APP_ID_NOT_INCLUDED;
                            try {
                                msg.replyTo.send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    if (service.legacyModeEnabled) {
                        DebugTool.logWarning(TAG, "Unable to register app as legacy mode is enabled");
                        if (msg.replyTo != null) {
                            message.arg1 = TransportConstants.REGISTRATION_RESPONSE_DENIED_LEGACY_MODE_ENABLED;
                            try {
                                msg.replyTo.send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }

                    int routerMessagingVersion = receivedBundle.getInt(TransportConstants.ROUTER_MESSAGING_VERSION, 1);

                    RegisteredApp app = service.new RegisteredApp(appId, routerMessagingVersion, msg.replyTo);

                    synchronized (service.REGISTERED_APPS_LOCK) {
                        RegisteredApp old = registeredApps.put(app.getAppId(), app);
                        if (old != null) {
                            DebugTool.logWarning(TAG, "Replacing already existing app with this app id");
                            service.removeAllSessionsForApp(old, true);
                            old.close();
                        }
                    }
                    service.onAppRegistered(app);

                    returnBundle = new Bundle();
                    //Add params if connected
                    if (service.isPrimaryTransportConnected()) {
                        ArrayList<TransportRecord> records = service.getConnectedTransports();
                        returnBundle.putString(TransportConstants.HARDWARE_CONNECTED, records.get(records.size() - 1).getType().name());
                        if (app.routerMessagingVersion > 1) {
                            returnBundle.putParcelableArrayList(TransportConstants.CURRENT_HARDWARE_CONNECTED, records);
                        }

                        if (service.bluetoothTransport != null) {
                            returnBundle.putString(CONNECTED_DEVICE_STRING_EXTRA_NAME, service.bluetoothTransport.getDeviceName());
                        }
                    }
                    //Add the version of this router service
                    returnBundle.putInt(TransportConstants.ROUTER_SERVICE_VERSION, SdlRouterService.ROUTER_SERVICE_VERSION_NUMBER);

                    message.setData(returnBundle);

                    int result = app.sendMessage(message);
                    if (result == RegisteredApp.SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT) {
                        synchronized (service.REGISTERED_APPS_LOCK) {
                            registeredApps.remove(appId);
                        }
                    }
                    break;
                case TransportConstants.ROUTER_UNREGISTER_CLIENT:
                    String appIdToUnregister = receivedBundle.getString(TransportConstants.APP_ID_EXTRA_STRING);
                    if (appIdToUnregister == null) {
                        appIdToUnregister = "" + receivedBundle.getLong(TransportConstants.APP_ID_EXTRA, -1);
                    }
                    DebugTool.logInfo(TAG, "Unregistering client: " + appIdToUnregister);
                    RegisteredApp unregisteredApp;
                    synchronized (service.REGISTERED_APPS_LOCK) {
                        unregisteredApp = registeredApps.remove(appIdToUnregister);
                    }
                    Message response = Message.obtain();
                    response.what = TransportConstants.ROUTER_UNREGISTER_CLIENT_RESPONSE;
                    if (unregisteredApp == null) {
                        response.arg1 = TransportConstants.UNREGISTRATION_RESPONSE_FAILED_APP_ID_NOT_FOUND;
                        service.removeAllSessionsWithAppId(appIdToUnregister);
                    } else {
                        response.arg1 = TransportConstants.UNREGISTRATION_RESPONSE_SUCESS;
                        service.removeAllSessionsForApp(unregisteredApp, false);
                    }
                    DebugTool.logInfo(TAG, "Unregistering client response: " + response.arg1);
                    try {
                        msg.replyTo.send(response); //We do this because we aren't guaranteed to find the correct registeredApp to send the message through
                    } catch (RemoteException e) {
                        e.printStackTrace();

                    } catch (NullPointerException e2) {
                        DebugTool.logError(TAG, "No reply address included, can't send a reply");
                    }

                    break;
                case TransportConstants.ROUTER_SEND_PACKET:
                    //Log.d(TAG, "Received packet to send");
                    if (receivedBundle != null) {
                        Runnable packetRun = new Runnable() {
                            @Override
                            public void run() {
                                String buffAppId = receivedBundle.getString(TransportConstants.APP_ID_EXTRA_STRING);
                                if (buffAppId == null) {
                                    buffAppId = "" + receivedBundle.getLong(TransportConstants.APP_ID_EXTRA, -1);
                                }

                                RegisteredApp buffApp;
                                synchronized (service.REGISTERED_APPS_LOCK) {
                                    buffApp = registeredApps.get(buffAppId);
                                }

                                if (buffApp != null) {
                                    buffApp.handleIncommingClientMessage(receivedBundle);
                                } else {
                                    TransportType transportType = TransportType.valueForString(receivedBundle.getString(TransportConstants.TRANSPORT_TYPE));
                                    if (transportType == null) {

                                        /* We check bluetooth first because we assume if this value
                                         * isn't included it is an older version of the proxy and
                                         * therefore will be expecting this to be bluetooth.
                                         */
                                        if (service.bluetoothTransport != null && service.bluetoothTransport.isConnected()) {
                                            transportType = TransportType.BLUETOOTH;
                                        } else if (service.usbTransport != null && service.usbTransport.isConnected()) {
                                            transportType = TransportType.USB;
                                        } else if (service.tcpTransport != null && service.tcpTransport.isConnected()) {
                                            transportType = TransportType.TCP;
                                        } else {
                                            // This means no transport is connected. Likely the
                                            // router service has already disconnected and this
                                            // is now just executing.
                                            DebugTool.logError(TAG, "Can't send packet, no transport specified and none are connected.");
                                            return;
                                        }
                                        //Log.d(TAG, "Transport type was null, so router set it to " + transportType.name());
                                        if (transportType != null) {
                                            receivedBundle.putString(TransportConstants.TRANSPORT_TYPE, transportType.name());
                                        }
                                    }
                                    service.writeBytesToTransport(receivedBundle);
                                }
                            }
                        };
                        if (service.packetExecutor != null) {
                            service.packetExecutor.execute(packetRun);
                        }
                    }
                    break;
                case TransportConstants.ROUTER_REQUEST_NEW_SESSION:
                    String appIdRequesting = receivedBundle.getString(TransportConstants.APP_ID_EXTRA_STRING);
                    if (appIdRequesting == null) {
                        appIdRequesting = "" + receivedBundle.getLong(TransportConstants.APP_ID_EXTRA, -1);
                    }
                    Message extraSessionResponse = Message.obtain();
                    extraSessionResponse.what = TransportConstants.ROUTER_REQUEST_NEW_SESSION_RESPONSE;
                    if (appIdRequesting.length() > 0) {
                        synchronized (service.REGISTERED_APPS_LOCK) {
                            if (registeredApps != null) {
                                RegisteredApp appRequesting = registeredApps.get(appIdRequesting);
                                if (appRequesting != null) {
                                    //Retrieve the transport the app is requesting a new session
                                    String transport = receivedBundle.getString(TransportConstants.TRANSPORT_TYPE);
                                    TransportType requestingTransport = null;
                                    if (transport != null) {
                                        try {
                                            requestingTransport = TransportType.valueOf(transport);
                                        } catch (IllegalArgumentException e) {
                                        }
                                    }
                                    if (requestingTransport == null) {
                                        /* We check bluetooth first because we assume if this value
                                         * isn't included it is an older version of the proxy and
                                         * therefore will be expecting this to be bluetooth.
                                         */
                                        if (service.bluetoothTransport != null && service.bluetoothTransport.isConnected()) {
                                            requestingTransport = TransportType.BLUETOOTH;
                                        } else if (service.usbTransport != null && service.usbTransport.isConnected()) {
                                            requestingTransport = TransportType.USB;
                                        } else if (service.tcpTransport != null && service.tcpTransport.isConnected()) {
                                            requestingTransport = TransportType.TCP;
                                        }
                                    }
                                    appRequesting.getSessionIds().add((long) -1); //Adding an extra session
                                    appRequesting.getAwaitingSession().add(requestingTransport);
                                    extraSessionResponse.arg1 = TransportConstants.ROUTER_REQUEST_NEW_SESSION_RESPONSE_SUCESS;
                                } else {
                                    extraSessionResponse.arg1 = TransportConstants.ROUTER_REQUEST_NEW_SESSION_RESPONSE_FAILED_APP_NOT_FOUND;
                                }
                            }
                        }
                    } else {
                        extraSessionResponse.arg1 = TransportConstants.ROUTER_REQUEST_NEW_SESSION_RESPONSE_FAILED_APP_ID_NOT_INCL;
                    }
                    try {
                        msg.replyTo.send(extraSessionResponse); //We do this because we aren't guaranteed to find the correct registeredApp to send the message through
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e2) {
                        DebugTool.logError(TAG, "No reply address included, can't send a reply");
                    }
                    break;
                case TransportConstants.ROUTER_REMOVE_SESSION:
                    String appIdWithSession = receivedBundle.getString(TransportConstants.APP_ID_EXTRA_STRING);
                    if (appIdWithSession == null) {
                        appIdWithSession = "" + receivedBundle.getLong(TransportConstants.APP_ID_EXTRA, -1);
                    }
                    long sessionId = receivedBundle.getLong(TransportConstants.SESSION_ID_EXTRA, -1);

                    Message removeSessionResponse = Message.obtain();
                    removeSessionResponse.what = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE;
                    if (appIdWithSession.length() > 0) {
                        if (sessionId >= 0) {
                            synchronized (service.REGISTERED_APPS_LOCK) {
                                if (registeredApps != null) {
                                    RegisteredApp appRequesting = registeredApps.get(appIdWithSession);
                                    if (appRequesting != null) {
                                        //Might need to check which session is on which transport
                                        service.removeSessionFromMap((int) sessionId, appRequesting.getTransportsForSession((int) sessionId));
                                        if (appRequesting.removeSession(sessionId)) {
                                            removeSessionResponse.arg1 = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE_SUCESS;
                                        } else {
                                            removeSessionResponse.arg1 = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE_FAILED_SESSION_NOT_FOUND;
                                        }
                                    } else {
                                        removeSessionResponse.arg1 = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE_FAILED_APP_NOT_FOUND;
                                    }
                                }
                            }
                        } else {
                            removeSessionResponse.arg1 = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE_FAILED_SESSION_ID_NOT_INCL;
                        }
                    } else {
                        removeSessionResponse.arg1 = TransportConstants.ROUTER_REMOVE_SESSION_RESPONSE_FAILED_APP_ID_NOT_INCL;
                    }
                    try {
                        msg.replyTo.send(removeSessionResponse); //We do this because we aren't guaranteed to find the correct registeredApp to send the message through
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e2) {
                        DebugTool.logError(TAG, "No reply address included, can't send a reply");
                    }
                    break;
                case TransportConstants.ROUTER_REQUEST_SECONDARY_TRANSPORT_CONNECTION:
                    // Currently this only handles one TCP connection

                    String ipAddress = receivedBundle.getString(ControlFrameTags.RPC.TransportEventUpdate.TCP_IP_ADDRESS);
                    int port = receivedBundle.getInt(ControlFrameTags.RPC.TransportEventUpdate.TCP_PORT);

                    if (ipAddress != null) {
                        if (service.tcpTransport != null) {
                            switch (service.tcpTransport.getState()) {
                                case MultiplexBaseTransport.STATE_CONNECTED:
                                case MultiplexBaseTransport.STATE_CONNECTING:
                                    // A TCP connection is currently active. This version of the
                                    // router service can't handle multiple TCP transports so just
                                    // return a connected message to requester.
                                    if (msg.replyTo != null) {
                                        // Send a transport connect message to the app that requested
                                        // the tcp transport
                                        try {
                                            msg.replyTo.send(service.createHardwareConnectedMessage(service.tcpTransport.transportRecord));
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //Nothing else to do, so return out of this method
                                    return;

                                case MultiplexBaseTransport.STATE_NONE:
                                case MultiplexBaseTransport.STATE_LISTEN:
                                case MultiplexBaseTransport.STATE_ERROR:
                                    //Clear out tcp transport
                                    service.tcpTransport.stop(MultiplexBaseTransport.STATE_NONE);
                                    service.tcpTransport = null;
                                    //Do not return, need to create a new TCP connection
                            }

                        }//else { TCP transport does not exists.}

                        //TCP transport either doesn't exist or is not connected. Start one up.
                        service.tcpTransport = new MultiplexTcpTransport(port, ipAddress, true, service.tcpHandler, service);
                        service.tcpTransport.start();

                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    /**
     * Target we publish for alternative transport (USB) clients to send messages to RouterHandler.
     */
    final Messenger altTransportMessenger = new Messenger(new AltTransportHandler(this));

    /**
     * Handler of incoming messages from an alternative transport (USB).
     */
    @SuppressWarnings("Convert2Diamond")
    static class AltTransportHandler extends Handler {
        final ClassLoader loader;
        final WeakReference<SdlRouterService> provider;

        public AltTransportHandler(SdlRouterService provider) {
            this.provider = new WeakReference<SdlRouterService>(provider);
            loader = getClass().getClassLoader();
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            SdlRouterService service = this.provider.get();
            Bundle receivedBundle = msg.getData();
            switch (msg.what) {
                case TransportConstants.HARDWARE_CONNECTION_EVENT:
                    if (receivedBundle.containsKey(TransportConstants.HARDWARE_DISCONNECTED)) {
                        //We should shut down, so call
                        if (altTransportService != null
                                && altTransportService.equals(msg.replyTo)) {
                            //The same transport that was connected to the router service is now telling us it's disconnected. Let's inform clients and clear our saved messenger
                            altTransportService = null;
                            service.onTransportDisconnected(new TransportRecord(TransportType.valueOf(receivedBundle.getString(TransportConstants.HARDWARE_DISCONNECTED)), null));
                            service.shouldServiceRemainOpen(null); //this will close the service if bluetooth is not available
                        }
                    } else if (receivedBundle.containsKey(TransportConstants.HARDWARE_CONNECTED)) {
                        Message retMsg = Message.obtain();
                        retMsg.what = TransportConstants.ROUTER_REGISTER_ALT_TRANSPORT_RESPONSE;
                        if (altTransportService == null) { //Ok no other transport is connected, this is good
                            DebugTool.logInfo(TAG, "Alt transport connected.");
                            if (msg.replyTo == null) {
                                break;
                            }
                            altTransportService = msg.replyTo;
                            //Clear out the timer to make sure the service knows we're good to go
                            if (service.altTransportTimerHandler != null && service.altTransportTimerRunnable != null) {
                                service.altTransportTimerHandler.removeCallbacks(service.altTransportTimerRunnable);
                            }
                            service.altTransportTimerHandler = null;
                            service.altTransportTimerRunnable = null;

                            //Let the alt transport know they are good to go
                            retMsg.arg1 = TransportConstants.ROUTER_REGISTER_ALT_TRANSPORT_RESPONSE_SUCESS;
                            service.onTransportConnected(new TransportRecord(TransportType.valueOf(receivedBundle.getString(TransportConstants.HARDWARE_CONNECTED)), null));

                        } else { //There seems to be some other transport connected
                            //Error
                            retMsg.arg1 = TransportConstants.ROUTER_REGISTER_ALT_TRANSPORT_ALREADY_CONNECTED;
                        }
                        if (msg.replyTo != null) {
                            try {
                                msg.replyTo.send(retMsg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case TransportConstants.ROUTER_RECEIVED_PACKET:
                    if (receivedBundle != null) {
                        receivedBundle.setClassLoader(loader);//We do this because loading a custom parcelable object isn't possible without it
                        if (receivedBundle.containsKey(TransportConstants.FORMED_PACKET_EXTRA_NAME)) {
                            SdlPacket packet = receivedBundle.getParcelable(TransportConstants.FORMED_PACKET_EXTRA_NAME);
                            if (packet != null) {
                                service.onPacketRead(packet);
                            } else {
                                DebugTool.logWarning(TAG, "Received null packet from alt transport service");
                            }
                        } else {
                            DebugTool.logWarning(TAG, "False positive packet reception");
                        }
                    } else {
                        DebugTool.logError(TAG, "Bundle was null while sending packet to router service from alt transport");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    /**
     * Target we publish for alternative transport (USB) clients to send messages to RouterHandler.
     */
    final Messenger routerStatusMessenger = new Messenger(new RouterStatusHandler(this));

    /**
     * Handler of incoming messages from an alternative transport (USB).
     */
    @SuppressWarnings("Convert2Diamond")
    static class RouterStatusHandler extends Handler {
        final WeakReference<SdlRouterService> provider;

        public RouterStatusHandler(SdlRouterService provider) {
            this.provider = new WeakReference<SdlRouterService>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            SdlRouterService service = this.provider.get();
            switch (msg.what) {
                case TransportConstants.ROUTER_STATUS_CONNECTED_STATE_REQUEST:
                    int flags = msg.arg1;
                    if (msg.replyTo != null) {
                        Message message = Message.obtain();
                        message.what = TransportConstants.ROUTER_STATUS_CONNECTED_STATE_RESPONSE;
                        message.arg1 = (service.isPrimaryTransportConnected()) ? 1 : 0;
                        try {
                            msg.replyTo.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    if (service.isPrimaryTransportConnected() && ((TransportConstants.ROUTER_STATUS_FLAG_TRIGGER_PING & flags) == TransportConstants.ROUTER_STATUS_FLAG_TRIGGER_PING)) {
                        if (service.pingIntent == null) {
                            service.initPingIntent();
                        }
                        AndroidTools.sendExplicitBroadcast(service.getApplicationContext(), service.pingIntent, null);
                    }
                    break;
                default:
                    DebugTool.logWarning(TAG, "Unsupported request: " + msg.what);
                    break;
            }
        }
    }


    /**
     * Target we publish for alternative transport (USB) clients to send messages to RouterHandler.
     */
    final Messenger usbTransferMessenger = new Messenger(new UsbTransferHandler(this));

    /**
     * Handler of incoming messages from an alternative transport (USB).
     */
    @SuppressWarnings("Convert2Diamond")
    static class UsbTransferHandler extends Handler {
        final WeakReference<SdlRouterService> provider;
        Runnable usbCableDisconnectRunnable;
        BroadcastReceiver usbCableDisconnectBroadcastReceiver;

        public UsbTransferHandler(SdlRouterService provider) {
            this.provider = new WeakReference<SdlRouterService>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            SdlRouterService service = this.provider.get();
            switch (msg.what) {
                case TransportConstants.USB_CONNECTED_WITH_DEVICE:
                    service.enterForeground("Opening USB connection", FOREGROUND_TIMEOUT, false);
                    service.resetForegroundTimeOut(FOREGROUND_TIMEOUT);
                    int flags = msg.arg1;

                    ParcelFileDescriptor parcelFileDescriptor = (ParcelFileDescriptor) msg.obj;

                    if (parcelFileDescriptor != null) {
                        //New USB constructor with PFD
                        service.usbTransport = new MultiplexUsbTransport(parcelFileDescriptor, service.usbHandler, msg.getData());


                        usbCableDisconnectRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (provider.get() != null && AndroidTools.isUSBCableConnected(provider.get().getApplicationContext())) {
                                    provider.get().usbTransport.start();
                                }
                            }
                        };
                        postDelayed(usbCableDisconnectRunnable, 4000);


                        // Register a BroadcastReceiver to stop USB transport if USB cable got disconnected
                        if (provider.get() != null) {
                            usbCableDisconnectBroadcastReceiver = new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                                    if (provider.get() != null && plugged != BatteryManager.BATTERY_PLUGGED_AC && plugged != BatteryManager.BATTERY_PLUGGED_USB) {
                                        try {
                                            provider.get().getApplicationContext().unregisterReceiver(usbCableDisconnectBroadcastReceiver);
                                        } catch (Exception e) {
                                        }
                                        removeCallbacks(usbCableDisconnectRunnable);
                                        if (provider.get().usbTransport != null) {
                                            provider.get().usbTransport.stop();
                                        }
                                    }
                                }
                            };
                            provider.get().getApplicationContext().registerReceiver(usbCableDisconnectBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                        }


                    }

                    if (msg.replyTo != null) {
                        Message message = Message.obtain();
                        message.what = TransportConstants.ROUTER_USB_ACC_RECEIVED;
                        try {
                            msg.replyTo.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case TransportConstants.ALT_TRANSPORT_CONNECTED:
                    break;
                default:
                    DebugTool.logWarning(TAG, "Unsupported request: " + msg.what);
                    break;
            }
        }
    }

    /* **************************************************************************************************************************************
     ***********************************************  Life Cycle **************************************************************
     ****************************************************************************************************************************************/

    @Override
    public IBinder onBind(Intent intent) {
        //Check intent to send back the correct binder (client binding vs alt transport)
        if (intent != null) {
            if (closing) {
                DebugTool.logWarning(TAG, "Denying bind request due to service shutting down.");
                return null;
            }
            String requestType = intent.getAction();//intent.getIntExtra(TransportConstants.ROUTER_BIND_REQUEST_TYPE_EXTRA, TransportConstants.BIND_REQUEST_TYPE_CLIENT);
            if (TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT.equals(requestType)) {
                if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) { //Only allow alt transport in debug mode
                    return this.altTransportMessenger.getBinder();
                }
            } else if (TransportConstants.BIND_REQUEST_TYPE_CLIENT.equals(requestType)) {
                return this.routerMessenger.getBinder();
            } else if (TransportConstants.BIND_REQUEST_TYPE_STATUS.equals(requestType)) {
                return this.routerStatusMessenger.getBinder();
            } else if (TransportConstants.BIND_REQUEST_TYPE_USB_PROVIDER.equals(requestType)) {
                return this.usbTransferMessenger.getBinder();
            } else {
                DebugTool.logWarning(TAG, "Unknown bind request type");
            }

        }
        return null;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        DebugTool.logInfo(TAG, "Unbind being called.");
        return super.onUnbind(intent);
    }


    private void notifyClients(final Message message) {
        if (message == null) {
            DebugTool.logWarning(TAG, "Can't notify clients, message was null");
            return;
        }
        DebugTool.logInfo(TAG, "Notifying " + registeredApps.size() + " clients");
        int result;
        synchronized (REGISTERED_APPS_LOCK) {
            Collection<RegisteredApp> apps = registeredApps.values();
            Iterator<RegisteredApp> it = apps.iterator();
            Message formattedMessage = new Message();
            while (it.hasNext()) {
                RegisteredApp app = it.next();
                formattedMessage.copyFrom(message);
                //Format the message for the receiving app and appropriate messaging version
                if (formatMessage(app, formattedMessage)) {
                    result = app.sendMessage(formattedMessage);
                    if (result == RegisteredApp.SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT) {
                        app.close();
                        it.remove();
                    }
                }
            }

        }
    }

    /**
     * Formats the message for the app that is to receive it
     *
     * @param app
     * @param message
     * @return if the message should be sent or not
     */
    protected boolean formatMessage(RegisteredApp app, Message message) {
        if (app.routerMessagingVersion <= 1) {
            Bundle bundle = message.getData();
            if (bundle != null) {
                if (message.what == TransportConstants.HARDWARE_CONNECTION_EVENT) {

                    switch (message.arg1) {
                        case TransportConstants.HARDWARE_CONNECTION_EVENT_CONNECTED:
                            if (app.isRegisteredOnTransport(-1, null)) {
                                //App is already registered on a transport and does not need this update
                                return false;
                            }
                            break;
                        case TransportConstants.HARDWARE_CONNECTION_EVENT_DISCONNECTED:
                            if (bundle.containsKey(TransportConstants.HARDWARE_DISCONNECTED)) {
                                TransportType transportType = TransportType.valueOf(bundle.getString(TransportConstants.HARDWARE_DISCONNECTED));
                                if (!app.isRegisteredOnTransport(-1, transportType)) {
                                    //App is not registered on this transport, not sending
                                    return false;
                                }
                            }
                            if (bundle.containsKey(TransportConstants.TRANSPORT_DISCONNECTED)) {
                                //Unable to handle new parcel TransportRecord
                                bundle.remove(TransportConstants.TRANSPORT_DISCONNECTED);
                            }
                            break;
                    }

                    //All connection event messages should have this as part of the bundle
                    if (bundle.containsKey(TransportConstants.CURRENT_HARDWARE_CONNECTED)) {
                        //Unable to handle new parcel TransportRecord
                        bundle.remove(TransportConstants.CURRENT_HARDWARE_CONNECTED);
                    }


                }
            }
        }
        return true;
    }

    private void pingClients() {
        Message message = Message.obtain();
        DebugTool.logInfo(TAG, "Pinging " + registeredApps.size() + " clients");
        int result;
        synchronized (REGISTERED_APPS_LOCK) {
            Collection<RegisteredApp> apps = registeredApps.values();
            Iterator<RegisteredApp> it = apps.iterator();
            while (it.hasNext()) {
                RegisteredApp app = it.next();
                result = app.sendMessage(message);
                if (result == RegisteredApp.SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT) {
                    app.close();
                    Vector<Long> sessions = app.getSessionIds();
                    for (Long session : sessions) {
                        if (session != null && session != -1) {
                            List<TransportType> transportTypes = app.getTransportsForSession(session.intValue());
                            if (transportTypes != null && transportTypes.size() > 0) {
                                attemptToCleanUpModule(session.intValue(), cachedModuleVersion, transportTypes.get(0));
                            }
                        }
                    }
                    it.remove();
                }
            }
        }
    }

    /**
     * We want to make sure we are in the right process here. If there is some sort of developer error
     * we want to just close out right away.
     *
     * @return if this service is executing in the correct process
     */
    private boolean processCheck() {
        int myPid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        if (am == null || am.getRunningAppProcesses() == null)
            return false; // No RunningAppProcesses, let's close out
        for (RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
            if (processInfo != null && processInfo.pid == myPid) {
                return ROUTER_SERVICE_PROCESS.equals(processInfo.processName);
            }
        }
        return false;

    }

    @SuppressWarnings("SameParameterValue")
    private boolean permissionCheck(String permissionToCheck) {
        if (permissionToCheck == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return PackageManager.PERMISSION_GRANTED == getBaseContext().checkPermission(permissionToCheck, android.os.Process.myPid(), android.os.Process.myUid());
    }

    /**
     * Runs several checks to ensure this router service has the correct conditions to run properly
     *
     * @return true if this service is set up correctly
     */
    private boolean initCheck() {
        if (!processCheck()) {
            DebugTool.logError(TAG, "Not using correct process. Shutting down");
            wrongProcess = true;
            return false;
        }
        if (!permissionCheck(Manifest.permission.BLUETOOTH)) {
            DebugTool.logError(TAG, "Bluetooth Permission is not granted. Shutting down");
            return false;
        }
        if (!AndroidTools.isServiceExported(this, new ComponentName(this, this.getClass()))) { //We want to check to see if our service is actually exported
            DebugTool.logError(TAG, "Service isn't exported. Shutting down");
            return false;
        }

        ComponentName name = new ComponentName(this, this.getClass());
        SdlAppInfo currentAppInfo = null;

        List<SdlAppInfo> sdlAppInfoList = AndroidTools.querySdlAppInfo(getApplicationContext(), new SdlAppInfo.BestRouterComparator(), receivedVehicleType);
        for (SdlAppInfo appInfo : sdlAppInfoList) {
            if (appInfo.getRouterServiceComponentName().equals(name)) {
                currentAppInfo = appInfo;
                break;
            }
        }

        if (currentAppInfo == null) {
            DebugTool.logError(TAG, "AppInfo for current package is not available. Shutting down");
            return false;
        }

        if (!SdlAppInfo.checkIfVehicleSupported(currentAppInfo.getVehicleMakesList(), receivedVehicleType)) {
            DebugTool.logError(TAG, "Received VD is not supported. Shutting down");
            return false;
        }

        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (AndroidTools.isDebugMode(getApplicationContext())) {
            IntegrationValidator.ValidationResult result = IntegrationValidator.validate(getApplicationContext(), this.getClass(), 0);
            if (!result.isSuccessful()) {
                throw new RuntimeException(result.getResultText());
            }
        }
        //Add this first to avoid the runtime exceptions for the entire lifecycle of the service
        setRouterServiceExceptionHandler();
        //This must be done regardless of if this service shuts down or not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hasCalledStartForeground = false;
            enterForeground("Waiting for connection...", FOREGROUND_TIMEOUT / 1000, false);
            hasCalledStartForeground = true;
            resetForegroundTimeOut(FOREGROUND_TIMEOUT / 1000);
        }
    }

    /**
     * The method will attempt to start up the next router service in line based on the sorting criteria of best router service.
     */
    protected void deployNextRouterService() {
        List<SdlAppInfo> sdlAppInfoList = AndroidTools.querySdlAppInfo(getApplicationContext(), new SdlAppInfo.BestRouterComparator(), receivedVehicleType);
        if (sdlAppInfoList != null && !sdlAppInfoList.isEmpty()) {
            ComponentName name = new ComponentName(this, this.getClass());
            SdlAppInfo info;
            int listSize = sdlAppInfoList.size();
            for (int i = 0; i < listSize; i++) {
                info = sdlAppInfoList.get(i);
                if (info.getRouterServiceComponentName().equals(name) && listSize > i + 1) {
                    SdlAppInfo nextUp = sdlAppInfoList.get(i + 1);
                    Intent serviceIntent = new Intent();
                    serviceIntent.setComponent(nextUp.getRouterServiceComponentName());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        startService(serviceIntent);
                    } else {
                        try {
                            startForegroundService(serviceIntent);
                        } catch (Exception e) {
                            DebugTool.logError(TAG, "Unable to start next SDL router service. " + e.getMessage());
                        }
                    }
                    break;

                }
            }
        } else {
            DebugTool.logInfo(TAG, "No sdl apps found");
            return;
        }
        closing = true;
        closeBluetoothSerialServer();
        notifyAltTransportOfClose(TransportConstants.ROUTER_SHUTTING_DOWN_REASON_NEWER_SERVICE);
    }

    public void startUpSequence() {
        IntentFilter disconnectFilter = new IntentFilter();
        disconnectFilter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
        disconnectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        disconnectFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        disconnectFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mListenForDisconnect, disconnectFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(REGISTER_WITH_ROUTER_ACTION);
        registerReceiver(mainServiceReceiver, filter);

        if (!connectAsClient) {
            if (bluetoothAvailable()) {
                initBluetoothSerialService();
            }
        }

        if (altTransportTimerHandler != null) {
            //There's an alt transport waiting for this service to be started
            Intent intent = new Intent(TransportConstants.ALT_TRANSPORT_RECEIVER);
            sendBroadcast(intent);
        }

        startSequenceComplete = true;
    }

    /**
     * This method will set a new UncaughtExceptionHandler for the current thread. The only
     * purpose of the custom UncaughtExceptionHandler is to catch the rare occurrence that the
     * a specific mobile device/OS can't properly handle the deletion and creation of the foreground
     * notification channel that is necessary for foreground services after Android Oreo.
     * The new UncaughtExceptionHandler will catch that specific exception and tell the
     * main looper to continue forward. This still leaves the SdlRouterService killed, but prevents
     * an ANR to the app that makes the startForegroundService call. It will set a flag that will
     * prevent the channel from being deleted in the future and therefore avoiding this exception.
     */
    protected void setRouterServiceExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (defaultUncaughtExceptionHandler != routerServiceExceptionHandler) {
            routerServiceExceptionHandler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (e != null
                            && e instanceof AndroidRuntimeException
                            && "android.app.RemoteServiceException".equals(e.getClass().getName())  //android.app.RemoteServiceException is a private class
                            && e.getMessage() != null
                            && e.getMessage().contains("invalid channel for service notification")) { //This is the message received in the exception for notification channel issues

                        // Set the flag to not delete the notification channel to avoid this exception in the future
                        try {
                            SdlRouterService.this.setSdlRouterServicePrefs(KEY_AVOID_NOTIFICATION_CHANNEL_DELETE, true);
                        } catch (Exception exception) {
                            //Unable to save flag for KEY_AVOID_NOTIFICATION_CHANNEL_DELETE
                        }
                        Looper.loop();
                    } else if (defaultUncaughtExceptionHandler != null) { //No other exception should be handled
                        defaultUncaughtExceptionHandler.uncaughtException(t, e);
                    }
                }
            };
            Thread.setDefaultUncaughtExceptionHandler(routerServiceExceptionHandler);
        }
    }


    @SuppressLint({"NewApi", "MissingPermission"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(TransportConstants.VEHICLE_INFO)) {
            receivedVehicleType = new VehicleType(
                    (HashMap<String, Object>) intent.getSerializableExtra(TransportConstants.VEHICLE_INFO)
            );
        }
        if (firstStart) { // So we only run logic from this classes onCreate once
            firstStart = false;
            if (!initCheck()) { // Run checks on process and permissions
                deployNextRouterService();
                closeSelf();
            }
            initPassed = true;


            synchronized (REGISTERED_APPS_LOCK) {
                registeredApps = new HashMap<String, RegisteredApp>();
            }
            closing = false;

            synchronized (SESSION_LOCK) {
                this.bluetoothSessionMap = new SparseArray<String>();
                this.sessionHashIdMap = new SparseIntArray();
                this.cleanedSessionMap = new SparseIntArray();
            }

            packetExecutor = Executors.newSingleThreadExecutor();

            startUpSequence();
        }
        if (intent != null) {
            if (intent.getBooleanExtra(FOREGROUND_EXTRA, false)) {
                hasCalledStartForeground = false;

                if (!this.isPrimaryTransportConnected()) {    //If there is no transport connected we need to ensure the service is moved to the foreground
                    String address = null;
                    if (intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            address = device.getAddress();
                        }
                    }
                    boolean confirmedDevice = intent.getBooleanExtra(TransportConstants.CONFIRMED_SDL_DEVICE, false);
                    int timeout = getNotificationTimeout(address, confirmedDevice);

                    enterForeground("Waiting for connection...", timeout, false);
                    resetForegroundTimeOut(timeout);
                } else {
                    enterForeground(createConnectedNotificationText(), 0, true);
                }

                hasCalledStartForeground = true;
            }

            if (intent.hasExtra(TransportConstants.PING_ROUTER_SERVICE_EXTRA)) {
                //Make sure we are listening on RFCOMM
                if (startSequenceComplete) { //We only check if we are sure we are already through the start up process
                    DebugTool.logInfo(TAG, "Received ping, making sure we are listening to bluetooth rfcomm");
                    initBluetoothSerialService();
                }
            }
        }

        if (!shouldServiceRemainOpen(intent)) {
            closeSelf();
        }

        if (registeredApps == null) {
            synchronized (REGISTERED_APPS_LOCK) {
                registeredApps = new HashMap<String, RegisteredApp>();
            }
        }
        return START_REDELIVER_INTENT;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDestroy() {
        stopClientPings();

        if (altTransportTimerHandler != null) {
            altTransportTimerHandler.removeCallbacks(altTransportTimerRunnable);
            altTransportTimerHandler = null;
        }

        DebugTool.logWarning(TAG, "Sdl Router Service Destroyed");
        closing = true;
        //No need for this Broadcast Receiver anymore
        unregisterAllReceivers();
        closeBluetoothSerialServer();
        if (registeredApps != null) {
            synchronized (REGISTERED_APPS_LOCK) {
                registeredApps.clear();
                registeredApps = null;
            }
        }
        synchronized (SESSION_LOCK) {
            if (this.bluetoothSessionMap != null) {
                this.bluetoothSessionMap.clear();
                this.bluetoothSessionMap = null;
            }
            if (this.sessionHashIdMap != null) {
                this.sessionHashIdMap.clear();
                this.sessionHashIdMap = null;
            }
        }

        //SESSION_LOCK = null;

        startSequenceComplete = false;
        if (packetExecutor != null) {
            packetExecutor.shutdownNow();
            packetExecutor = null;
        }

        exitForeground();
        if (packetWriteTaskMasterMap != null && packetWriteTaskMasterMap.values() != null) {
            Collection<PacketWriteTaskMaster> tasks = packetWriteTaskMasterMap.values();
            for (PacketWriteTaskMaster packetWriteTaskMaster : tasks) {
                if (packetWriteTaskMaster != null) {
                    packetWriteTaskMaster.close();
                }
            }
        }
        if (packetWriteTaskMasterMap != null) {
            packetWriteTaskMasterMap.clear();
        }
        packetWriteTaskMasterMap = null;


        super.onDestroy();
        System.gc(); //Lower end phones need this hint
        if (!wrongProcess) {
            //noinspection EmptyCatchBlock
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
            }
        }
    }

    private void unregisterAllReceivers() {
        //noinspection EmptyCatchBlock
        try {
            unregisterReceiver(mListenForDisconnect);
            unregisterReceiver(mainServiceReceiver);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void notifyAltTransportOfClose(int reason) {
        if (altTransportService != null) {
            Message msg = Message.obtain();
            msg.what = TransportConstants.ROUTER_SHUTTING_DOWN_NOTIFICATION;
            msg.arg1 = reason;
            try {
                altTransportService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the correct timeout for the foreground notification.
     *
     * @param address the address of the device that is currently connected
     * @return the amount of time for a timeout handler to remove the notification.
     */
    @SuppressLint("MissingPermission")
    private int getNotificationTimeout(String address, boolean confirmedDevice) {
        if (address != null) {
            if (confirmedDevice || hasSDLConnected(address)) {
                return FOREGROUND_TIMEOUT * 2;
            } else if (this.isFirstStatusCheck(address)) {
                // If this is the first time the service has ever connected to this device we want
                // to ensure we have a record of it
                setSDLConnectedStatus(address, false);
                return FOREGROUND_TIMEOUT;
            }
        }
        // If this is a new device or hasn't connected through SDL we want to limit the exposure
        // of the SDL service in the foreground
        return FOREGROUND_TIMEOUT / 1000;
    }

    public void resetForegroundTimeOut(long delay) {
        synchronized (FOREGROUND_NOTIFICATION_LOCK) {
            if (foregroundTimeoutHandler == null) {
                foregroundTimeoutHandler = new Handler();
            }
            if (foregroundTimeoutRunnable == null) {
                foregroundTimeoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!getConnectedTransports().isEmpty()) {
                            // Updates notification to one of still connected transport
                            enterForeground(createConnectedNotificationText(), 0, true);
                            return;
                        } else {
                            exitForeground();//Leave our foreground state as we don't have a connection

                        }
                    }
                };
            } else {
                //This instance likely means there is a callback in the queue so we should remove it
                foregroundTimeoutHandler.removeCallbacks(foregroundTimeoutRunnable);
            }
            foregroundTimeoutHandler.postDelayed(foregroundTimeoutRunnable, delay);
        }
    }

    public void cancelForegroundTimeOut() {
        synchronized (FOREGROUND_NOTIFICATION_LOCK) {
            if (foregroundTimeoutHandler != null && foregroundTimeoutRunnable != null) {
                foregroundTimeoutHandler.removeCallbacks(foregroundTimeoutRunnable);
            }
        }

    }

    @SuppressLint("NewApi")
    private void enterForeground(String content, long chronometerLength, boolean ongoing) {
        DebugTool.logInfo(TAG, "Attempting to enter the foreground - " + System.currentTimeMillis());

        Bitmap icon;
        int resourcesIncluded = getResources().getIdentifier("ic_sdl", "drawable", getPackageName());

        if (resourcesIncluded != 0) {  //No additional pylons required
            icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sdl);
        } else {
            icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.stat_sys_data_bluetooth);
        }
        // Bitmap icon = BitmapFactory.decodeByteArray(SdlLogo.SDL_LOGO_STRING, 0, SdlLogo.SDL_LOGO_STRING.length);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this);
        } else {
            builder = new Notification.Builder(this, SDL_NOTIFICATION_CHANNEL_ID);
        }

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) { //If we are in debug mode, include what app has the router service open
            ComponentName name = new ComponentName(this, this.getClass());
            builder.setContentTitle("SDL: " + name.getPackageName());
        } else {
            builder.setContentTitle("SmartDeviceLink");
        }
        builder.setTicker("SmartDeviceLink");
        builder.setContentText(content);

        //We should use icon from library resources if available
        int trayId = getResources().getIdentifier("sdl_tray_icon", "drawable", getPackageName());

        if (resourcesIncluded != 0) {  //No additional pylons required
            builder.setSmallIcon(trayId);
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth);
        }
        builder.setLargeIcon(icon);
        builder.setOngoing(ongoing);

        // Create an intent that will be fired when the user clicks the notification.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SDL_NOTIFICATION_FAQS_PAGE));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        if (chronometerLength > (FOREGROUND_TIMEOUT / 1000) && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //The countdown method is only available in SDKs >= 24
            // Only add countdown if it is over the min timeout
            builder.setWhen(chronometerLength + System.currentTimeMillis());
            builder.setUsesChronometer(true);
            builder.setChronometerCountDown(true);
        }
        synchronized (FOREGROUND_NOTIFICATION_LOCK) {
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //Now we need to add a notification channel
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    NotificationChannel notificationChannel = new NotificationChannel(SDL_NOTIFICATION_CHANNEL_ID, SDL_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                    notificationChannel.enableLights(false);
                    notificationChannel.enableVibration(false);
                    notificationManager.createNotificationChannel(notificationChannel);
                } else {
                    DebugTool.logError(TAG, "Unable to retrieve notification Manager service");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        safeStartForeground(FOREGROUND_SERVICE_ID, builder.build());
                        stopSelf();    //A valid notification channel must be supplied for SDK 27+
                    }
                }

            }
            notification = builder.build();
            safeStartForeground(FOREGROUND_SERVICE_ID, notification);
            isForeground = true;

        }

    }

    /**
     * This is a simple wrapper around the startForeground method. In the case that the notification
     * is null, or a notification was unable to be created we will still attempt to call the
     * startForeground method in hopes that Android will not throw the System Exception.
     *
     * @param id           notification channel id
     * @param notification the notification to display when in the foreground
     */
    private void safeStartForeground(int id, Notification notification) {
        try {
            if (notification == null) {
                //Try the NotificationCompat this time in case there was a previous error
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this, SDL_NOTIFICATION_CHANNEL_ID)
                                .setContentTitle("SmartDeviceLink")
                                .setContentText("Service Running");
                notification = builder.build();
            }
            startForeground(id, notification);
            DebugTool.logInfo(TAG, "Entered the foreground - " + System.currentTimeMillis());
        } catch (Exception e) {
            DebugTool.logError(TAG, "Unable to start service in foreground", e);
        }
    }

    private void exitForeground() {
        synchronized (FOREGROUND_NOTIFICATION_LOCK) {
            if (isForeground && !isPrimaryTransportConnected()) {    //Ensure that the service is in the foreground and no longer connected to a transport
                DebugTool.logInfo(TAG, "SdlRouterService to exit foreground");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    this.stopForeground(Service.STOP_FOREGROUND_DETACH);
                } else {
                    stopForeground(false);
                }
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    try {
                        notificationManager.cancel(FOREGROUND_SERVICE_ID);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getBooleanPref(KEY_AVOID_NOTIFICATION_CHANNEL_DELETE, false)) {
                            notificationManager.deleteNotificationChannel(SDL_NOTIFICATION_CHANNEL_ID);
                        }
                    } catch (Exception e) {
                        DebugTool.logError(TAG, "Issue when removing notification and channel", e);
                    }
                }
                isForeground = false;
            }
        }
    }


    /**
     * Creates a notification message to attach to the foreground service notification.
     *
     * @return string to be used as the message
     */
    private String createConnectedNotificationText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Connected to ");

        if (bluetoothTransport != null && bluetoothTransport.isConnected()) {
            if (bluetoothTransport.getDeviceName() != null) {
                builder.append(bluetoothTransport.getDeviceName());
                if (0 == (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                    //If this is production, just the device name is fine
                    return builder.toString();
                }
            } else {
                builder.append(TransportType.BLUETOOTH.name().toLowerCase());
            }
        }

        if (usbTransport != null && usbTransport.isConnected()) {
            if (builder.length() > 13) { //13 characters for initial Connected to string
                builder.append(" & ");
            }
            builder.append(TransportType.USB.name());
        }

        return builder.toString();
    }


    /* **************************************************************************************************************************************
     ***********************************************  Helper Methods **************************************************************
     ****************************************************************************************************************************************/

    private ArrayList<TransportRecord> getConnectedTransports() {
        ArrayList<TransportRecord> connected = new ArrayList<>();
        if (bluetoothTransport != null && bluetoothTransport.isConnected()) {
            connected.add(bluetoothTransport.getTransportRecord());
        }

        if (tcpTransport != null && tcpTransport.isConnected()) {
            connected.add(tcpTransport.getTransportRecord());
        }

        if (usbTransport != null && usbTransport.isConnected()) {
            connected.add(usbTransport.getTransportRecord());
        }

        return connected;
    }

    private boolean isPrimaryTransportConnected() {
        return isTransportConnected(TransportType.BLUETOOTH) || isTransportConnected(TransportType.USB);
    }

    private boolean isTransportConnected(TransportType transportType) {
        if (bluetoothTransport != null && transportType.equals(TransportType.BLUETOOTH)) {
            return bluetoothTransport.isConnected();
        } else if (tcpTransport != null && transportType.equals(TransportType.TCP)) {
            return tcpTransport.isConnected();
        } else if (usbTransport != null && transportType.equals(TransportType.USB)) {
            return usbTransport.isConnected();
        }
        return false;
    }

    /**
     * Checks to make sure bluetooth adapter is available and on
     *
     * @return if the bluetooth adapter is available and is enabled
     */
    @SuppressWarnings("MissingPermission")
    private boolean bluetoothAvailable() {
        try {
            return (!(BluetoothAdapter.getDefaultAdapter() == null) && BluetoothAdapter.getDefaultAdapter().isEnabled());
        } catch (NullPointerException e) { // only for BluetoothAdapter.getDefaultAdapter().isEnabled() call
            return false;
        }
    }

    /**
     * 1. If the app has SDL shut off, 												shut down
     * 2. if The app has an Alt Transport address or was started by one, 			stay open
     * 3. If Bluetooth is off/NA	 												shut down
     * 4. Anything else
     */
    public boolean shouldServiceRemainOpen(Intent intent) {
        ArrayList<TransportRecord> connectedTransports = getConnectedTransports();

        if (connectedTransports != null && !connectedTransports.isEmpty()) { // stay open if we have any transports connected
            DebugTool.logInfo(TAG, "1 or more transports connected, remaining open");
            return true;
        } else if (altTransportService != null || altTransportTimerHandler != null) {
            //We have been started by an alt transport, we must remain open. "My life for Auir...."
            DebugTool.logInfo(TAG, "Alt Transport connected, remaining open");
            return true;

        } else if (intent != null && TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT.equals(intent.getAction())) {
            DebugTool.logInfo(TAG, "Received start intent with alt transport request.");
            startAltTransportTimer();
            return true;
        } else if (!bluetoothAvailable()) {//If bluetooth isn't on...there's nothing to see here
            //Bluetooth is off, we should shut down
            DebugTool.logInfo(TAG, "Bluetooth not available, shutting down service");

            return connectedTransports != null && connectedTransports.size() > 0; //If a transport is connected the list will be >0
        } else {
            DebugTool.logInfo(TAG, "Service to remain open");
            return true;
        }
    }

    /**
     * This method is needed so that apps that choose not to implement this as a service as defined by Android, but rather
     * just a simple class we have to know how to shut down.
     */
    public void closeSelf() {
        closing = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasCalledStartForeground) {
            //This must be called before stopping self
            safeStartForeground(FOREGROUND_SERVICE_ID, null);
            exitForeground();
        }

        if (getBaseContext() != null) {
            stopSelf();
        }

        //For good measure.
        onDestroy();

    }

    private synchronized void initBluetoothSerialService() {
        if (legacyModeEnabled) {
            DebugTool.logInfo(TAG, "Not starting own bluetooth during legacy mode");
            return;
        }
        //init serial service
        if (bluetoothTransport == null || bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_ERROR) {
            bluetoothTransport = new MultiplexBluetoothTransport(bluetoothHandler);
        }
        if (bluetoothTransport != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_NONE) {
                // Start the Bluetooth services
                DebugTool.logInfo(TAG, "Starting bluetooth transport");
                bluetoothTransport.start();
            }

        }
    }

    public void onTransportConnected(final TransportRecord record) {
        cancelForegroundTimeOut();
        enterForeground(createConnectedNotificationText(), 0, true);

        if (packetWriteTaskMasterMap == null) {
            packetWriteTaskMasterMap = new ConcurrentHashMap<>();
        }

        TransportType type = record.getType();
        PacketWriteTaskMaster packetWriteTaskMaster = packetWriteTaskMasterMap.get(type);

        if (packetWriteTaskMaster != null) {
            packetWriteTaskMaster.close();
            packetWriteTaskMaster.alert();
        }
        packetWriteTaskMaster = new PacketWriteTaskMaster();
        packetWriteTaskMaster.setTransportType(type);
        packetWriteTaskMaster.start();
        packetWriteTaskMasterMap.put(type, packetWriteTaskMaster);

        Intent startService = new Intent();
        startService.setAction(TransportConstants.START_ROUTER_SERVICE_ACTION);

        startService.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_EXTRA, true);
        startService.putExtra(TransportConstants.FORCE_TRANSPORT_CONNECTED, true);
        startService.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_APP_PACKAGE, getBaseContext().getPackageName());
        startService.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_CMP_NAME, new ComponentName(this, this.getClass()));
        if (receivedVehicleType != null) {
            startService.putExtra(TransportConstants.VEHICLE_INFO, receivedVehicleType.getStore());
        }

        if (record != null && record.getType() != null) {
            startService.putExtra(TransportConstants.START_ROUTER_SERVICE_TRANSPORT_CONNECTED, record.getType().toString());
        }

        startService.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        AndroidTools.sendExplicitBroadcast(getApplicationContext(), startService, null);

        //HARDWARE_CONNECTED
        if (!(registeredApps == null || registeredApps.isEmpty())) {
            //If we have clients
            notifyClients(createHardwareConnectedMessage(record));
        }
    }

    private Message createHardwareConnectedMessage(final TransportRecord record) {
        Message message = Message.obtain();
        message.what = TransportConstants.HARDWARE_CONNECTION_EVENT;
        message.arg1 = TransportConstants.HARDWARE_CONNECTION_EVENT_CONNECTED;
        Bundle bundle = new Bundle();
        bundle.putString(TransportConstants.HARDWARE_CONNECTED, record.getType().name());
        bundle.putParcelableArrayList(TransportConstants.CURRENT_HARDWARE_CONNECTED, getConnectedTransports());

        if (bluetoothTransport != null) {
            bundle.putString(CONNECTED_DEVICE_STRING_EXTRA_NAME, bluetoothTransport.getDeviceName());
        }

        message.setData(bundle);
        return message;

    }

    public void onTransportDisconnected(TransportRecord record) {
        cachedModuleVersion = -1; //Reset our cached version
        //Stop any current pings being sent before the proper state can be determined.
        stopClientPings();

        if (registeredApps != null && !registeredApps.isEmpty()) {
            Message message = Message.obtain();
            message.what = TransportConstants.HARDWARE_CONNECTION_EVENT;
            message.arg1 = TransportConstants.HARDWARE_CONNECTION_EVENT_DISCONNECTED;

            Bundle bundle = new Bundle();
            bundle.putParcelable(TRANSPORT_DISCONNECTED, record);
            //For legacy
            bundle.putString(HARDWARE_DISCONNECTED, record.getType().name());
            bundle.putBoolean(TransportConstants.ENABLE_LEGACY_MODE_EXTRA, legacyModeEnabled);

            //Still connected transports
            bundle.putParcelableArrayList(TransportConstants.CURRENT_HARDWARE_CONNECTED, getConnectedTransports());

            message.setData(bundle);
            notifyClients(message);

            synchronized (REGISTERED_APPS_LOCK) {
                Collection<RegisteredApp> apps = registeredApps.values();
                for (RegisteredApp app : apps) {
                    app.unregisterTransport(-1, record.getType());

                }
            }
        }
        //Remove and close the packet task master assigned to this transport
        if (packetWriteTaskMasterMap != null
                && record != null
                && packetWriteTaskMasterMap.containsKey(record.getType())) {
            PacketWriteTaskMaster master = packetWriteTaskMasterMap.remove(record.getType());
            if (master != null) {
                master.close();
                master.alert();
            }
        }
        if (record != null) {
            //Ensure the associated transport is dealt with
            switch (record.getType()) {
                case BLUETOOTH:
                    synchronized (SESSION_LOCK) {
                        if (bluetoothSessionMap != null) {
                            bluetoothSessionMap.clear();
                        }
                    }
                    if (!connectAsClient) {
                        if (!legacyModeEnabled && !closing) {
                            initBluetoothSerialService();
                        }
                    }
                    break;
                case USB:
                    if (usbTransport != null) {
                        usbTransport = null;
                    }
                    synchronized (SESSION_LOCK) {
                        if (usbSessionMap != null) {
                            usbSessionMap.clear();
                        }
                    }
                    break;
                case TCP:
                    if (tcpTransport != null) {
                        tcpTransport = null;
                    }
                    synchronized (SESSION_LOCK) {
                        if (tcpSessionMap != null) {
                            tcpSessionMap.clear();
                        }
                    }
                    break;
            }
        }

        if (!getConnectedTransports().isEmpty()) {
            // Updates notification to one of still connected transport
            enterForeground(createConnectedNotificationText(), 0, true);
            return;
        } else {
            exitForeground();//Leave our foreground state as we don't have a connection anymore
            if (!shouldServiceRemainOpen(null)) {
                closeSelf();
            }
        }

        if (altTransportService != null) {  //If we still have an alt transport open, then we don't need to tell the clients to close
            return;
        }

        DebugTool.logError(TAG, "Notifying client service of hardware disconnect.");


        //We've notified our clients, less clean up the mess now.
        synchronized (SESSION_LOCK) {
            this.sessionHashIdMap.clear();
        }
        synchronized (REGISTERED_APPS_LOCK) {
            if (registeredApps == null) {
                return;
            }
            registeredApps.clear();
        }
    }

    public void onTransportError(TransportRecord transport, Bundle errorBundle) {
        switch (transport.getType()) {
            case BLUETOOTH:
                if (bluetoothTransport != null) {
                    bluetoothTransport.setStateManually(MultiplexBluetoothTransport.STATE_NONE);
                    bluetoothTransport = null;
                }
                if (errorBundle != null && errorBundle.getByte(MultiplexBaseTransport.ERROR_REASON_KEY) == MultiplexBaseTransport.REASON_SPP_ERROR) {
                    notifySppError();
                }
                break;
            case USB:
                break;
            case TCP:
                break;
        }

    }

    public void onPacketRead(SdlPacket packet) {
        try {
            //Log.i(TAG, "******** Read packet with header: " +(packet).toString());
            if (packet.getVersion() == 1) {
                if (packet.getFrameType() == FrameType.Control && packet.getFrameInfo() == SdlPacket.FRAME_INFO_START_SERVICE_ACK) {
                    //We received a v1 packet from the head unit, this means we can't use the router service.
                    //Enable legacy mode
                    enableLegacyMode(true);
                    return;
                }
            } else if (cachedModuleVersion == -1) {
                cachedModuleVersion = packet.getVersion();
            }
            //Send the received packet to the registered app
            sendPacketToRegisteredApp(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler for all Multiplex Based transports.
     * It will ensure messages are properly queued back to the router service.
     */
    private static class TransportHandler extends Handler {

        final WeakReference<SdlRouterService> provider;

        public TransportHandler(SdlRouterService provider) {
            this.provider = new WeakReference<SdlRouterService>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            SdlRouterService service = this.provider.get();
            switch (msg.what) {
                case MESSAGE_DEVICE_NAME:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        service.setSDLConnectedStatus(bundle.getString(MultiplexBaseTransport.DEVICE_ADDRESS), true);
                    }
                    break;
                case MESSAGE_STATE_CHANGE:
                    TransportRecord transportRecord = (TransportRecord) msg.obj;
                    switch (msg.arg1) {
                        case MultiplexBaseTransport.STATE_CONNECTED:
                            service.onTransportConnected(transportRecord);
                            break;
                        case MultiplexBaseTransport.STATE_CONNECTING:
                            // Currently attempting to connect - update UI?
                            break;
                        case MultiplexBaseTransport.STATE_LISTEN:
                            break;
                        case MultiplexBaseTransport.STATE_NONE:
                            // We've just lost the connection
                            service.onTransportDisconnected(transportRecord);
                            break;
                        case MultiplexBaseTransport.STATE_ERROR:
                            service.onTransportError(transportRecord, msg.getData());
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    service.onPacketRead((SdlPacket) msg.obj);
                    break;
            }
        }
    }

    //The return false after the packet null check is not dead code. Read the getByteArray method from bundle
    public boolean writeBytesToTransport(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        byte[] packet = bundle.getByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME);
        if (packet == null) {
            DebugTool.logWarning(TAG, "Ignoring null packet");
            return false;
        }
        int offset = bundle.getInt(TransportConstants.BYTES_TO_SEND_EXTRA_OFFSET, 0); //If nothing, start at the beginning of the array
        int count = bundle.getInt(TransportConstants.BYTES_TO_SEND_EXTRA_COUNT, packet.length);  //In case there isn't anything just send the whole packet.
        TransportType transportType = TransportType.valueForString(bundle.getString(TransportConstants.TRANSPORT_TYPE));
        if (transportType != null) {
            switch ((transportType)) {
                case BLUETOOTH:
                    if (bluetoothTransport != null && bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_CONNECTED) {
                        bluetoothTransport.write(packet, offset, count);
                        return true;
                    }
                case USB:
                    if (usbTransport != null && usbTransport.getState() == MultiplexBaseTransport.STATE_CONNECTED) {
                        usbTransport.write(packet, offset, count);
                        return true;
                    }
                case TCP:
                    if (tcpTransport != null && tcpTransport.getState() == MultiplexBaseTransport.STATE_CONNECTED) {
                        tcpTransport.write(packet, offset, count);
                        return true;
                    }
                default:
                    if (sendThroughAltTransport(bundle)) {
                        return true;
                    }
            }
        }
        DebugTool.logError(TAG, "Can't send data, no transport  of specified type connected");
        return false;
    }

    private boolean manuallyWriteBytes(TransportType transportType, byte[] packet, int offset, int count) {
        switch ((transportType)) {
            case BLUETOOTH:
                if (bluetoothTransport != null && bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_CONNECTED) {
                    bluetoothTransport.write(packet, offset, count);
                    return true;
                }
            case USB:
                if (usbTransport != null && usbTransport.getState() == MultiplexBaseTransport.STATE_CONNECTED) {
                    usbTransport.write(packet, offset, count);
                    return true;
                }
            case TCP:
                if (tcpTransport != null && tcpTransport.getState() == MultiplexBaseTransport.STATE_CONNECTED) {
                    tcpTransport.write(packet, offset, count);
                    return true;
                }
            default:
                return sendThroughAltTransport(packet, offset, count);
        }
    }


    /**
     * This Method will send the packets through the alt transport that is connected
     *
     * @param bundle This bundle will have its what changed and sent off to the alt transport
     * @return If it was possible to send the packet off.
     * <p><b>NOTE: This is not guaranteed. It is a best attempt at sending the packet, it may fail.</b>
     */
    private boolean sendThroughAltTransport(Bundle bundle) {
        if (altTransportService != null) {
            Message msg = Message.obtain();
            msg.what = TransportConstants.ROUTER_SEND_PACKET;
            msg.setData(bundle);
            try {
                altTransportService.send(msg);
            } catch (RemoteException e) {
                DebugTool.logError(TAG, "Unable to send through alt transport!");
                e.printStackTrace();
            }
            return true;
        } else {
            DebugTool.logWarning(TAG, "Unable to send packet through alt transport, it was null");
        }
        return false;
    }

    /**
     * This Method will send the packets through the alt transport that is connected
     *
     * @param bytes The byte array of data to be wrote out
     * @return If it was possible to send the packet off.
     * <p><b>NOTE: This is not guaranteed. It is a best attempt at sending the packet, it may fail.</b>
     */
    private boolean sendThroughAltTransport(byte[] bytes, int offset, int count) {
        if (altTransportService != null) {
            Message msg = Message.obtain();
            msg.what = TransportConstants.ROUTER_SEND_PACKET;
            Bundle bundle = new Bundle();
            bundle.putByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME, bytes);
            bundle.putInt(TransportConstants.BYTES_TO_SEND_EXTRA_OFFSET, offset);
            bundle.putInt(TransportConstants.BYTES_TO_SEND_EXTRA_COUNT, count);
            msg.setData(bundle);
            try {
                altTransportService.send(msg);
            } catch (RemoteException e) {
                DebugTool.logError(TAG, "Unable to send through alt transport!");
                e.printStackTrace();
            }
            return true;
        } else {
            DebugTool.logWarning(TAG, "Unable to send packet through alt transport, it was null");
        }
        return false;
    }

    /**
     * This will send the received packet to the registered service. It will default to the single registered "foreground" app.
     * This can be overridden to provide more specific functionality.
     *
     * @param packet the packet that is received
     * @return whether or not the sending was successful
     */
    public boolean sendPacketToRegisteredApp(SdlPacket packet) {
        if (registeredApps != null && registeredApps.size() > 0) {
            final int session = packet.getSessionId();
            boolean isNewSessionRequest = false, isNewTransportRequest = false;

            final int frameInfo = packet.getFrameInfo();
            if (packet.getFrameType() == FrameType.Control) {
                isNewSessionRequest = (frameInfo == SdlPacket.FRAME_INFO_START_SERVICE_ACK || frameInfo == SdlPacket.FRAME_INFO_START_SERVICE_NAK)
                        && packet.getServiceType() == SdlPacket.SERVICE_TYPE_RPC;
                isNewTransportRequest = (frameInfo == SdlPacket.FRAME_INFO_REGISTER_SECONDARY_TRANSPORT_ACK
                        || frameInfo == SdlPacket.FRAME_INFO_REGISTER_SECONDARY_TRANSPORT_NAK); // && packet.getServiceType() != SdlPacket.SERVICE_TYPE_RPC;
            }

            //Find where this packet should go
            String appid = getAppIDForSession(session, isNewSessionRequest, isNewTransportRequest, packet.getTransportRecord().getType());

            if (appid != null && appid.length() > 0) {

                RegisteredApp app;
                synchronized (REGISTERED_APPS_LOCK) {
                    app = registeredApps.get(appid);
                }

                if (app == null) {
                    DebugTool.logError(TAG, "No app found for app id " + appid + " Removing session mapping and sending unregisterAI to head unit.");

                    //We have no app to match the app id tied to this session
                    removeSessionFromMap(session, Collections.singletonList(packet.getTransportRecord().getType()));

                    final int serviceType = packet.getServiceType();
                    if (serviceType == SdlPacket.SERVICE_TYPE_RPC || serviceType == SdlPacket.SERVICE_TYPE_BULK_DATA) {
                        //This is a primary transport packet as it is an RPC packet
                        //Create an unregister app interface to remove the app as it doesn't appear to exist anymore
                        byte[] uai = createForceUnregisterApp((byte) session, (byte) packet.getVersion());
                        manuallyWriteBytes(packet.getTransportRecord().getType(), uai, 0, uai.length);

                        int hashId = 0;
                        synchronized (this.SESSION_LOCK) {
                            if (this.sessionHashIdMap.indexOfKey(session) >= 0) {
                                hashId = this.sessionHashIdMap.get(session);
                                this.sessionHashIdMap.delete(session);
                            }
                        }

                        //TODO stop other services on that transport for the session with no app
                        byte[] stopService = SdlPacketFactory.createEndSession(SessionType.RPC, (byte) session, 0, (byte) packet.getVersion(), hashId).constructPacket();
                        manuallyWriteBytes(packet.getTransportRecord().getType(), stopService, 0, stopService.length);
                    } else {
                        DebugTool.logWarning(TAG, "No where to send a packet from what appears to be a non primary transport");
                    }

                    return false;
                }

                //There is an app id and can continue to normal flow
                byte version = (byte) packet.getVersion();

                if (isNewSessionRequest && version > 1 && packet.getFrameInfo() == SdlPacket.FRAME_INFO_START_SERVICE_ACK) { //we know this was a start session response
                    if (version >= 5) {
                        Integer hashId = (Integer) packet.getTag(ControlFrameTags.RPC.StartServiceACK.HASH_ID);
                        if (hashId != null) {
                            synchronized (SESSION_LOCK) {
                                this.sessionHashIdMap.put(session, hashId);
                            }
                        } else {
                            DebugTool.logWarning(TAG, "Hash ID not found in V5 start service ACK frame for session " + session);
                        }
                    } else {
                        if (packet.getPayload() != null && packet.getDataSize() == 4) { //hashid will be 4 bytes in length
                            synchronized (SESSION_LOCK) {
                                this.sessionHashIdMap.put(session, (BitConverter.intFromByteArray(packet.getPayload(), 0)));
                            }
                        }
                    }
                }

                // check and prevent a UAI from being passed to an app that is using a recycled session id
                if (cleanedSessionMap != null && cleanedSessionMap.size() > 0) {
                    if (packet.getFrameType() == FrameType.Single && packet.getServiceType() == SdlPacket.SERVICE_TYPE_RPC) {
                        BinaryFrameHeader binFrameHeader = BinaryFrameHeader.parseBinaryHeader(packet.getPayload());
                        if (binFrameHeader != null && FunctionID.UNREGISTER_APP_INTERFACE.getId() == binFrameHeader.getFunctionID()) {
                            DebugTool.logInfo(TAG, "Received an unregister app interface. Checking session hash before sending");
                            // make sure that we don't try to unregister a recently added app that might have a
                            // session ID of a removed app whose UAI was delayed
                            int hashOfRemoved = this.cleanedSessionMap.get(session, -1);
                            int currentHash = this.sessionHashIdMap.get(session, -1);
                            if (hashOfRemoved != -1) {
                                // Current session contains key that was held before
                                if (hashOfRemoved != currentHash) {
                                    // App assigned same session id but is a different app. Keep this from being killed
                                    DebugTool.logInfo(TAG, "same session id for different apps found, dropping packet");
                                    this.cleanedSessionMap.delete(session);
                                    return false;
                                }
                            }
                        }
                    }
                }

                int packetSize = (int) (packet.getDataSize() + SdlPacket.HEADER_SIZE);
                //Log.i(TAG, "Checking packet size: " + packetSize);
                Message message = Message.obtain();
                Bundle bundle = new Bundle();

                if (packetSize < ByteArrayMessageSpliter.MAX_BINDER_SIZE) { //This is a small enough packet just send on through
                    //Log.w(TAG, " Packet size is just right " + packetSize  + " is smaller than " + ByteArrayMessageSpliter.MAX_BINDER_SIZE + " = " + (packetSize<ByteArrayMessageSpliter.MAX_BINDER_SIZE));
                    message.what = TransportConstants.ROUTER_RECEIVED_PACKET;

                    // !!!! ADD ADDITIONAL ITEMS TO BUNDLE HERE !!!

                    packet.setMessagingVersion(app.routerMessagingVersion);
                    bundle.putParcelable(FORMED_PACKET_EXTRA_NAME, packet);
                    /* !!!!!! DO NOT ADD ANY ADDITIONAL ITEMS TO THE BUNDLE AFTER PACKET. ONLY BYTES_TO_SEND_FLAG !!!!!!!*/
                    bundle.putInt(TransportConstants.BYTES_TO_SEND_FLAGS, TransportConstants.BYTES_TO_SEND_FLAG_NONE);
                    /* !!!!!! DO NOT ADD ANY ADDITIONAL ITEMS TO THE BUNDLE AFTER PACKET. ONLY BYTES_TO_SEND_FLAG !!!!!!!*/

                    message.setData(bundle);
                    return sendPacketMessageToClient(app, message, version);
                } else {
                    //Log.w(TAG, "Packet too big for IPC buffer. Breaking apart and then sending to client.");
                    //We need to churn through the packet payload and send it in chunks
                    byte[] bytes = packet.getPayload();
                    SdlPacket copyPacket = new SdlPacket(packet.getVersion(), packet.isEncrypted(),
                            (int) packet.getFrameType().getValue(),
                            packet.getServiceType(), packet.getFrameInfo(), session,
                            (int) packet.getDataSize(), packet.getMessageId(), null);
                    message.what = TransportConstants.ROUTER_RECEIVED_PACKET;
                    // !!!! ADD ADDITIONAL ITEMS TO BUNDLE HERE !!!

                    bundle.putParcelable(FORMED_PACKET_EXTRA_NAME, copyPacket);
                    /* !!!!!! DO NOT ADD ANY ADDITIONAL ITEMS TO THE BUNDLE AFTER PACKET. ONLY BYTES_TO_SEND_FLAG !!!!!!!*/
                    bundle.putInt(TransportConstants.BYTES_TO_SEND_FLAGS, TransportConstants.BYTES_TO_SEND_FLAG_SDL_PACKET_INCLUDED);
                    /* !!!!!! DO NOT ADD ANY ADDITIONAL ITEMS TO THE BUNDLE AFTER PACKET. ONLY BYTES_TO_SEND_FLAG !!!!!!!*/

                    message.setData(bundle);
                    //Log.d(TAG, "First packet before sending: " + message.getData().toString());
                    if (!sendPacketMessageToClient(app, message, version)) {
                        DebugTool.logWarning(TAG, "Error sending first message of split packet to client " + app.appId);
                        return false;
                    }
                    //Log.w(TAG, "Message too big for single IPC transaction. Breaking apart. Size - " +  packet.getDataSize());
                    ByteArrayMessageSpliter splitter = new ByteArrayMessageSpliter(appid, TransportConstants.ROUTER_RECEIVED_PACKET, bytes, 0);
                    while (splitter.isActive()) {
                        if (!sendPacketMessageToClient(app, splitter.nextMessage(), version)) {
                            DebugTool.logWarning(TAG, "Error sending first message of split packet to client " + app.appId);
                            splitter.close();
                            return false;
                        }
                    }
                    //Log.i(TAG, "Large packet finished being sent");
                }

            } else {    //If we can't find a session for this packet we just drop the packet
                DebugTool.logError(TAG, "App Id was NULL for session! " + session);
                TransportType transportType = packet.getTransportRecord().getType();
                if (removeSessionFromMap(session, Collections.singletonList(transportType))) { //If we found the session id still tied to an app in our map we need to remove it and send the proper shutdown sequence.
                    DebugTool.logInfo(TAG, "Removed session from map.  Sending unregister request to module.");
                    attemptToCleanUpModule(session, packet.getVersion(), transportType);
                } else { //There was no mapping so let's try to resolve this

                    if (packet.getFrameType() == FrameType.Single && packet.getServiceType() == SdlPacket.SERVICE_TYPE_RPC) {
                        BinaryFrameHeader binFrameHeader = BinaryFrameHeader.parseBinaryHeader(packet.getPayload());
                        if (binFrameHeader != null && FunctionID.UNREGISTER_APP_INTERFACE.getId() == binFrameHeader.getFunctionID()) {
                            DebugTool.logInfo(TAG, "Received an unregister app interface with no where to send it, dropping the packet.");
                        } else {
                            attemptToCleanUpModule(session, packet.getVersion(), transportType);
                        }
                    } else if ((packet.getFrameType() == FrameType.Control
                            && (packet.getFrameInfo() == SdlPacket.FRAME_INFO_END_SERVICE_ACK || packet.getFrameInfo() == SdlPacket.FRAME_INFO_END_SERVICE_NAK))) {
                        //We want to ignore this
                        DebugTool.logInfo(TAG, "Received a stop service ack/nak with no where to send it, dropping the packet.");
                    } else {
                        attemptToCleanUpModule(session, packet.getVersion(), transportType);
                    }
                }
            }
        }
        return false;
    }

    /**
     * This method is an all else fails situation. If the head unit is out of sync with the apps on the phone
     * this method will clear out an unwanted or out of date session.
     *
     * @param session the session id that is to be cleaned up
     * @param version the last known version that this session was operating with
     */
    private void attemptToCleanUpModule(int session, int version, TransportType primaryTransport) {
        DebugTool.logInfo(TAG, "Attempting to stop session " + session);
        byte[] uai = createForceUnregisterApp((byte) session, (byte) version);
        manuallyWriteBytes(primaryTransport, uai, 0, uai.length);
        int hashId = 0;
        synchronized (this.SESSION_LOCK) {
            if (this.sessionHashIdMap.indexOfKey(session) >= 0) {
                hashId = this.sessionHashIdMap.get(session);
                this.sessionHashIdMap.delete(session);
                this.cleanedSessionMap.put(session, hashId);
            }
        }
        byte[] stopService = SdlPacketFactory.createEndSession(SessionType.RPC, (byte) session, 0, (byte) version, hashId).constructPacket();
        manuallyWriteBytes(primaryTransport, stopService, 0, stopService.length);
    }

    private boolean sendPacketMessageToClient(RegisteredApp app, Message message, byte version) {
        int result = app.sendMessage(message);
        if (result == RegisteredApp.SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT) {
            DebugTool.logInfo(TAG, "Dead object, removing app and sessions");
            //Get all their sessions and send out unregister info
            //Use the version in this packet as a best guess
            app.close();
            Vector<Long> sessions = app.getSessionIds();
            byte[] unregister, stopService;
            int size = sessions.size(), sessionId;
            for (int i = 0; i < size; i++) {
                sessionId = sessions.get(i).intValue();
                unregister = createForceUnregisterApp((byte) sessionId, version);
                List<TransportType> transportTypes = app.getTransportsForSession(sessionId);
                if (transportTypes != null && !transportTypes.isEmpty()) {
                    manuallyWriteBytes(transportTypes.get(0), unregister, 0, unregister.length);
                    int hashId = 0;
                    synchronized (this.SESSION_LOCK) {
                        if (this.sessionHashIdMap.indexOfKey(sessionId) >= 0) {
                            hashId = this.sessionHashIdMap.get(sessionId);
                        }
                    }
                    stopService = SdlPacketFactory.createEndSession(SessionType.RPC, (byte) sessionId, 0, version, hashId).constructPacket();

                    manuallyWriteBytes(transportTypes.get(0), stopService, 0, stopService.length);
                    synchronized (SESSION_LOCK) {
                        this.bluetoothSessionMap.remove(sessionId);
                        this.sessionHashIdMap.delete(sessionId);
                    }
                }
            }
            synchronized (REGISTERED_APPS_LOCK) {
                registeredApps.remove(app.appId);
            }
            return false;//We did our best to correct errors
        }
        return true;//We should have sent our packet, so we can return true now
    }

    private synchronized void closeBluetoothSerialServer() {
        if (bluetoothTransport != null) {
            bluetoothTransport.stop();
            bluetoothTransport = null;
        }
    }

    /**
     * This function looks through the phones currently paired bluetooth devices
     * If one of the devices' names contain "sync", or livio it will attempt to connect the RFCOMM
     * And start SDL
     *
     * @return a boolean if a connection was attempted
     */
    @SuppressWarnings({"MissingPermission"})
    public synchronized boolean bluetoothQuerryAndConnect() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            Set<BluetoothDevice> pairedBT = adapter.getBondedDevices();
            DebugTool.logInfo(TAG, "Query Bluetooth paired devices");
            if (pairedBT.size() > 0) {
                for (BluetoothDevice device : pairedBT) {
                    String name = device.getName().toLowerCase(Locale.US);
                    if (name.contains("sync") || name.contains("livio")) {
                        bluetoothConnect(device);
                        return true;
                    }
                }
            }
        } else {
            DebugTool.logError(TAG, "There was an issue with connecting as client");
        }
        return false;
    }

    @SuppressWarnings("MissingPermission")
    private synchronized boolean bluetoothConnect(BluetoothDevice device) {
        DebugTool.logInfo(TAG, "Connecting to device: " + device.getName());
        if (bluetoothTransport == null || !bluetoothTransport.isConnected()) {    // Set up the Bluetooth serial object
            bluetoothTransport = new MultiplexBluetoothTransport(bluetoothHandler);
        }
        // We've been given a device - let's connect to it
        if (bluetoothTransport.getState() != MultiplexBluetoothTransport.STATE_CONNECTING) {
            bluetoothTransport.connect(device);
            if (bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_CONNECTING) {
                return true;
            }
        }

        DebugTool.logInfo(TAG, "Bluetooth SPP Connect Attempt Completed");
        return false;
    }


    //**************************************************************************************************************************************
    //********************************************************* PREFERENCES ****************************************************************
    //**************************************************************************************************************************************

    /**
     * Set the connection establishment status of the particular device
     *
     * @param address         address of the device in quesiton
     * @param hasSDLConnected true if a connection has been established, false if not
     */
    protected void setSDLConnectedStatus(String address, boolean hasSDLConnected) {
        SharedPreferences preferences = this.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(address, hasSDLConnected);
        editor.commit();
    }

    /**
     * Checks to see if a device address has connected to SDL before.
     *
     * @param address the mac address of the device in quesiton
     * @return if this is the first status check of this device
     */
    protected boolean isFirstStatusCheck(String address) {
        SharedPreferences preferences = this.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
        return !preferences.contains(address);
    }

    /**
     * Checks to see if a device address has connected to SDL before.
     *
     * @param address the mac address of the device in quesiton
     * @return if an SDL connection has ever been established with this device
     */
    protected boolean hasSDLConnected(String address) {
        SharedPreferences preferences = this.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
        return preferences.contains(address) && preferences.getBoolean(address, false);
    }

    /**
     * Set specific settings through key/value to the SDL_ROUTER_SERVICE_PREFS
     *
     * @param key   the key of the pair to set in the preferences
     * @param value boolean to attach to key in the preferences
     */
    protected void setSdlRouterServicePrefs(String key, boolean value) {
        SharedPreferences preferences = this.getSharedPreferences(SDL_ROUTER_SERVICE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
        DebugTool.logInfo(TAG, "Preference set: " + key + " : " + value);
    }

    /**
     * Retrieves a boolean value for the given key in the SDL_ROUTER_SERVICE_PREFS
     *
     * @param key          the string key that will be used to retrieve the boolean value
     * @param defaultValue if they key does not exist or there is no value to be found, this is the
     *                     value that will be returned
     * @return the value associated with the supplied key or defaultValue if one does not exist
     */
    protected boolean getBooleanPref(String key, boolean defaultValue) {
        SharedPreferences preferences = this.getSharedPreferences(SDL_ROUTER_SERVICE_PREFS, Context.MODE_PRIVATE);
        if (preferences != null) {
            return preferences.getBoolean(key, defaultValue);
        }
        return false;
    }


    /* ***********************************************************************************************************************************************************************
     * *****************************************************************  CUSTOM ADDITIONS  ************************************************************************************
     *************************************************************************************************************************************************************************/

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    protected static LocalRouterService getLocalRouterService(Intent launchIntent, ComponentName name) {
        if (launchIntent == null) {
            DebugTool.logWarning(TAG, "Supplied intent was null, local router service will not contain intent");
        }
        if (name == null) {
            DebugTool.logError(TAG, "Unable to create local router service object because component name was null");
            return null;
        }
        //noinspection deprecation
        return new LocalRouterService(launchIntent, ROUTER_SERVICE_VERSION_NUMBER, System.currentTimeMillis(), name);
    }

    /**
     * This method is used to check for the newest version of this class to make sure the latest and greatest is up and running.
     */
    private void startAltTransportTimer() {
        altTransportTimerHandler = new Handler();
        altTransportTimerRunnable = new Runnable() {
            public void run() {
                altTransportTimerHandler = null;
                altTransportTimerRunnable = null;
                if (!shouldServiceRemainOpen(null)) {
                    closeSelf();
                }
            }
        };
        altTransportTimerHandler.postDelayed(altTransportTimerRunnable, ALT_TRANSPORT_TIMEOUT_RUNNABLE);
    }

    /**
     * Removes session from map if the key is found.
     *
     * @param sessionId the session id that is to be removed from our current mapping
     * @return if the key was found
     */
    private boolean removeSessionFromMap(int sessionId, List<TransportType> transportTypes) {
        synchronized (SESSION_LOCK) {
            boolean retVal = false;
            if (transportTypes != null) {    //FIXME I don't believe this should be null
                if (transportTypes.contains(TransportType.BLUETOOTH) && bluetoothSessionMap != null) {
                    if (bluetoothSessionMap.indexOfKey(sessionId) >= 0) {
                        bluetoothSessionMap.remove(sessionId);
                        retVal = true;
                    }
                } else if (transportTypes.contains(TransportType.USB) && usbSessionMap != null) {
                    if (usbSessionMap.indexOfKey(sessionId) >= 0) {
                        usbSessionMap.remove(sessionId);
                        retVal = true;
                    }
                } else if (transportTypes.contains(TransportType.TCP) && tcpSessionMap != null) {
                    if (tcpSessionMap.indexOfKey(sessionId) >= 0) {
                        tcpSessionMap.remove(sessionId);
                        retVal = true;
                    }
                }
            }
            return retVal;
        }
    }


    private boolean removeAllSessionsWithAppId(String appId) {
        synchronized (SESSION_LOCK) {
            if (bluetoothSessionMap != null) {
                SparseArray<String> iter = bluetoothSessionMap.clone();
                int size = iter.size();
                for (int i = 0; i < size; i++) {
                    if (iter.valueAt(i).compareTo(appId) == 0) {
                        sessionHashIdMap.delete(iter.keyAt(i));
                        bluetoothSessionMap.removeAt(i);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all sessions from the sessions map for this given app id
     *
     * @param app         the RegisteredApp object that should have all its sessions removed
     * @param cleanModule a flag if this service should attempt to clear off the sessions tied to the app off the module
     */
    private void removeAllSessionsForApp(RegisteredApp app, boolean cleanModule) {
        Vector<Long> sessions = app.getSessionIds();
        int size = sessions.size(), sessionId;
        for (int i = 0; i < size; i++) {
            //Log.d(TAG, "Investigating session " +sessions.get(i).intValue());
            //Log.d(TAG, "App id is: " + bluetoothSessionMap.get(sessions.get(i).intValue()));
            sessionId = sessions.get(i).intValue();
            List<TransportType> transportTypes = app.getTransportsForSession(sessionId);
            removeSessionFromMap(sessionId, transportTypes);
            if (cleanModule) {
                if (transportTypes != null && transportTypes.size() > 0) {
                    attemptToCleanUpModule(sessionId, cachedModuleVersion, transportTypes.get(0));
                }
            }
        }
    }

    private boolean removeAppFromMap(RegisteredApp app) {
        synchronized (REGISTERED_APPS_LOCK) {
            //noinspection SuspiciousMethodCalls
            RegisteredApp old = registeredApps.remove(app);
            if (old != null) {
                old.close();
                return true;
            }
        }
        return false;
    }

    private String getAppIDForSession(int sessionId, boolean newSession, boolean newTransport, TransportType transportType) {
        synchronized (SESSION_LOCK) {
            //Log.d(TAG, "Looking for session: " + sessionId);
            //First get the session map for the correct transport
            SparseArray<String> sessionMap;
            switch (transportType) {
                case BLUETOOTH:
                    if (bluetoothSessionMap == null) {
                        bluetoothSessionMap = new SparseArray<String>();
                    }
                    sessionMap = bluetoothSessionMap;
                    break;
                case USB:
                    if (usbSessionMap == null) {
                        usbSessionMap = new SparseArray<String>();
                    }
                    sessionMap = usbSessionMap;
                    break;
                case TCP:
                    if (tcpSessionMap == null) {
                        tcpSessionMap = new SparseArray<String>();
                    }
                    sessionMap = tcpSessionMap;
                    break;
                default:
                    return null;
            }

            String appId = sessionMap.get(sessionId);
            if (appId == null) {
                // If service type is RPC then we know we need to just skip ahead and see if there
                // is a registered app awaiting a session.
                if (newSession) {
                    int pos;
                    synchronized (REGISTERED_APPS_LOCK) {
                        for (RegisteredApp app : registeredApps.values()) {
                            if (app.getAwaitingSession().contains(transportType)) {
                                pos = app.containsSessionId(-1);
                                if (pos != -1) {
                                    app.setSessionId(pos, sessionId);
                                    app.registerTransport(sessionId, transportType);
                                    app.getAwaitingSession().remove(transportType);
                                    appId = app.getAppId();
                                    sessionMap.put(sessionId, appId);
                                    break;
                                }
                            }
                        }
                    }
                } else if (newTransport) {

                    // If this is anything other than RPC with a start service response we can assume
                    // the app wants to use a new transport as secondary.

                    // We would only receive a start service response for RPC service when an app is
                    // attempting to register for the first time. Other services can be ran on
                    //secondary transports.
                    switch (transportType) {
                        case BLUETOOTH:            //Check for BT as a secondary transport
                            //USB is potential primary
                            appId = usbSessionMap.get(sessionId);
                            // No other suitable transport for primary transport
                            break;
                        case USB:                //Check for USB as a secondary transport
                            //BT potential primary transport
                            appId = bluetoothSessionMap.get(sessionId);
                            // No other suitable transport for primary transport
                            break;
                        case TCP:                //Check for TCP as a secondary transport
                            //BT potential primary transport
                            appId = bluetoothSessionMap.get(sessionId);
                            if (appId == null) {
                                //USB is potential primary transport
                                appId = usbSessionMap.get(sessionId);
                            }
                            break;
                        default:
                            return null;
                    }

                    if (appId != null) {
                        //This means that there is a session id of the same id on another transport
                        synchronized (REGISTERED_APPS_LOCK) {
                            RegisteredApp app = registeredApps.get(appId);
                            //Ensure a registered app actually exists and is not null
                            if (app != null) {
                                //Register this new transport for the app and add the entry to the
                                //session map associated with this transport
                                app.registerTransport(sessionId, transportType);
                                sessionMap.put(sessionId, appId);
                            } else {
                                DebugTool.logWarning(TAG, "No registered app found when register secondary transport");
                            }
                        }
                    }
                }

            }
            //Log.d(TAG, sessionId + " session returning App Id: " + appId);
            return appId;
        }
    }

    /* ****************************************************************************************************************************************
	// ***********************************************************   LEGACY   ****************************************************************
	//*****************************************************************************************************************************************/
    private boolean legacyModeEnabled = false;

    private void enableLegacyMode(boolean enable) {
        DebugTool.logInfo(TAG, "Enable legacy mode: " + enable);
        legacyModeEnabled = enable; //We put this at the end to avoid a race condition between the bluetooth d/c and notify of legacy mode enabled

        if (legacyModeEnabled) {
            //So we need to let the clients know they need to host their own bluetooth sessions because the currently connected head unit only supports a very old version of SDL/Applink
            //Start by closing our own bluetooth connection. The following calls will handle actually notifying the clients of legacy mode
            closeBluetoothSerialServer();
            //Now wait until we get a d/c, then the apps should shut their bluetooth down and go back to normal

        }//else{}

    }

	/* ****************************************************************************************************************************************
	// ***********************************************************   UTILITY   ****************************************************************
	//*****************************************************************************************************************************************/

    private void debugPacket(byte[] bytes) {
        //DEBUG

        if (bytes[0] != 0x00) {
            DebugTool.logInfo(TAG, "Writing packet with header: " + BitConverter.bytesToHex(bytes, 12)); //just want the header
        } else {

            //Log.d(TAG, "Writing packet with binary header: " + BitConverter.bytesToHex(bytes, 12)); //just want the header
            //int length = bytes.length-12;
            if (bytes.length <= 8) {
                DebugTool.logWarning(TAG, "No payload to debug or too small");
                return;
            }
            //Check first byte if 0, make to json
            char[] buffer = new char[bytes.length];
            for (int i = 12; i < bytes.length; i++) {
                buffer[i - 12] = (char) (bytes[i] & 0xFF);
            }
            try {

                JSONObject object = new JSONObject(new String(buffer));
                DebugTool.logInfo(TAG, "JSON: " + object.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * If an app crashes the only way we can handle it being on the head unit is to send an unregister app interface rpc.
     * This method should only be called when the router service recognizes the client is no longer valid
     *
     * @param sessionId session id that is currently being cleared from the module. It will be used to form the packet.
     * @param version   the last known version this session was operating with
     * @return a byte array that contains the full packet for an UnregisterAppInterface that can be written out to the transport
     */
    private byte[] createForceUnregisterApp(byte sessionId, byte version) {
        UnregisterAppInterface request = new UnregisterAppInterface();
        request.setCorrelationID(UNREGISTER_APP_INTERFACE_CORRELATION_ID);
        request.format(null, true);
        byte[] msgBytes = JsonRPCMarshaller.marshall(request, version);
        ProtocolMessage pm = new ProtocolMessage();
        pm.setData(msgBytes);
        pm.setSessionID(sessionId);
        pm.setMessageType(MessageType.RPC);
        pm.setSessionType(SessionType.RPC);
        pm.setFunctionID(FunctionID.getFunctionId(request.getFunctionName()));
        pm.setCorrID(request.getCorrelationID());
        if (request.getBulkData() != null)
            pm.setBulkData(request.getBulkData());
        byte[] data;
        if (version > 1) { //If greater than v1 we need to include a binary frame header in the data before all the JSON starts
            data = new byte[12 + pm.getJsonSize()];
            BinaryFrameHeader binFrameHeader = SdlPacketFactory.createBinaryFrameHeader(pm.getRPCType(), pm.getFunctionID(), pm.getCorrID(), pm.getJsonSize());
            System.arraycopy(binFrameHeader.assembleHeaderBytes(), 0, data, 0, 12);
            System.arraycopy(pm.getData(), 0, data, 12, pm.getJsonSize());
        } else {
            data = pm.getData();
        }

        SdlPacket packet = new SdlPacket(version, false, SdlPacket.FRAME_TYPE_SINGLE, SdlPacket.SERVICE_TYPE_RPC, 0, sessionId, data.length, data.length + 100, data);
        return packet.constructPacket();
    }


    /**
     * Method for finding the next, highest priority write task from all connected apps.
     *
     * @return the next task for writing out packets if one exists
     */
    protected PacketWriteTask getNextTask(TransportType transportType) {
        final long currentTime = System.currentTimeMillis();
        RegisteredApp priorityApp = null;
        long currentPriority = -Long.MAX_VALUE, peekWeight;
        synchronized (REGISTERED_APPS_LOCK) {
            PacketWriteTask peekTask;
            for (RegisteredApp app : registeredApps.values()) {
                peekTask = app.peekNextTask(transportType);
                if (peekTask != null) {
                    peekWeight = peekTask.getWeight(currentTime);
                    //Log.v(TAG, "App " + app.appId +" has a task with weight "+ peekWeight);
                    if (peekWeight > currentPriority) {
                        if (app.queuePaused) {
                            app.notIt(transportType);//Reset the timer
                            continue;
                        }
                        if (priorityApp != null) {
                            priorityApp.notIt(transportType);
                        }
                        currentPriority = peekWeight;
                        priorityApp = app;
                    }
                }
            }
            if (priorityApp != null) {
                return priorityApp.getNextTask(transportType);
            }
        }
        return null;
    }

    private void initPingIntent() {
        pingIntent = new Intent();
        pingIntent.setAction(TransportConstants.START_ROUTER_SERVICE_ACTION);
        pingIntent.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_EXTRA, true);
        pingIntent.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_APP_PACKAGE, getBaseContext().getPackageName());
        pingIntent.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_CMP_NAME, new ComponentName(SdlRouterService.this, SdlRouterService.this.getClass()));
        pingIntent.putExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_PING, true);
        if (receivedVehicleType != null) {
            pingIntent.putExtra(TransportConstants.VEHICLE_INFO, receivedVehicleType.getStore());
        }
    }

    private void startClientPings() {
        synchronized (this) {
            if (!isPrimaryTransportConnected()) { //If we aren't connected, bail
                return;
            }
            if (isPingingClients) {
                DebugTool.logWarning(TAG, "Already pinging clients. Resting count");
                synchronized (PING_COUNT_LOCK) {
                    pingCount = 0;
                }
                return;
            }
            if (clientPingExecutor == null) {
                clientPingExecutor = Executors.newSingleThreadScheduledExecutor();
            }
            isPingingClients = true;
            synchronized (PING_COUNT_LOCK) {
                pingCount = 0;
            }

            clientPingExecutor.scheduleAtFixedRate(new Runnable() {
                List<ResolveInfo> sdlApps;

                @Override
                public void run() {
                    if (getPingCount() >= 10) {
                        DebugTool.logInfo(TAG, "Hit ping limit");
                        stopClientPings();
                        return;
                    }
                    if (pingIntent == null) {
                        initPingIntent();
                    }

                    if (sdlApps == null) {
                        sdlApps = getPackageManager().queryBroadcastReceivers(pingIntent, 0);
                    }

                    AndroidTools.sendExplicitBroadcast(getApplicationContext(), pingIntent, sdlApps);
                    synchronized (PING_COUNT_LOCK) {
                        pingCount++;
                    }

                }
            }, CLIENT_PING_DELAY, CLIENT_PING_DELAY, TimeUnit.MILLISECONDS); //Give a little delay for first call
        }
    }

    private int getPingCount() {
        synchronized (PING_COUNT_LOCK) {
            return pingCount;
        }
    }

    private void stopClientPings() {
        if (clientPingExecutor != null && !clientPingExecutor.isShutdown()) {
            clientPingExecutor.shutdownNow();
            clientPingExecutor = null;
            isPingingClients = false;
        }
        pingIntent = null;
    }

	/* ****************************************************************************************************************************************
	// **********************************************************   TINY CLASSES   ************************************************************
	//*****************************************************************************************************************************************/

    /**
     * This class enables us to compare two router services
     * from different apps and determine which is the newest
     * and therefore which one should be the one spun up.
     *
     * @author Joey Grover
     * @deprecated Move to the new version checking system with meta-data
     */
    @SuppressWarnings({"DeprecatedIsStillUsed"})
    @Deprecated
    static class LocalRouterService implements Parcelable {
        Intent launchIntent = null;
        int version;
        final long timestamp;
        ComponentName name;

        @SuppressWarnings("SameParameterValue")
        private LocalRouterService(Intent intent, int version, long timeStamp, ComponentName name) {
            this.launchIntent = intent;
            this.version = version;
            this.timestamp = timeStamp;
            this.name = name;
        }

        /**
         * Check if input is newer than this version
         *
         * @param service a reference to another possible router service that is in quesiton
         * @return if the supplied service is newer than this one
         */
        public boolean isNewer(@SuppressWarnings("deprecation") LocalRouterService service) {
            if (service.version > this.version) {
                return true;
            } else if (service.version == this.version) { //If we have the same version, we will use a timestamp
                return service.timestamp < this.timestamp;
            }
            return false;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isEqual(@SuppressWarnings("deprecation") LocalRouterService service) {
            return service != null && service.name != null && this.name != null && this.name.equals(service.name);
        }

        @Override
        public String toString() {
            StringBuilder build = new StringBuilder();
            build.append("Intent action: ");
            if (launchIntent != null && launchIntent.getComponent() != null) {
                build.append(launchIntent.getComponent().getClassName());
            } else if (name != null) {
                build.append(name.getClassName());
            }

            build.append(" Version: ");
            build.append(version);
            build.append(" Timestamp: ");
            build.append(timestamp);

            return build.toString();
        }

        public LocalRouterService(Parcel p) {
            this.version = p.readInt();
            this.timestamp = p.readLong();
            try {
                this.launchIntent = p.readParcelable(Intent.class.getClassLoader());
                this.name = p.readParcelable(ComponentName.class.getClassLoader());
            } catch (Exception e) {
                // catch DexException
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(version);
            dest.writeLong(timestamp);
            dest.writeParcelable(launchIntent, 0);
            dest.writeParcelable(name, 0);

        }

        @SuppressWarnings("deprecation")
        public static final Parcelable.Creator<LocalRouterService> CREATOR = new Parcelable.Creator<LocalRouterService>() {
            @SuppressWarnings("deprecation")
            public LocalRouterService createFromParcel(Parcel in) {
                //noinspection deprecation
                return new LocalRouterService(in);
            }

            @SuppressWarnings("deprecation")
            @Override
            public LocalRouterService[] newArray(int size) {
                //noinspection deprecation
                return new LocalRouterService[size];
            }

        };

    }


    /**
     * This class helps keep track of all the different sessions established with the head unit
     * and to which app they belong to.
     *
     * @author Joey Grover
     */
    @SuppressWarnings("Convert2Diamond")
    class RegisteredApp {
        protected static final int SEND_MESSAGE_SUCCESS = 0x00;
        protected static final int SEND_MESSAGE_ERROR_MESSAGE_NULL = 0x01;
        protected static final int SEND_MESSAGE_ERROR_MESSENGER_NULL = 0x02;
        protected static final int SEND_MESSAGE_ERROR_MESSENGER_GENERIC_EXCEPTION = 0x03;
        protected static final int SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT = 0x04;

        protected static final int PAUSE_TIME_FOR_QUEUE = 1500;

        private final Object TRANSPORT_LOCK = new Object();

        final String appId;
        final Messenger messenger;
        final Vector<Long> sessionIds;
        final Vector<TransportType> awaitingSession;
        final int routerMessagingVersion;

        ByteAraryMessageAssembler buffer;
        int priorityForBuffingMessage;
        DeathRecipient deathNote = null;
        //Packet queue vars
        final ConcurrentHashMap<TransportType, PacketWriteTaskBlockingQueue> queues;
        Handler queueWaitHandler;
        Runnable queueWaitRunnable = null;
        boolean queuePaused = false;

        //Primary will always be first
        final SparseArray<ArrayList<TransportType>> registeredTransports;

        /**
         * This is a simple class to hold onto a reference of a registered app.
         *
         * @param appId                  the supplied id for this app that is attempting to register
         * @param routerMessagingVersion
         * @param messenger              the specific messenger that is tied to this app
         */
        public RegisteredApp(String appId, int routerMessagingVersion, Messenger messenger) {
            this.appId = appId;
            this.messenger = messenger;
            this.sessionIds = new Vector<Long>();
            this.queues = new ConcurrentHashMap<>();
            queueWaitHandler = new Handler();
            registeredTransports = new SparseArray<ArrayList<TransportType>>();
            awaitingSession = new Vector<>();
            setDeathNote(); //messaging Version
            this.routerMessagingVersion = routerMessagingVersion;
        }


        /**
         * Closes this app properly.
         */
        public void close() {
            clearDeathNote();
            clearBuffer();
            Collection<PacketWriteTaskBlockingQueue> queueCollection = queues.values();
            for (PacketWriteTaskBlockingQueue queue : queueCollection) {
                if (queue != null) {
                    queue.clear();
                }
            }
            queueCollection.clear();

            if (queueWaitHandler != null) {
                if (queueWaitRunnable != null) {
                    queueWaitHandler.removeCallbacks(queueWaitRunnable);
                }
                queueWaitHandler = null;
            }
        }

        public String getAppId() {
            return appId;
        }

		/*public long getAppId() {
			return appId;
		}*/

        /**
         * This is a convenience variable and may not be used or useful in different protocols
         *
         * @return a vector of all the session ids associated with this app
         */
        public Vector<Long> getSessionIds() {
            return sessionIds;
        }

        /**
         * Returns the position of the desired object if it is contained in the vector. If not it will return -1.
         *
         * @param id a session id value that is in question to be associated with this app
         * @return the index of the supplied session id or -1 if it is not associated with this app
         */
        public int containsSessionId(long id) {
            return sessionIds.indexOf(id);
        }

        /**
         * This will remove a session from the session id list
         *
         * @param sessionId the id of the session that should be removed
         * @return if the session was successfully removed, or false if the session id wasn't associated with this app.
         */
        public boolean removeSession(Long sessionId) {
            int location = sessionIds.indexOf(sessionId);
            if (location >= 0) {
                Long removedSessionId = sessionIds.remove(location);
                synchronized (TRANSPORT_LOCK) {
                    registeredTransports.remove(sessionId.intValue());
                }
                return removedSessionId != null;
            } else {
                return false;
            }
        }

        /**
         * This method is to manually put a session id into the mapping. This method should be used with extreme caution and
         * only in certain cases when the sesion id needs to exist at a specific position in the mapping (overwriting a value)
         *
         * @param position  the position at which the session id should be placed
         * @param sessionId the session id that will be put into the specific position in the mapping
         * @throws ArrayIndexOutOfBoundsException if the position is outside of the current size of the sessionIds vector
         */
        public void setSessionId(int position, long sessionId) throws ArrayIndexOutOfBoundsException {
            this.sessionIds.set(position, sessionId);
            synchronized (TRANSPORT_LOCK) {
                this.registeredTransports.put((int) sessionId, new ArrayList<TransportType>());
            }
        }

        public void clearSessionIds() {
            this.sessionIds.clear();
        }

        public Vector<TransportType> getAwaitingSession() {
            return awaitingSession;
        }

        protected void registerTransport(int sessionId, TransportType transportType) {
            synchronized (TRANSPORT_LOCK) {
                ArrayList<TransportType> transportTypes = this.registeredTransports.get(sessionId);
                if (transportTypes != null) {
                    if (queues.get(transportType) == null) {
                        queues.put(transportType, new PacketWriteTaskBlockingQueue());
                    }
                    transportTypes.add(transportType);
                    this.registeredTransports.put(sessionId, transportTypes);
                }

            }
        }

        /**
         * @param sessionId     the session id to find if a transport is registered. -1 for sessionId will
         *                      trigger a search through all sessions.
         * @param transportType the transport type to find if a transport is registered. If null is
         *                      passed, will return true for any transport being registered on
         * @return
         */
        protected boolean isRegisteredOnTransport(int sessionId, TransportType transportType) {
            synchronized (TRANSPORT_LOCK) {
                if (this.registeredTransports.indexOfKey(sessionId) >= 0) {
                    if (transportType == null) {
                        return this.registeredTransports.get(sessionId).size() > 0;
                    }
                    return this.registeredTransports.get(sessionId).indexOf(transportType) >= 0;
                } else if (sessionId < 0) {

                    //Check if any session is registered on this transport
                    int numberOfSessions = registeredTransports.size();
                    ArrayList<TransportType> transportTypes;
                    for (int i = 0; i < numberOfSessions; i++) {
                        transportTypes = registeredTransports.valueAt(i);
                        if (transportTypes != null) {
                            if (transportType == null && transportTypes.size() > 0) {
                                return true;
                            }
                            for (TransportType type : transportTypes) {
                                if (type.equals(transportType)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;

                } else {
                    return false;
                }
            }
        }

        protected List<TransportType> getTransportsForSession(int sessionId) {
            synchronized (TRANSPORT_LOCK) {
                if (this.registeredTransports.indexOfKey(sessionId) >= 0) {
                    return this.registeredTransports.get(sessionId);
                } else {
                    return null;
                }
            }
        }

        protected boolean unregisterTransport(int sessionId, @NonNull TransportType transportType) {
            if (queues != null && queues.containsKey(transportType)) {
                PacketWriteTaskBlockingQueue queue = queues.remove(transportType);
                if (queue != null) {
                    queue.clear();
                }
            }
            synchronized (TRANSPORT_LOCK) {
                if (sessionId == -1) {
                    int size = this.registeredTransports.size();
                    for (int i = 0; i < size; i++) {
                        this.registeredTransports.valueAt(i).remove(transportType);
                    }
                    return true;
                } else if (this.registeredTransports.indexOfKey(sessionId) >= 0) {
                    return this.registeredTransports.get(sessionId).remove(transportType);
                } else {
                    return false;
                }
            }

        }

        protected void unregisterAllTransports(int sessionId) {
            synchronized (TRANSPORT_LOCK) {
                if (this.registeredTransports.indexOfKey(sessionId) >= 0) {
                    this.registeredTransports.get(sessionId).clear();
                } else if (sessionId == -1) {
                    int size = this.registeredTransports.size();
                    for (int i = 0; i < size; i++) {
                        this.registeredTransports.valueAt(i).clear();
                    }
                }
            }
        }


        /**
         * This method will attempt to return a transport type that can be associated to this
         * registered app
         *
         * @return
         */
        private TransportType getCompatPrimaryTransport() {
            synchronized (TRANSPORT_LOCK) {
                if (this.registeredTransports != null && this.registeredTransports.size() > 0) {
                    Object obj = this.registeredTransports.valueAt(0);
                    //Lint shows to ignore this call, but there are crash logs that show otherwise
                    if (obj != null && obj instanceof List) {
                        try {
                            List<TransportType> transportTypes = (List<TransportType>) obj;
                            if (transportTypes != null) {
                                if (transportTypes.get(0) != null) {
                                    return transportTypes.get(0);
                                }
                            }
                        } catch (ClassCastException e) {
                            DebugTool.logError(TAG, "Unable to cast transport list", e);
                        }
                    }
                }
            }

            //No transport stored
            if (bluetoothTransport != null && bluetoothTransport.isConnected()) {
                return TransportType.BLUETOOTH;
            } else if (usbTransport != null && usbTransport.isConnected()) {
                return TransportType.USB;
            } else if (tcpTransport != null && tcpTransport.isConnected()) {
                return TransportType.TCP;
            }

            return TransportType.BLUETOOTH;
        }

        @SuppressWarnings("SameReturnValue")
        public boolean handleIncommingClientMessage(final Bundle receivedBundle) {
            int flags = receivedBundle.getInt(TransportConstants.BYTES_TO_SEND_FLAGS, TransportConstants.BYTES_TO_SEND_FLAG_NONE);
            TransportType transportType = TransportType.valueForString(receivedBundle.getString(TransportConstants.TRANSPORT_TYPE));
            if (transportType == null) {
                synchronized (TRANSPORT_LOCK) {
                    transportType = getCompatPrimaryTransport();
                }
                receivedBundle.putString(TransportConstants.TRANSPORT_TYPE, transportType.name());
            }

            if (flags != TransportConstants.BYTES_TO_SEND_FLAG_NONE) {
                byte[] packet = receivedBundle.getByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME);
                if (flags == TransportConstants.BYTES_TO_SEND_FLAG_LARGE_PACKET_START) {
                    this.priorityForBuffingMessage = receivedBundle.getInt(TransportConstants.PACKET_PRIORITY_COEFFICIENT, 0);
                }
                handleMessage(flags, packet, transportType);
            } else {
                //Add the write task on the stack
                PacketWriteTaskBlockingQueue queue = queues.get(transportType);
                if (queue == null) {    //TODO check to see if there is any better place to put this
                    queue = new PacketWriteTaskBlockingQueue();
                    queues.put(transportType, queue);
                }
                queue.add(new PacketWriteTask(receivedBundle));
                if (packetWriteTaskMasterMap != null) {
                    PacketWriteTaskMaster packetWriteTaskMaster = packetWriteTaskMasterMap.get(transportType);
                    if (packetWriteTaskMaster != null) {
                        packetWriteTaskMaster.alert();
                    }
                } //If this is null, it is likely the service is closing
            }
            return true;
        }

        public int sendMessage(Message message) {
            if (this.messenger == null) {
                return SEND_MESSAGE_ERROR_MESSENGER_NULL;
            }
            if (message == null) {
                return SEND_MESSAGE_ERROR_MESSAGE_NULL;
            }
            try {
                this.messenger.send(message);
                return SEND_MESSAGE_SUCCESS;
            } catch (RemoteException e) {
                e.printStackTrace();
                if (e instanceof DeadObjectException) {
                    return SEND_MESSAGE_ERROR_MESSENGER_DEAD_OBJECT;
                } else {
                    return SEND_MESSAGE_ERROR_MESSENGER_GENERIC_EXCEPTION;
                }
            }
        }

        public void handleMessage(int flags, byte[] packet, TransportType transportType) {
            if (flags == TransportConstants.BYTES_TO_SEND_FLAG_LARGE_PACKET_START) {
                clearBuffer();
                buffer = new ByteAraryMessageAssembler();
                buffer.init();
                buffer.setTransportType(transportType);
            }
            if (buffer != null) {
                if (!buffer.handleMessage(flags, packet)) { //If this returns false
                    DebugTool.logError(TAG, "Error handling bytes");
                }
                if (buffer.isFinished()) { //We are finished building the buffer so we should write the bytes out
                    byte[] bytes = buffer.getBytes();
                    PacketWriteTaskBlockingQueue queue = queues.get(transportType);
                    if (queue != null) {
                        queue.add(new PacketWriteTask(bytes, 0, bytes.length, this.priorityForBuffingMessage, transportType));
                        if (packetWriteTaskMasterMap != null) {
                            PacketWriteTaskMaster packetWriteTaskMaster = packetWriteTaskMasterMap.get(transportType);
                            if (packetWriteTaskMaster != null) {
                                packetWriteTaskMaster.alert();
                            }
                        }
                    }
                    buffer.close();
                }
            }
        }

        protected PacketWriteTask peekNextTask(TransportType transportType) {
            PacketWriteTaskBlockingQueue queue = queues.get(transportType);
            if (queue != null) {
                return queue.peek();
            }
            return null;
        }

        protected PacketWriteTask getNextTask(TransportType transportType) {
            PacketWriteTaskBlockingQueue queue = queues.get(transportType);
            if (queue != null) {
                return queue.poll();
            }
            return null;
        }

        /**
         * This will inform the local app object that it was not picked to have the highest priority. This will allow the user to continue to perform interactions
         * with the module and not be bogged down by large packet requests.
         */
        protected void notIt(final TransportType transportType) {
            PacketWriteTaskBlockingQueue queue = queues.get(transportType);
            if (queue != null && queue.peek().priorityCoefficient > 0) { //If this has any sort of priority coefficient we want to make it wait.
                //Flag to wait
                if (queueWaitHandler == null) {
                    DebugTool.logError(TAG, "Unable to pause queue, handler was null");
                }
                if (queueWaitRunnable == null) {
                    queueWaitRunnable = new Runnable() {

                        @Override
                        public void run() {
                            pauseQueue(false);
                            PacketWriteTaskMaster packetWriteTaskMaster = packetWriteTaskMasterMap.get(transportType);
                            if (packetWriteTaskMaster != null) {
                                packetWriteTaskMaster.alert();
                            }
                        }
                    };
                }
                if (queuePaused) {
                    queueWaitHandler.removeCallbacks(queueWaitRunnable);
                }
                pauseQueue(queueWaitHandler.postDelayed(queueWaitRunnable, PAUSE_TIME_FOR_QUEUE));
            }
        }

        private void pauseQueue(boolean paused) {
            this.queuePaused = paused;
        }

        protected void clearBuffer() {
            if (buffer != null) {
                buffer.close();
                buffer = null;
            }
        }

        protected boolean setDeathNote() {
            if (messenger != null) {
                if (deathNote == null) {
                    deathNote = new DeathRecipient() {
                        final Object deathLock = new Object();

                        @Override
                        public void binderDied() {
                            synchronized (deathLock) {
                                DebugTool.logWarning(TAG, "Binder died for app " + RegisteredApp.this.appId);
                                if (messenger.getBinder() != null) {
                                    messenger.getBinder().unlinkToDeath(this, 0);
                                }
                                removeAllSessionsForApp(RegisteredApp.this, true);
                                removeAppFromMap(RegisteredApp.this);
                                startClientPings();
                            }
                        }
                    };
                }
                try {
                    messenger.getBinder().linkToDeath(deathNote, 0);
                    return true;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        }

        protected boolean clearDeathNote() {
            return messenger != null && messenger.getBinder() != null && deathNote != null && messenger.getBinder().unlinkToDeath(deathNote, 0);
        }
    }

    /**
     * A runnable task for writing out packets.
     *
     * @author Joey Grover
     */
    public class PacketWriteTask implements Runnable {
        private static final long DELAY_CONSTANT = 500; //250ms
        private static final long SIZE_CONSTANT = 1000; //1kb
        private static final long PRIORITY_COEF_CONSTANT = 500;
        private static final int DELAY_COEF = 1;
        private static final int SIZE_COEF = 1;

        private byte[] bytesToWrite;
        private final int offset, size, priorityCoefficient;
        private final long timestamp;
        final Bundle receivedBundle;
        TransportType transportType;

        public PacketWriteTask(byte[] bytes, int offset, int size, int priorityCoefficient, TransportType transportType) {
            timestamp = System.currentTimeMillis();
            bytesToWrite = bytes;
            this.offset = offset;
            this.size = size;
            this.priorityCoefficient = priorityCoefficient;
            receivedBundle = null;
            this.transportType = transportType;
        }

        public PacketWriteTask(Bundle bundle) {
            this.receivedBundle = bundle;
            timestamp = System.currentTimeMillis();
            bytesToWrite = bundle.getByteArray(TransportConstants.BYTES_TO_SEND_EXTRA_NAME);
            offset = bundle.getInt(TransportConstants.BYTES_TO_SEND_EXTRA_OFFSET, 0); //If nothing, start at the beginning of the array
            size = bundle.getInt(TransportConstants.BYTES_TO_SEND_EXTRA_COUNT, bytesToWrite.length);  //In case there isn't anything just send the whole packet.
            this.priorityCoefficient = bundle.getInt(TransportConstants.PACKET_PRIORITY_COEFFICIENT, 0);
            this.transportType = TransportType.valueForString(receivedBundle.getString(TransportConstants.TRANSPORT_TYPE));

        }

        protected void setTransportType(TransportType transportType) {
            this.transportType = transportType;
        }

        @Override
        public void run() {
            if (receivedBundle != null) {
                writeBytesToTransport(receivedBundle);
            } else if (bytesToWrite != null) {
                manuallyWriteBytes(this.transportType, bytesToWrite, offset, size);
            }
        }

        private long getWeight(long currentTime) { //Time waiting - size - priority_coef
            return ((((currentTime - timestamp) + DELAY_CONSTANT) * DELAY_COEF) - ((size - SIZE_CONSTANT) * SIZE_COEF) - (priorityCoefficient * PRIORITY_COEF_CONSTANT));
        }
    }

    /**
     * Extends thread to consume PacketWriteTasks in a priority queue fashion. It will attempt to look
     * at all apps serial queue of tasks and compare them
     *
     * @author Joey Grover
     */
    private class PacketWriteTaskMaster extends Thread {
        protected final Object QUEUE_LOCK = new Object();
        private boolean isHalted = false, isWaiting = false;
        private TransportType transportType;

        public PacketWriteTaskMaster() {
            this.setName("PacketWriteTaskMaster");
            this.setDaemon(true);
        }

        protected void setTransportType(TransportType transportType) {
            this.transportType = transportType;
        }

        @Override
        public void run() {
            while (!isHalted) {
                try {
                    PacketWriteTask task;
                    synchronized (QUEUE_LOCK) {
                        task = getNextTask(transportType);
                        if (task != null) {
                            task.run();
                        } else {
                            isWaiting = true;
                            QUEUE_LOCK.wait();
                            isWaiting = false;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void alert() {
            if (isWaiting) {
                synchronized (QUEUE_LOCK) {
                    QUEUE_LOCK.notify();
                }
            }
        }

        private void close() {
            this.isHalted = true;
        }
    }

    /**
     * Custom queue to prioritize packet write tasks based on their priority coefficient.<br> The queue is a doubly linked list.<br><br>
     * When a tasks is added to the queue, it will be evaluated using it's priority coefficient. If the coefficient is greater than 0, it will simply
     * be placed at the end of the queue. If the coefficient is equal to 0, the queue will begin to iterate at the head and work it's way back. Once it is found that the current
     * tasks has a priority coefficient greater than 0, it will be placed right before that task. The idea is to keep a semi-serial queue but creates a priority that allows urgent
     * tasks such as UI related to skip near the front. However, it is assumed those tasks of higher priority should also be handled in a serial fashion.
     *
     * @author Joey Grover
     */
    @SuppressWarnings("Convert2Diamond")
    private class PacketWriteTaskBlockingQueue {
        final class Node<E> {
            final E item;
            Node<E> prev;
            Node<E> next;

            Node(E item, Node<E> previous, Node<E> next) {
                this.item = item;
                this.prev = previous;
                this.next = next;
            }
        }

        private Node<PacketWriteTask> head;
        private Node<PacketWriteTask> tail;

        /**
         * This will take the given task and insert it at the tail of the queue
         *
         * @param task the task to be inserted at the tail of the queue
         */
        private void insertAtTail(PacketWriteTask task) {
            if (task == null) {
                throw new NullPointerException();
            }
            Node<PacketWriteTask> oldTail = tail;
            Node<PacketWriteTask> newTail = new Node<PacketWriteTask>(task, oldTail, null);
            tail = newTail;
            if (head == null) {
                head = newTail;
            } else {
                oldTail.next = newTail;
            }

        }

        /**
         * This will take the given task and insert it at the head of the queue
         *
         * @param task the task to be inserted at the head of the queue
         */
        private void insertAtHead(PacketWriteTask task) {
            if (task == null) {
                throw new NullPointerException();
            }
            Node<PacketWriteTask> oldHead = head;
            Node<PacketWriteTask> newHead = new Node<PacketWriteTask>(task, null, oldHead);
            head = newHead;
            if (tail == null) {
                tail = newHead;
            } else {
                if (oldHead != null) {
                    oldHead.prev = newHead;
                }
            }
        }

        /**
         * Insert the task in the queue where it belongs
         *
         * @param task the new PacketWriteTask that needs to be added to the queue to be handled
         */
        public void add(PacketWriteTask task) {
            synchronized (this) {
                if (task == null) {
                    throw new NullPointerException();
                }

                //If we currently don't have anything in our queue
                if (head == null || tail == null) {
                    Node<PacketWriteTask> taskNode = new Node<PacketWriteTask>(task, head, tail);
                    head = taskNode;
                    tail = taskNode;
                } else if (task.priorityCoefficient > 0) { //If the  task is already a not high priority task, we just need to insert it at the tail
                    insertAtTail(task);
                } else if (head.item.priorityCoefficient > 0) { //If the head task is already a not high priority task, we just need to insert at head
                    insertAtHead(task);
                } else {
                    if (tail.item.priorityCoefficient == 0) { //Saves us from going through the entire list if all of these tasks are priority coef == 0
                        insertAtTail(task);
                        return;
                    }
                    Node<PacketWriteTask> currentPlace = head;
                    while (true) {
                        if (currentPlace.item.priorityCoefficient == 0) {
                            if (currentPlace.next == null) {
                                //We've reached the end of the list
                                insertAtTail(task);
                                return;
                            } else {
                                currentPlace = currentPlace.next;
                            }
                        } else {
                            //We've found where this task should be inserted
                            Node<PacketWriteTask> previous = currentPlace.prev;
                            Node<PacketWriteTask> taskNode = new Node<PacketWriteTask>(task, previous, currentPlace);
                            previous.next = taskNode;
                            currentPlace.prev = taskNode;
                            return;

                        }
                    }
                }
            }
        }

        /**
         * Peek at the current head of the queue
         *
         * @return the task at the head of the queue but does not remove it from the queue
         */
        public PacketWriteTask peek() {
            synchronized (this) {
                if (head == null) {
                    return null;
                } else {
                    return head.item;
                }
            }
        }

        /**
         * Remove the head of the queue
         *
         * @return the old head of the queue
         */
        public PacketWriteTask poll() {
            synchronized (this) {
                if (head == null) {
                    return null;
                } else {
                    Node<PacketWriteTask> retValNode = head;
                    Node<PacketWriteTask> newHead = head.next;
                    if (newHead == null) {
                        tail = null;
                    }
                    head = newHead;

                    return retValNode.item;
                }
            }
        }

        /**
         * Currently only clears the head and the tail of the queue.
         */
        public void clear() {
            //Should probably go through the linked list and clear elements, but gc should clear them out automatically.
            head = null;
            tail = null;
        }
    }

    /**
     * notifySppError: utilize notification channel to notify the SPP out-of-resource error.
     */
    @TargetApi(11)
    @SuppressLint("NewApi")
    private void notifySppError() {
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(getApplicationContext());
        } else {
            builder = new Notification.Builder(getApplicationContext(), TransportConstants.SDL_ERROR_NOTIFICATION_CHANNEL_ID);
        }
        ComponentName name = new ComponentName(this, this.getClass());
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) { //If we are in debug mode, include what app has the router service open
            builder.setContentTitle("SDL: " + name.getPackageName());
        } else {
            builder.setContentTitle(getString(R.string.notification_title));
        }
        builder.setTicker(getString(R.string.sdl_error_notification_channel_name));
        builder.setContentText(getString(R.string.spp_out_of_resource));

        //We should use icon from library resources if available
        int trayId = getResources().getIdentifier("sdl_tray_icon", "drawable", getPackageName());

        builder.setSmallIcon(trayId);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.spp_error);
        builder.setLargeIcon(icon);

        builder.setOngoing(false);

        DebugTool.logError(TAG, "Notification: notifySppError entering");
        final String tag = "SDL";
        //Now we need to add a notification channel
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(tag, TransportConstants.SDL_ERROR_NOTIFICATION_CHANNEL_ID_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(TransportConstants.SDL_ERROR_NOTIFICATION_CHANNEL_ID, getString(R.string.sdl_error_notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.enableLights(true);
                notificationChannel.enableVibration(true);
                notificationChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(notificationChannel);
                builder.setChannelId(notificationChannel.getId());
            }
            Notification notification = builder.build();
            notificationManager.notify(tag, TransportConstants.SDL_ERROR_NOTIFICATION_CHANNEL_ID_INT, notification);
        } else {
            DebugTool.logError(TAG, "notifySppError: Unable to retrieve notification Manager service");
        }
    }
}
