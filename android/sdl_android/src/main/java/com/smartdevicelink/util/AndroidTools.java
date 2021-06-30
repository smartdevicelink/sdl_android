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

package com.smartdevicelink.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.smartdevicelink.marshal.JsonRPCMarshaller;
import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.transport.TransportConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


public class AndroidTools {

    private static final String SDL_DEVICE_VEHICLES_PREFS = "sdl.device.vehicles";
    private static final String TAG = "VehicleTypeHolder";
    private static final Object LOCK = new Object();

    /**
     * Check to see if a component is exported
     *
     * @param context object used to retrieve the package manager
     * @param name    of the component in question
     * @return true if this component is tagged as exported
     */
    public static boolean isServiceExported(Context context, ComponentName name) {
        try {
            ServiceInfo serviceInfo = context.getPackageManager().getServiceInfo(name, PackageManager.GET_META_DATA);
            return serviceInfo.exported;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all SDL enabled apps. If the package name is null, it will return all apps. However, if the package name is included, the
     * resulting hash map will not include the app with that package name.
     *
     * @param context       a context object used to get an instance of the package manager
     * @param myPackageName the package of the requesting app. This should only be included if the app wants to exclude itself from the map
     * @return a hash map of SDL apps with the package name as the key, and the ResolveInfo as the value
     */
    public static HashMap<String, ResolveInfo> getSdlEnabledApps(Context context, String myPackageName) {
        Intent intent = new Intent(TransportConstants.START_ROUTER_SERVICE_ACTION);
        List<ResolveInfo> infos = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        HashMap<String, ResolveInfo> sdlMultiList = new HashMap<>();
        for (ResolveInfo info : infos) {
            if (info.activityInfo.applicationInfo.packageName.equals(myPackageName)) {
                continue; //Ignoring my own package
            }
            sdlMultiList.put(info.activityInfo.packageName, info);
        }
        return sdlMultiList;
    }

    /**
     * Finds all SDL apps via their SdlRouterService manifest entry. It will return the metadata associated with that router service.
     *
     * @param context    a context instance to obtain the package manager
     * @param comparator the Comparator to sort the resulting list. If null is supplied, they will be returned as they are from the system
     * @return the sorted list of SdlAppInfo objects that represent SDL apps
     */
    @Deprecated
    public static List<SdlAppInfo> querySdlAppInfo(Context context, Comparator<SdlAppInfo> comparator) {
        List<SdlAppInfo> sdlAppInfoList = new ArrayList<>();
        Intent intent = new Intent(TransportConstants.ROUTER_SERVICE_ACTION);
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentServices(intent, PackageManager.GET_META_DATA);

        if (resolveInfoList != null && resolveInfoList.size() > 0) {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {

                for (ResolveInfo info : resolveInfoList) {
                    PackageInfo packageInfo;
                    try {
                        packageInfo = packageManager.getPackageInfo(info.serviceInfo.packageName, 0);
                        sdlAppInfoList.add(new SdlAppInfo(info, packageInfo, context));
                    } catch (NameNotFoundException e) {
                        //Package was not found, likely a sign the resolve info can't be trusted.
                    }

                }
            }

            if (comparator != null) {
                Collections.sort(sdlAppInfoList, comparator);
            }
        }

        return sdlAppInfoList;
    }

    /**
     * Finds all SDL apps via their SdlRouterService manifest entry. It will return the metadata associated with that router service.
     *
     * @param context    a context instance to obtain the package manager
     * @param comparator the Comparator to sort the resulting list. If null is supplied, they will be returned as they are from the system
     * @return the sorted list of SdlAppInfo objects that represent SDL apps
     */
    public static List<SdlAppInfo> querySdlAppInfo(Context context, Comparator<SdlAppInfo> comparator, VehicleType type) {
        List<SdlAppInfo> sdlAppInfoList = new ArrayList<>();
        Intent intent = new Intent(TransportConstants.ROUTER_SERVICE_ACTION);
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentServices(intent, PackageManager.GET_META_DATA);

        if (resolveInfoList != null && resolveInfoList.size() > 0) {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {

                for (ResolveInfo info : resolveInfoList) {
                    PackageInfo packageInfo;
                    try {
                        packageInfo = packageManager.getPackageInfo(info.serviceInfo.packageName, 0);
                        SdlAppInfo appInformation = new SdlAppInfo(info, packageInfo, context);
                        sdlAppInfoList.add(appInformation);

                    } catch (NameNotFoundException e) {
                        //Package was not found, likely a sign the resolve info can't be trusted.
                    }

                }
            }

            List<SdlAppInfo> sdlAppInfoListVehicleType = new ArrayList<>();

            for (SdlAppInfo appInformation : sdlAppInfoList) {
                if (appInformation.routerServiceVersion < 14) {
                    sdlAppInfoListVehicleType.add(appInformation);
                } else if (SdlAppInfo.checkIfVehicleSupported(appInformation.vehicleMakesList, type)) {
                    sdlAppInfoListVehicleType.add(appInformation);
                }
            }
            sdlAppInfoList = sdlAppInfoListVehicleType;

            if (comparator != null) {
                Collections.sort(sdlAppInfoList, comparator);
            }
        }
        return sdlAppInfoList;
    }


    /**
     * Sends the provided intent to the specified destinations making it an explicit intent, rather
     * than an implicit intent. A direct replacement of sendBroadcast(Intent). As of Android 8.0
     * (API 26+) implicit broadcasts are no longer sent to broadcast receivers that are declared via
     * the AndroidManifest. If no apps are found to receive the intent, this method will send the
     * broadcast implicitly if no list of apps is provided.
     *
     * @param intent - the intent to send explicitly
     * @param apps   - the list of apps that this broadcast will be sent to. If null is passed in
     *               the intent will be sent to all apps that match the provided intent via a query
     *               to the package manager; it will also be sent implicitly to mimic
     *               sendBroadcast()'s original functionality.
     */
    public static void sendExplicitBroadcast(Context context, Intent intent, List<ResolveInfo> apps) {

        if (context == null || intent == null) {
            return;
        }

        if (apps == null) {
            apps = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        }

        if (apps != null && apps.size() > 0) {
            for (ResolveInfo app : apps) {
                try {
                    intent.setClassName(app.activityInfo.applicationInfo.packageName, app.activityInfo.name);
                    context.sendBroadcast(intent);
                } catch (Exception e) {
                    //In case there is missing info in the app reference we want to keep moving
                }
            }
        } else {
            // fallback to implicit broadcast if we cannot resolve apps info.
            context.sendBroadcast(intent);
        }
    }

    /**
     * Checks if the usb cable is physically connected or not
     * Note: the intent here is a sticky intent so registerReceiver is actually a synchronous call and doesn't register a receiver on each call
     *
     * @param context a context instance
     * @return boolean value that represents whether the usb cable is physically connected or not
     */
    public static boolean isUSBCableConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return false;
        }
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    public static Bitmap downloadImage(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
        Bitmap result = BitmapFactory.decodeStream(bis);
        bis.close();
        return result;
    }

    public static boolean isDebugMode(Context context) {
        if (context != null && context.getApplicationInfo() != null) {
            return 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
        }
        return false;
    }

    public static void saveVehicleType(Context context, VehicleType vehicleType, String address) {
        synchronized (LOCK) {

            if (vehicleType == null || address == null) {
                return;
            }
            try {
                SharedPreferences preferences = context.getSharedPreferences(SDL_DEVICE_VEHICLES_PREFS, Context.MODE_PRIVATE);

                String jsonString = vehicleType.serializeJSON().toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(address, jsonString);
                editor.commit();
            } catch (JSONException e) {
                android.util.Log.e(TAG, e.getMessage());
            }
        }
    }

    public static Hashtable<String, Object> getVehicleTypeFromPrefs(Context context, String address) {
        synchronized (LOCK) {
            try {
                SharedPreferences preferences = context.getSharedPreferences(SDL_DEVICE_VEHICLES_PREFS, Context.MODE_PRIVATE);
                String storedVehicleTypeSerialized = preferences.getString(address, null);

                if (storedVehicleTypeSerialized == null) {
                    return null;
                } else {
                    JSONObject object = new JSONObject(storedVehicleTypeSerialized);
                    return JsonRPCMarshaller.deserializeJSONObject(object);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }
    }

    public static List<VehicleType> getVehicleTypesFromManifest(Context context, Class<?> className, int manifestFieldId) {
        XmlResourceParser parser = null;
        try {
            ComponentName myService = new ComponentName(context, className);
            Bundle metaData = context.getPackageManager().getServiceInfo(myService, PackageManager.GET_META_DATA).metaData;
            int xmlFieldId = metaData.getInt(context.getResources().getString(manifestFieldId));

            if (xmlFieldId == 0) {
                Log.e(TAG, "Field with id " + manifestFieldId + " was not found in manifest");
                return null;
            }

            parser = context.getResources().getXml(xmlFieldId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get OEM vehicle data filter: " + e.getMessage()+ " - assume vehicle data is supported");
            return null;
        }

        return SdlAppInfo.deserializeVehicleMake(parser);
    }

    public static boolean isSupportableVehicleType(List<VehicleType> supportedList, VehicleType typeToCheck) {
        if (!SdlAppInfo.checkIfVehicleSupported(supportedList, typeToCheck)) {
            DebugTool.logError(TAG, "Vehicle type is NOT supportable by current package");
            DebugTool.logError(TAG, "Received VD: " + typeToCheck.getStore().toString());

            StringBuilder builder = new StringBuilder();
            builder.append("Supportable VD: ");
            for (VehicleType vtype : supportedList) {
                builder.append(vtype.getStore().toString());
                builder.append("; ");
            }

            DebugTool.logError(TAG, builder.toString());
            return false;
        }

        DebugTool.logInfo(TAG, "Vehicle type is supportable by current package");
        return true;
    }
}
