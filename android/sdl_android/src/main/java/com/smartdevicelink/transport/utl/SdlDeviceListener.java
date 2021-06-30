/*
 * Copyright (c) 2020 Livio, Inc.
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


package com.smartdevicelink.transport.utl;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smartdevicelink.protocol.SdlPacket;
import com.smartdevicelink.protocol.SdlPacketFactory;
import com.smartdevicelink.protocol.enums.ControlFrameTags;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.transport.MultiplexBaseTransport;
import com.smartdevicelink.transport.MultiplexBluetoothTransport;
import com.smartdevicelink.transport.SdlRouterService;
import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.BitConverter;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.SdlAppInfo;
import com.smartdevicelink.util.Version;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.List;


public class SdlDeviceListener {

    private static final String TAG = "SdlListener";
    private static final int MIN_VERSION_REQUIRED = 13;
    private static final String SDL_DEVICE_STATUS_SHARED_PREFS = "sdl.device.status";
    private static final Object LOCK = new Object(), RUNNING_LOCK = new Object();

    private final WeakReference<Context> contextWeakReference;
    private final Callback callback;
    private BluetoothDevice connectedDevice;
    private MultiplexBluetoothTransport bluetoothTransport;
    private TransportHandler bluetoothHandler;
    private Handler timeoutHandler;
    private Runnable timeoutRunner;
    private boolean isRunning = false;


    public SdlDeviceListener(Context context, BluetoothDevice device, Callback callback) {
        this.contextWeakReference = new WeakReference<>(context);
        this.connectedDevice = device;
        this.callback = callback;
    }

    /**
     * This will start the SDL Device Listener with two paths. The first path will be a check
     * against the supplied bluetooth device to see if it has already successfully connected as an
     * SDL device. If it has, the supplied callback will be called immediately. If the device hasn't
     * connected as an SDL device before, the SDL Device Listener will then open up an RFCOMM channel
     * using the SDL UUID and await a potential connection. A timeout is used to ensure this only
     * listens for a finite amount of time. If this is the first time the device has been seen, this
     * will listen for 30 seconds, if it is not, this will listen for 15 seconds instead.
     */
    public void start() {
        if (connectedDevice == null) {
            DebugTool.logInfo(TAG, ": No supplied bluetooth device");
        } else if (hasSDLConnected(contextWeakReference.get(), connectedDevice.getAddress())) {
            DebugTool.logInfo(TAG, ": Confirmed SDL device, should start router service");
            //This device has connected to SDL previously, it is ok to start the RS right now
            VehicleType vehicleType = null;
            Hashtable<String, Object> store = AndroidTools.getVehicleTypeFromPrefs(contextWeakReference.get(), connectedDevice.getAddress());
            if (store != null) {
                vehicleType = new VehicleType(store);
            }
            callback.onTransportConnected(contextWeakReference.get(), connectedDevice, vehicleType);
            return;
        }

        synchronized (RUNNING_LOCK) {
            isRunning = true;
            // set timeout = if first time seeing BT device, 30s, if not 15s
            int timeout = connectedDevice != null && isFirstStatusCheck(connectedDevice.getAddress()) ? 30000 : 15000;
            //Set our preference as false for this device for now
            if(connectedDevice != null) {
                setSDLConnectedStatus(contextWeakReference.get(), connectedDevice.getAddress(), false);
            }
            bluetoothHandler = new TransportHandler(this);
            bluetoothTransport = new MultiplexBluetoothTransport(bluetoothHandler);
            bluetoothTransport.start();
            timeoutRunner = new Runnable() {
                @Override
                public void run() {
                    if (bluetoothTransport != null) {
                        int state = bluetoothTransport.getState();
                        if (state != MultiplexBluetoothTransport.STATE_CONNECTED) {
                            DebugTool.logInfo(TAG, ": No bluetooth connection made");
                            bluetoothTransport.stop();
                        } //else BT is connected; it will close itself through callbacks
                    }
                }
            };
            timeoutHandler = new Handler(Looper.getMainLooper());
            timeoutHandler.postDelayed(timeoutRunner, timeout);
        }
    }

    /**
     * Check to see if this instance is in the middle of running or not
     *
     * @return if this is already in the process of running
     */
    public boolean isRunning() {
        synchronized (RUNNING_LOCK) {
            return isRunning;
        }
    }

    private static class TransportHandler extends Handler {

        final WeakReference<SdlDeviceListener> provider;

        TransportHandler(SdlDeviceListener provider) {
            this.provider = new WeakReference<>(provider);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            SdlDeviceListener sdlListener = this.provider.get();
            switch (msg.what) {

                case SdlRouterService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case MultiplexBaseTransport.STATE_CONNECTED:
                            if (sdlListener.connectedDevice == null) {
                                sdlListener.connectedDevice = sdlListener.bluetoothTransport.getConnectedDevice();
                            }
                            sendStartService();
                            break;
                        case MultiplexBaseTransport.STATE_NONE:
                            // We've just lost the connection
                            sdlListener.callback.onTransportDisconnected(sdlListener.connectedDevice);
                            break;
                        case MultiplexBaseTransport.STATE_ERROR:
                            sdlListener.callback.onTransportError(sdlListener.connectedDevice);
                            break;
                    }
                    break;

                case com.smartdevicelink.transport.SdlRouterService.MESSAGE_READ:
                    onPacketRead((SdlPacket) msg.obj);
                    break;
            }
        }

        public void sendStartService() {
            SdlDeviceListener sdlListener = this.provider.get();
            SdlPacket serviceProbe = SdlPacketFactory.createStartSession(SessionType.RPC, 0x00, (byte)0x01, (byte)0x00, false);
            Version v = new Version(5, 4, 0);
            serviceProbe.putTag(ControlFrameTags.RPC.StartService.PROTOCOL_VERSION, v.toString());
            byte[] constructed = serviceProbe.constructPacket();
            if (sdlListener.bluetoothTransport != null && sdlListener.bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_CONNECTED) {
                sdlListener.bluetoothTransport.write(constructed, 0, constructed.length);
            }
        }

        public void onPacketRead(SdlPacket packet) {
            SdlDeviceListener sdlListener = this.provider.get();
            VehicleType vehicleType = null;
            if (packet.getFrameInfo() == SdlPacket.FRAME_INFO_START_SERVICE_ACK) {
                //parse vehicle Type info from connected system
                if (packet.getVersion() >= 5) {
                    vehicleType = getVehicleType(packet);
                }
                notifyConnection(vehicleType);
            }
            int hashID = BitConverter.intFromByteArray(packet.getPayload(), 0);
            byte[] stopService = SdlPacketFactory.createEndSession(SessionType.RPC, (byte)packet.getSessionId(), 0, (byte)packet.getVersion(), hashID).constructPacket();
            if (sdlListener.bluetoothTransport != null && sdlListener.bluetoothTransport.getState() == MultiplexBluetoothTransport.STATE_CONNECTED) {
                sdlListener.bluetoothTransport.write(stopService, 0, stopService.length);
            }
        }

        private VehicleType getVehicleType(SdlPacket packet) {
            String make = (String) packet.getTag(ControlFrameTags.RPC.StartServiceACK.MAKE);
            String model = (String) packet.getTag(ControlFrameTags.RPC.StartServiceACK.MODEL);
            String modelYear = (String) packet.getTag(ControlFrameTags.RPC.StartServiceACK.MODEL_YEAR);
            String vehicleTrim = (String) packet.getTag(ControlFrameTags.RPC.StartServiceACK.TRIM);
            if (make != null) {
                // checking if tags have come from core
                VehicleType type = new VehicleType();
                type.setMake(make);
                type.setModel(model);
                type.setModelYear(modelYear);
                type.setTrim(vehicleTrim);
                return type;
            } else {
                return null;
            }
        }

        public void notifyConnection(VehicleType vehicleType) {
            SdlDeviceListener sdlListener = this.provider.get();
            sdlListener.setSDLConnectedStatus(sdlListener.contextWeakReference.get(), sdlListener.connectedDevice.getAddress(), true);
            AndroidTools.saveVehicleType(sdlListener.contextWeakReference.get(), vehicleType, sdlListener.connectedDevice.getAddress());
            boolean keepConnectionOpen = sdlListener.callback.onTransportConnected(sdlListener.contextWeakReference.get(), sdlListener.connectedDevice, vehicleType);
            if (!keepConnectionOpen) {
                sdlListener.bluetoothTransport.stop();
                sdlListener.bluetoothTransport = null;
                sdlListener.timeoutHandler.removeCallbacks(sdlListener.timeoutRunner);
            }
        }
    }


    /**
     * Set the connection establishment status of the particular device
     *
     * @param address         address of the device in question
     * @param hasSDLConnected true if a connection has been established, false if not
     */
    public static void setSDLConnectedStatus(Context context, String address, boolean hasSDLConnected) {
        synchronized (LOCK) {
            if (context != null) {
                DebugTool.logInfo(TAG, ": Saving connected status - " + address + " : " + hasSDLConnected);
                SharedPreferences preferences = context.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
                if (preferences.contains(address) && hasSDLConnected == preferences.getBoolean(address, false)) {
                    //The same key/value exists in our shared preferences. No reason to write again.
                    return;
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(address, hasSDLConnected);
                editor.commit();
            }
        }
    }

    /**
     * Checks to see if a device address has connected to SDL before.
     *
     * @param address the mac address of the device in question
     * @return if this is the first status check of this device
     */
    private boolean isFirstStatusCheck(String address) {
        synchronized (LOCK) {
            Context context = contextWeakReference.get();
            if (context != null) {
                SharedPreferences preferences = context.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
                return !preferences.contains(address);
            }
            return false;
        }
    }

    /**
     * Checks to see if a device address has connected to SDL before.
     *
     * @param address the mac address of the device in question
     * @return if an SDL connection has ever been established with this device
     */
    public static boolean hasSDLConnected(Context context, String address) {
        synchronized (LOCK) {
            if (context != null) {
                SharedPreferences preferences = context.getSharedPreferences(SDL_DEVICE_STATUS_SHARED_PREFS, Context.MODE_PRIVATE);
                return preferences.contains(address) && preferences.getBoolean(address, false);
            }
            return false;
        }
    }

    /**
     * This method will check the current device and list of SDL enabled apps to derive if the
     * feature can be supported. Due to older libraries sending their intents to start the router
     * service right at the bluetooth A2DP/HFS connections, this feature can't be used until all
     * applications are updated to the point they include the feature.
     *
     * @param sdlAppInfoList current list of SDL enabled applications on the device
     * @return if this feature is supported or not. If it is not, the caller should follow the
     * previously used flow, ie start the router service.
     */
    public static boolean isFeatureSupported(List<SdlAppInfo> sdlAppInfoList) {

        SdlAppInfo appInfo;
        for (int i = sdlAppInfoList.size() - 1; i >= 0; i--) {
            appInfo = sdlAppInfoList.get(i);
            if (appInfo != null
                    && !appInfo.isCustomRouterService()
                    && appInfo.getRouterServiceVersion() < MIN_VERSION_REQUIRED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Callback for the SdlDeviceListener. It will return if the supplied device makes a bluetooth
     * connection on the SDL UUID RFCOMM chanel or not. Most of the time the only callback that
     * matters will be the onTransportConnected.
     */
    public interface Callback {
        /**
         * @param bluetoothDevice the BT device that successfully connected to SDL's UUID
         * @return if the RFCOMM connection should stay open. In most cases this should be false
         */
        boolean onTransportConnected(Context context, BluetoothDevice bluetoothDevice, VehicleType vehicleType);

        void onTransportDisconnected(BluetoothDevice bluetoothDevice);

        void onTransportError(BluetoothDevice bluetoothDevice);
    }
}
