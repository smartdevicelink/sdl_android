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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.DebugTool;
import com.smartdevicelink.util.SdlAppInfo;
import com.smartdevicelink.util.ServiceFinder;

import java.util.List;
import java.util.Vector;

import static com.smartdevicelink.transport.TransportConstants.FOREGROUND_EXTRA;

/**
 * The USBAccessoryAttachmentActivity is a proxy to listen for
 * USB_ACCESSORY_ATTACHED intents.
 * <br><br>
 * Unfortunately, the USB_ACCESSORY_ATTACHED intent can only be sent to an
 * activity. So this class is a workaround to get that intent.
 * <br><br>
 * Some reference: http://stackoverflow.com/questions/6981736/android-3-1-usb-host-broadcastreceiver-does-not-receive-usb-device-attached/9814826#9814826
 * <br><br>
 * Inspired by OpenXC-Android: https://github.com/openxc/openxc-android
 * <br><br>
 * <strong>NOTE:</strong> An application that wants to use USB transport
 * must make the following changes to AndroidManifest.xml:
 * <br><br>
 * <b>1.</b> Add these lines to the {@literal <manifest>…</manifest>} scope:<br>
 * <pre>{@code
 * <!-- Required to use the USB Accessory mode -->
 * <uses-feature android:name="android.hardware.usb.accessory"/>
 * }</pre>
 * <b>2.</b> Add these lines to the {@literal <application>…</application>} scope:
 * <pre>{@code <activity android:name="com.smartdevicelink.transport.USBAccessoryAttachmentActivity">
 *     <intent-filter>
 *         <action android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"/>
 *     </intent-filter>
 *     <meta-data
 *         android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"
 *         android:resource="@xml/accessory_filter"/>
 * </activity>
 * }</pre>
 */
public class USBAccessoryAttachmentActivity extends Activity {

    private static final String TAG = USBAccessoryAttachmentActivity.class.getSimpleName();
    private static final int USB_SUPPORTED_ROUTER_SERVICE_VERSION = 8;

    UsbAccessory usbAccessory;
    Parcelable permissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUsbAccessoryIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    private synchronized void checkUsbAccessoryIntent() {
        if (usbAccessory != null) {
            return;
        }
        final Intent intent = getIntent();
        String action = intent.getAction();
        DebugTool.logInfo(TAG, "Received intent with action: " + action);

        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            permissionGranted = intent.getParcelableExtra(UsbManager.EXTRA_PERMISSION_GRANTED);

            wakeUpRouterService(getApplicationContext());

        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        usbAccessory = null;
        permissionGranted = null;
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void wakeUpRouterService(final Context context) {
        new ServiceFinder(context, context.getPackageName(), new ServiceFinder.ServiceFinderCallback() {
            @Override
            public void onComplete(Vector<ComponentName> routerServices) {
                Vector<ComponentName> runningBluetoothServicePackage = new Vector<>(routerServices);
                if (runningBluetoothServicePackage.isEmpty()) {
                    //If there isn't a service running we should try to start one
                    //We will try to sort the SDL enabled apps and find the one that's been installed the longest
                    Intent serviceIntent;
                    List<SdlAppInfo> sdlAppInfoList = AndroidTools.querySdlAppInfo(context, new SdlAppInfo.BestRouterComparator(), null);

                    if (sdlAppInfoList != null && !sdlAppInfoList.isEmpty()) {
                        SdlAppInfo optimalRouterService = sdlAppInfoList.get(0);

                        if (optimalRouterService.getRouterServiceVersion() < USB_SUPPORTED_ROUTER_SERVICE_VERSION) {
                            // The most optimal router service doesn't support the USB connection
                            // At this point to ensure that USB connection is still possible it might be
                            // worth trying to use the legacy USB transport scheme
                            attemptLegacyUsbConnection(usbAccessory);
                            return;
                        }

                        serviceIntent = new Intent();
                        serviceIntent.setComponent(optimalRouterService.getRouterServiceComponentName());
                    } else {
                        DebugTool.logInfo(TAG, "No SDL Router Services found");
                        DebugTool.logInfo(TAG, "WARNING: This application has not specified its SdlRouterService correctly in the manifest. THIS WILL THROW AN EXCEPTION IN FUTURE RELEASES!!");
                        // At this point to ensure that USB connection is still possible it might be
                        // worth trying to use the legacy USB transport scheme
                        attemptLegacyUsbConnection(usbAccessory);
                        return;
                    }
                    serviceIntent.setAction(TransportConstants.BIND_REQUEST_TYPE_ALT_TRANSPORT);

                    ComponentName startedService;
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            startedService = context.startService(serviceIntent);
                        } else {
                            serviceIntent.putExtra(FOREGROUND_EXTRA, true);
                            startedService = context.startForegroundService(serviceIntent);
                        }

                        if (startedService == null) {
                            // A router service was not started or is not running.
                            DebugTool.logError(TAG, " - Error starting router service. Attempting legacy connection ");
                            attemptLegacyUsbConnection(usbAccessory);
                            return;
                        }

                        //Make sure to send this out for old apps to close down
                        SdlRouterService.LocalRouterService self = SdlRouterService.getLocalRouterService(serviceIntent, serviceIntent.getComponent());
                        Intent restart = new Intent(SdlRouterService.REGISTER_NEWER_SERVER_INSTANCE_ACTION);
                        restart.putExtra(SdlBroadcastReceiver.LOCAL_ROUTER_SERVICE_EXTRA, self);
                        restart.putExtra(SdlBroadcastReceiver.LOCAL_ROUTER_SERVICE_DID_START_OWN, true);
                        context.sendBroadcast(restart);

                        if (usbAccessory != null) {
                            new UsbTransferProvider(context, serviceIntent.getComponent(), usbAccessory, new UsbTransferProvider.UsbTransferCallback() {
                                @Override
                                public void onUsbTransferUpdate(boolean success) {
                                    finish();
                                }
                            });

                        }

                    } catch (SecurityException e) {
                        DebugTool.logError(TAG, "Security exception, process is bad");
                    }
                } else {
                    if (usbAccessory != null) {
                        new UsbTransferProvider(context, runningBluetoothServicePackage.get(0), usbAccessory, new UsbTransferProvider.UsbTransferCallback() {
                            @Override
                            public void onUsbTransferUpdate(boolean success) {
                                finish();
                            }
                        });

                    }
                }
            }
        });
    }

    private void attemptLegacyUsbConnection(UsbAccessory usbAccessory) {
        if (usbAccessory != null) {
            DebugTool.logInfo(TAG, "Attempting to send USB connection intent using legacy method");
            Intent usbAccessoryAttachedIntent = new Intent(TransportConstants.ACTION_USB_ACCESSORY_ATTACHED);
            usbAccessoryAttachedIntent.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory);
            usbAccessoryAttachedIntent.putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, permissionGranted);
            AndroidTools.sendExplicitBroadcast(getApplicationContext(), usbAccessoryAttachedIntent, null);
        } else {
            DebugTool.logError(TAG, "Unable to start legacy USB mode as the accessory was null");
        }
        finish();
    }
}
