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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AndroidRuntimeException;

import androidx.annotation.CallSuper;

import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.transport.RouterServiceValidator.TrustedListCallback;
import com.smartdevicelink.transport.enums.TransportType;
import com.smartdevicelink.transport.utl.SdlDeviceListener;
import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.IntegrationValidator;
import com.smartdevicelink.util.SdlAppInfo;
import com.smartdevicelink.util.ServiceFinder;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.smartdevicelink.transport.TransportConstants.FOREGROUND_EXTRA;

public abstract class SdlBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "Sdl Broadcast Receiver";

    protected static final String SDL_ROUTER_SERVICE_CLASS_NAME = "sdlrouterservice";

    public static final String LOCAL_ROUTER_SERVICE_EXTRA = "router_service";
    public static final String LOCAL_ROUTER_SERVICE_DID_START_OWN = "did_start";

    public static final String TRANSPORT_GLOBAL_PREFS = "SdlTransportPrefs";
    public static final String IS_TRANSPORT_CONNECTED = "isTransportConnected";

    public static Vector<ComponentName> runningBluetoothServicePackage = null;

    @SuppressWarnings("rawtypes")
    private static Class localRouterClass;

    private static final Object QUEUED_SERVICE_LOCK = new Object();
    private static ComponentName queuedService = null;
    private static Thread.UncaughtExceptionHandler foregroundExceptionHandler = null;
    private static final Object DEVICE_LISTENER_LOCK = new Object();
    private static SdlDeviceListener sdlDeviceListener;

    public int getRouterServiceVersion() {
        return SdlRouterService.ROUTER_SERVICE_VERSION_NUMBER;
    }

    @Override
    @CallSuper
    public void onReceive(Context context, Intent intent) {
        //Log.i(TAG, "Sdl Receiver Activated");
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        BluetoothDevice device = null;

        if (action.equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED)
                || action.equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED)) {
            //The package manager has sent out a new broadcast.
            RouterServiceValidator.invalidateList(context);
            return;
        }

        if (!(action.equalsIgnoreCase(BluetoothDevice.ACTION_ACL_CONNECTED)
                || action.equalsIgnoreCase(TransportConstants.ACTION_USB_ACCESSORY_ATTACHED)
                || action.equalsIgnoreCase(TransportConstants.START_ROUTER_SERVICE_ACTION))) {
            //We don't want anything else here if the child class called super and has different intent filters
            //Log.i(TAG, "Unwanted intent from child class");
            return;
        }

        if (action.equalsIgnoreCase(TransportConstants.ACTION_USB_ACCESSORY_ATTACHED)) {
            DebugTool.logInfo(TAG, "Usb connected");
            intent.setAction(null);
            onSdlEnabled(context, intent);
            return;
        }

        if (intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {    //Grab the bluetooth device if available
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }

        boolean didStart = false;
        if (localRouterClass == null) {
            localRouterClass = defineLocalSdlRouterClass();
            // we need to check this again because for USB apps, the returned class can still be null
            if (AndroidTools.isDebugMode(context)) {
                IntegrationValidator.ValidationResult result = IntegrationValidator.validate(context, localRouterClass, 0);
                if (!result.isSuccessful()) {
                    throw new RuntimeException(result.getResultText());
                }
            }
        }

        if (localRouterClass != null && localRouterClass.getName().equalsIgnoreCase(com.smartdevicelink.transport.SdlRouterService.class.getName())) {
            DebugTool.logError(TAG, "You cannot use the default SdlRouterService class, it must be extended in your project. THIS WILL THROW AN EXCEPTION IN FUTURE RELEASES!!");
        }

        HashMap<String, Object> vehicleInfoStore = (HashMap<String, Object>) intent.getSerializableExtra(TransportConstants.VEHICLE_INFO);

        VehicleType vehicleType;
        if (vehicleInfoStore == null || vehicleInfoStore.isEmpty()) {
            vehicleType = null;
        } else {
            vehicleType = new VehicleType(vehicleInfoStore);
        }

        //This will only be true if we are being told to reopen our SDL service because SDL is enabled
        if (action.equalsIgnoreCase(TransportConstants.START_ROUTER_SERVICE_ACTION)) {
            if (intent.hasExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_EXTRA)) {
                if (intent.getBooleanExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_EXTRA, false)) {
                    String packageName = intent.getStringExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_APP_PACKAGE);
                    final ComponentName componentName = intent.getParcelableExtra(TransportConstants.START_ROUTER_SERVICE_SDL_ENABLED_CMP_NAME);
                    if (componentName != null) {
                        final Intent finalIntent = intent;
                        final Context finalContext = context;
                        RouterServiceValidator.createTrustedListRequest(context, false, new TrustedListCallback() {
                            @Override
                            public void onListObtained(boolean successful) {
                                //Log.v(TAG, "SDL enabled by router service from " + packageName + " component package " + componentName.getPackageName()  + " - " + componentName.getClassName());
                                //List obtained. Let's start our service
                                queuedService = componentName;
                                finalIntent.setAction("com.sdl.noaction"); //Replace what's there so we do go into some unintended loop
                                String transportType = finalIntent.getStringExtra(TransportConstants.START_ROUTER_SERVICE_TRANSPORT_CONNECTED);
                                if (transportType != null) {
                                    if (TransportType.USB.toString().equals(transportType)) {
                                        finalIntent.putExtra(UsbManager.EXTRA_ACCESSORY, (Parcelable) null);
                                    }
                                }
                                onSdlEnabled(finalContext, finalIntent);
                            }

                        });
                    }

                }
                return;
            } else if (intent.getBooleanExtra(TransportConstants.PING_ROUTER_SERVICE_EXTRA, false)) {
                //We were told to wake up our router services
                boolean altServiceWake = intent.getBooleanExtra(TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT, false);
                didStart = wakeUpRouterService(context, false, altServiceWake, device, vehicleType);

            }
        }


        DebugTool.logInfo(TAG, "Check for local router");
        if (localRouterClass != null) { //If there is a supplied router service lets run some logic regarding starting one

            if (!didStart) {
                DebugTool.logInfo(TAG, "attempting to wake up router service");
                didStart = wakeUpRouterService(context, true, false, device, vehicleType);
            }

            //So even though we started our own version, on some older phones we find that two services are started up so we want to make sure we send our version that we are working with
            //We will send it an intent with the version number of the local instance and an intent to start this instance

            Intent serviceIntent = new Intent(context, localRouterClass);
            SdlRouterService.LocalRouterService self = SdlRouterService.getLocalRouterService(serviceIntent, serviceIntent.getComponent());
            Intent restart = new Intent(SdlRouterService.REGISTER_NEWER_SERVER_INSTANCE_ACTION);
            restart.putExtra(LOCAL_ROUTER_SERVICE_EXTRA, self);
            restart.putExtra(LOCAL_ROUTER_SERVICE_DID_START_OWN, didStart);
            context.sendBroadcast(restart);
        }
    }

    /**
     * This method will attempt to start the router service.
     *
     * @param context          to be used to start the service and send broadcasts
     * @param componentName    the router service that should be started
     * @param altTransportWake if the alt transport flag should be set. Only used in debug
     * @param device           the connected bluetooth device
     * @param confirmedDevice  if the device is confirmed
     * @param vehicleType      vehicle params retrieved from connected device
     */
    private static void startRouterService(Context context, ComponentName componentName, boolean altTransportWake, BluetoothDevice device, boolean confirmedDevice, VehicleType vehicleType) {
        if (componentName == null) {
            return;
        }

        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(componentName);

        if (altTransportWake) {
            serviceIntent.setAction(TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT);
        }

        if (device != null) {
            serviceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        }

        if (confirmedDevice) {
            serviceIntent.putExtra(TransportConstants.CONFIRMED_SDL_DEVICE, confirmedDevice);
        }

        if (vehicleType == null) {
            final String address = device != null ? device.getAddress() : null;
            Hashtable<String, Object> store = AndroidTools.getVehicleTypeFromPrefs(context, address);
            if (store != null) {
                vehicleType = new VehicleType(store);
            }
        }
        if (vehicleType != null) {
            serviceIntent.putExtra(TransportConstants.VEHICLE_INFO, vehicleType.getStore());
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(serviceIntent);
            } else {
                serviceIntent.putExtra(FOREGROUND_EXTRA, true);
                DebugTool.logInfo(TAG, "Attempting to startForegroundService - " + System.currentTimeMillis());
                setForegroundExceptionHandler(); //Prevent ANR in case the OS takes too long to start the service
                context.startForegroundService(serviceIntent);

            }
            //Make sure to send this out for old apps to close down
            SdlRouterService.LocalRouterService self = SdlRouterService.getLocalRouterService(serviceIntent, serviceIntent.getComponent());
            Intent restart = new Intent(SdlRouterService.REGISTER_NEWER_SERVER_INSTANCE_ACTION);
            restart.putExtra(LOCAL_ROUTER_SERVICE_EXTRA, self);
            restart.putExtra(LOCAL_ROUTER_SERVICE_DID_START_OWN, true);
            context.sendBroadcast(restart);

        } catch (SecurityException e) {
            DebugTool.logError(TAG, "Security exception, process is bad");
        }
    }

    private boolean wakeUpRouterService(final Context context, final boolean ping, final boolean altTransportWake, final BluetoothDevice device, final VehicleType vehicleType) {
        new ServiceFinder(context, context.getPackageName(), new ServiceFinder.ServiceFinderCallback() {
            @Override
            public void onComplete(Vector<ComponentName> routerServices) {
                runningBluetoothServicePackage = new Vector<>();
                runningBluetoothServicePackage.addAll(routerServices);
                if (runningBluetoothServicePackage.isEmpty()) {
                    //If there isn't a service running we should try to start one
                    //We will try to sort the SDL enabled apps and find the one that's been installed the longest
                    final List<SdlAppInfo> sdlAppInfoList = AndroidTools.querySdlAppInfo(context, new SdlAppInfo.BestRouterComparator(), vehicleType);
                    synchronized (DEVICE_LISTENER_LOCK) {
                        final boolean sdlDeviceListenerEnabled = SdlDeviceListener.isFeatureSupported(sdlAppInfoList);
                        if (sdlDeviceListenerEnabled) {
                            String myPackage = context.getPackageName();
                            String routerServicePackage = null;
                            if (sdlAppInfoList != null && !sdlAppInfoList.isEmpty() && sdlAppInfoList.get(0).getRouterServiceComponentName() != null) {
                                routerServicePackage = sdlAppInfoList.get(0).getRouterServiceComponentName().getPackageName();
                            }
                            DebugTool.logInfo(TAG, ": This app's package: " + myPackage);
                            DebugTool.logInfo(TAG, ": Router service app's package: " + routerServicePackage);
                            if (myPackage != null && myPackage.equalsIgnoreCase(routerServicePackage)) {
                                //If the device is not null the listener should start as well as the
                                //case where this app was installed after BT connected and is the
                                //only SDL app installed on the device. (Rare corner case)
                                if(device != null || sdlAppInfoList.size() == 1) {
                                    SdlDeviceListener sdlDeviceListener = getSdlDeviceListener(context, device);
                                    if (!sdlDeviceListener.isRunning()) {
                                        sdlDeviceListener.start();
                                    }
                                } else {
                                    DebugTool.logInfo(TAG, "Not starting device listener, bluetooth device is null and other SDL apps installed.");
                                }
                            } else {
                                DebugTool.logInfo(TAG, ": Not the app to start the router service nor device listener");
                            }
                            return;
                        }
                    }

                    if (sdlAppInfoList != null && !sdlAppInfoList.isEmpty()) {
                        startRouterService(context, sdlAppInfoList.get(0).getRouterServiceComponentName(), altTransportWake, device, false, vehicleType);
                    } else {
                        DebugTool.logInfo(TAG, "No SDL Router Services found");
                        DebugTool.logInfo(TAG, "WARNING: This application has not specified its SdlRouterService correctly in the manifest. THIS WILL THROW AN EXCEPTION IN FUTURE RELEASES!!");
                        return;
                    }

                } else { //There are currently running services
                    if (DebugTool.isDebugEnabled()) {
                        for (ComponentName service : runningBluetoothServicePackage) {
                            DebugTool.logInfo(TAG, "Currently running router service: " + service.getPackageName());
                        }
                    }
                    if (altTransportWake) {
                        wakeRouterServiceAltTransport(context);
                        return;
                    } else {
                        for (ComponentName service : runningBluetoothServicePackage) {
                            pingRouterService(context, service.getPackageName(), service.getClassName());
                        }
                    }
                    return;
                }
            }
        });
        return true;
    }

    private void wakeRouterServiceAltTransport(Context context) {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT);
        for (ComponentName compName : runningBluetoothServicePackage) {
            serviceIntent.setComponent(compName);
            try {
                context.startService(serviceIntent);
            } catch (Exception e) {
                DebugTool.logError(TAG, "Can't start router service for alt transport");
            }

        }
    }

    /**
     * This method will set a new UncaughtExceptionHandler for the current thread. The only
     * purpose of the custom UncaughtExceptionHandler is to catch the rare occurrence that the
     * SdlRouterService can't be started fast enough by the system after calling
     * startForegroundService so the onCreate method doesn't get called before the foreground promise
     * timer expires. The new UncaughtExceptionHandler will catch that specific exception and tell the
     * main looper to continue forward. This still leaves the SdlRouterService killed, but prevents
     * an ANR to the app that makes the startForegroundService call.
     */
    static protected void setForegroundExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (defaultUncaughtExceptionHandler != foregroundExceptionHandler) {
            foregroundExceptionHandler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (e != null
                            && e instanceof AndroidRuntimeException
                            && "android.app.RemoteServiceException".equals(e.getClass().getName())  //android.app.RemoteServiceException is a private class
                            && e.getMessage() != null
                            && e.getMessage().contains("SdlRouterService")) {

                        DebugTool.logInfo(TAG, "Handling failed startForegroundService call");
                        Looper.loop();
                    } else if (defaultUncaughtExceptionHandler != null) { //No other exception should be handled
                        defaultUncaughtExceptionHandler.uncaughtException(t, e);
                    }
                }
            };
            Thread.setDefaultUncaughtExceptionHandler(foregroundExceptionHandler);
        }
    }

    /**
     * Determines if an instance of the Router Service is currently running on the device.<p>
     * <b>Note:</b> This method no longer works on Android Oreo or newer
     *
     * @param context A context to access Android system services through.
     * @return True if a SDL Router Service is currently running, false otherwise.
     */
    private static boolean isRouterServiceRunning(Context context) {
        if (context == null) {
            DebugTool.logError(TAG, "Can't look for router service, context supplied was null");
            return false;
        }
        if (runningBluetoothServicePackage == null) {
            runningBluetoothServicePackage = new Vector<>();
        } else {
            runningBluetoothServicePackage.clear();
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            manager.getRunningAppProcesses();
        }
        List<RunningServiceInfo> runningServices;
        try {
            runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        } catch (NullPointerException e) {
            DebugTool.logError(TAG, "Can't get list of running services");
            return false;
        }
        for (RunningServiceInfo service : runningServices) {
            //We will check to see if it contains this name, should be pretty specific
            //Log.d(TAG, "Found Service: "+ service.service.getClassName());
            if ((service.service.getClassName()).toLowerCase(Locale.US).contains(SDL_ROUTER_SERVICE_CLASS_NAME) && AndroidTools.isServiceExported(context, service.service)) {
                runningBluetoothServicePackage.add(service.service);    //Store which instance is running
            }
        }
        return runningBluetoothServicePackage.size() > 0;

    }

    /**
     * Attempts to ping a running router service. It does call startForegroundService so it is
     * important to only call this as a ping if the service is already started.
     *
     * @param context     A context to access Android system services through.
     * @param packageName Package name for service to ping
     * @param className   Class name for service to ping
     */
    protected static void pingRouterService(Context context, String packageName, String className) {
        if (context == null || packageName == null || className == null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClassName(packageName, className);
            intent.putExtra(TransportConstants.PING_ROUTER_SERVICE_EXTRA, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(FOREGROUND_EXTRA, true);
                DebugTool.logInfo(TAG, "Attempting to startForegroundService - " + System.currentTimeMillis());
                setForegroundExceptionHandler(); //Prevent ANR in case the OS takes too long to start the service
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (SecurityException e) {
            DebugTool.logError(TAG, "Security exception, process is bad");
            // This service could not be started
        }
    }

    /**
     * This call will reach out to all SDL related router services to check if they're connected. If a the router service is connected, it will react by pinging all clients. This receiver will then
     * receive that ping and if the router service is trusted, the onSdlEnabled method will be called.
     *
     * @param context
     */
    public static void queryForConnectedService(final Context context) {
        if (AndroidTools.isDebugMode(context)) {
            int flag = 0;
            if (localRouterClass == null) {
                flag = IntegrationValidator.FLAG_SKIP_ROUTER_SERVICE_CHECK;
            }

            IntegrationValidator.ValidationResult result = IntegrationValidator.validate(context, localRouterClass, flag);
            if (!result.isSuccessful()) {
                throw new RuntimeException(result.getResultText());
            }
        }
        //Leverage existing call. Include ping bit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceFinder finder = new ServiceFinder(context, context.getPackageName(), new ServiceFinder.ServiceFinderCallback() {
                @Override
                public void onComplete(Vector<ComponentName> routerServices) {
                    runningBluetoothServicePackage = new Vector<>();
                    runningBluetoothServicePackage.addAll(routerServices);
                    requestTransportStatus(context, null, true, false);
                }
            });

        } else {
            requestTransportStatus(context, null, true, true);
        }
    }

    /**
     * If a Router Service is running, this method determines if that service is connected to a device over some form of transport.
     *
     * @param context  A context to access Android system services through. If null is passed, this will always return false
     * @param callback Use this callback to find out if the router service is connected or not.
     */
    private static void requestTransportStatus(Context context, final SdlRouterStatusProvider.ConnectedStatusCallback callback, final boolean triggerRouterServicePing, final boolean lookForServices) {
        if (context == null) {
            if (callback != null) {
                callback.onConnectionStatusUpdate(false, null, context);
            }
            return;
        }
        if ((!lookForServices || isRouterServiceRunning(context)) && !runningBluetoothServicePackage.isEmpty()) {    //So there is a service up, let's see if it's connected
            final ConcurrentLinkedQueue<ComponentName> list = new ConcurrentLinkedQueue<>(runningBluetoothServicePackage);
            final SdlRouterStatusProvider.ConnectedStatusCallback sdlBrCallback = new SdlRouterStatusProvider.ConnectedStatusCallback() {

                @Override
                public void onConnectionStatusUpdate(boolean connected, ComponentName service, Context context) {
                    if (!connected && !list.isEmpty()) {
                        SdlRouterStatusProvider provider = new SdlRouterStatusProvider(context, list.poll(), this);
                        if (triggerRouterServicePing) {
                            provider.setFlags(TransportConstants.ROUTER_STATUS_FLAG_TRIGGER_PING);
                        }
                        provider.checkIsConnected();
                    } else {
                        if (service != null) {
                            DebugTool.logInfo(TAG, service.getPackageName() + " is connected = " + connected);
                        } else {
                            DebugTool.logInfo(TAG, "No service is connected/running");
                        }
                        if (callback != null) {
                            callback.onConnectionStatusUpdate(connected, service, context);
                        }
                        list.clear();
                    }

                }
            };
            final SdlRouterStatusProvider provider = new SdlRouterStatusProvider(context, list.poll(), sdlBrCallback);
            if (triggerRouterServicePing) {
                provider.setFlags(TransportConstants.ROUTER_STATUS_FLAG_TRIGGER_PING);
            }
            //Lets ensure we have a current list of trusted router services
            RouterServiceValidator.createTrustedListRequest(context, false, new TrustedListCallback() {
                @Override
                public void onListObtained(boolean successful) {
                    //This will kick off our check of router services
                    provider.checkIsConnected();
                }
            });

        } else {
            DebugTool.logWarning(TAG, "Router service isn't running, returning false.");
            if (isBluetoothConnected()) {
                DebugTool.logInfo(TAG, "Bluetooth is connected. Attempting to ping Router Service");
                Intent serviceIntent = new Intent();
                serviceIntent.setAction(TransportConstants.START_ROUTER_SERVICE_ACTION);
                serviceIntent.putExtra(TransportConstants.PING_ROUTER_SERVICE_EXTRA, true);
                AndroidTools.sendExplicitBroadcast(context, serviceIntent, null);
            }

            if (callback != null) {
                callback.onConnectionStatusUpdate(false, null, context);
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private static boolean isBluetoothConnected() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            int a2dpState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
            int headSetState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);

            return ((a2dpState == BluetoothAdapter.STATE_CONNECTED || a2dpState == BluetoothAdapter.STATE_CONNECTING)
                    && (headSetState == BluetoothAdapter.STATE_CONNECTED || headSetState == BluetoothAdapter.STATE_CONNECTING));
        }
        return false;
    }


    private static SdlDeviceListener getSdlDeviceListener(Context context, BluetoothDevice bluetoothDevice) {

        synchronized (DEVICE_LISTENER_LOCK) {
            if (sdlDeviceListener == null) {
                sdlDeviceListener = new SdlDeviceListener(context, bluetoothDevice, new SdlDeviceListener.Callback() {
                    @Override
                    public boolean onTransportConnected(Context context, BluetoothDevice bluetoothDevice, VehicleType vehicleType) {

                        synchronized (DEVICE_LISTENER_LOCK) {
                            sdlDeviceListener = null;
                            if (context != null) {
                                final List<SdlAppInfo> sdlAppInfoList = AndroidTools.querySdlAppInfo(context, new SdlAppInfo.BestRouterComparator(), vehicleType);
                                if (sdlAppInfoList != null && !sdlAppInfoList.isEmpty()) {
                                    ComponentName routerService = sdlAppInfoList.get(0).getRouterServiceComponentName();
                                    startRouterService(context, routerService, false, bluetoothDevice, true, vehicleType);
                                }
                            }
                        }

                        return false;
                    }

                    @Override
                    public void onTransportDisconnected(BluetoothDevice bluetoothDevice) {
                        synchronized (DEVICE_LISTENER_LOCK) {
                            sdlDeviceListener = null;
                        }
                    }

                    @Override
                    public void onTransportError(BluetoothDevice bluetoothDevice) {
                        synchronized (DEVICE_LISTENER_LOCK) {
                            sdlDeviceListener = null;
                        }
                    }
                });
            }
        }

        return sdlDeviceListener;
    }

    public static ComponentName consumeQueuedRouterService() {
        synchronized (QUEUED_SERVICE_LOCK) {
            ComponentName retVal = queuedService;
            queuedService = null;
            return retVal;
        }
    }

    /**
     * We need to define this for local copy of the Sdl Router Service class.
     * It will be the main point of connection for Sdl enabled apps
     *
     * @return Return the local copy of SdlRouterService.class
     * {@inheritDoc}
     */
    public abstract Class<? extends SdlRouterService> defineLocalSdlRouterClass();


    /**
     * The developer will need to define exactly what should happen when Sdl is enabled.
     * This method will only get called when a Sdl  session is initiated.
     * <p> The most useful code here would be to start the activity or service that handles most of the Livio
     * Connect code.
     *
     * @param context this is the context that was passed to this receiver when it was called.
     * @param intent  this is the intent that alerted this broadcast. Make sure to pass all extra it came with to your service/activity
     *                {@inheritDoc}
     */
    public abstract void onSdlEnabled(Context context, Intent intent);

    //public abstract void onSdlDisabled(Context context); //Removing for now until we're able to abstract from developer


}
