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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;

import com.smartdevicelink.proxy.rpc.VehicleType;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Joey Grover on 2/2/18.
 */

public class SdlAppInfo {
    private static final String TAG = "SdlAppInfo";

    //FIXME we shouldn't be duplicating constants, but this currently keeps us from needing a context instance.
    private static final String SDL_ROUTER_VERSION_METADATA = "sdl_router_version";
    private static final String SDL_CUSTOM_ROUTER_METADATA = "sdl_custom_router";
    private static final String SDL_OEM_VEHICLE_TYPE_METADATA = "sdl_oem_vehicle_type";


    String packageName;
    ComponentName routerServiceComponentName;
    int routerServiceVersion = 4; //We use this as a default and assume if the number doesn't exist in meta data it is because the app hasn't updated.
    boolean isCustomRouterService = false;
    List<VehicleType> vehicleMakesList = new ArrayList<>();
    long lastUpdateTime;

    @Deprecated
    public SdlAppInfo(ResolveInfo resolveInfo, PackageInfo packageInfo) {
        if (resolveInfo.serviceInfo != null) {

            this.packageName = resolveInfo.serviceInfo.packageName;
            this.routerServiceComponentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);

            Bundle metadata = resolveInfo.serviceInfo.metaData;
            if (metadata != null) {

                if (metadata.containsKey(SDL_ROUTER_VERSION_METADATA)) {
                    this.routerServiceVersion = metadata.getInt(SDL_ROUTER_VERSION_METADATA);
                }

                if (metadata.containsKey(SDL_CUSTOM_ROUTER_METADATA)) {
                    this.isCustomRouterService = metadata.getBoolean(SDL_CUSTOM_ROUTER_METADATA);
                }
            } else {
                DebugTool.logWarning(TAG, packageName + " has not supplied metadata with their router service!");
            }
        }

        if (packageInfo != null) {
            this.lastUpdateTime = packageInfo.lastUpdateTime;
            if (this.lastUpdateTime <= 0) {
                this.lastUpdateTime = packageInfo.firstInstallTime;
            }
        } else {
            this.lastUpdateTime = 0;
        }
    }

    public SdlAppInfo(ResolveInfo resolveInfo, PackageInfo packageInfo, Context context) {
        if (resolveInfo.serviceInfo != null) {

            this.packageName = resolveInfo.serviceInfo.packageName;
            this.routerServiceComponentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);

            Bundle metadata = resolveInfo.serviceInfo.metaData;
            if (metadata != null) {

                if (metadata.containsKey(SDL_ROUTER_VERSION_METADATA)) {
                    this.routerServiceVersion = metadata.getInt(SDL_ROUTER_VERSION_METADATA);
                }

                if (metadata.containsKey(SDL_CUSTOM_ROUTER_METADATA)) {
                    this.isCustomRouterService = metadata.getBoolean(SDL_CUSTOM_ROUTER_METADATA);
                }

                if (metadata.containsKey(SDL_OEM_VEHICLE_TYPE_METADATA)) {
                    try {
                        XmlResourceParser parser;
                        if (!context.getPackageName().equals(packageName)) {
                            Context appContext = context.createPackageContext(packageName, 0);
                            parser = appContext.getResources().getXml(metadata.getInt(SDL_OEM_VEHICLE_TYPE_METADATA));
                        } else {
                            parser = context.getResources().getXml(metadata.getInt(SDL_OEM_VEHICLE_TYPE_METADATA));
                        }

                        this.vehicleMakesList = deserializeVehicleMake(parser);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                DebugTool.logWarning(TAG, packageName + " has not supplied metadata with their router service!");
            }
        }

        if (packageInfo != null) {
            this.lastUpdateTime = packageInfo.lastUpdateTime;
            if (this.lastUpdateTime <= 0) {
                this.lastUpdateTime = packageInfo.firstInstallTime;
            }
        } else {
            this.lastUpdateTime = 0;
        }
    }

    public int getRouterServiceVersion() {
        return routerServiceVersion;
    }

    public boolean isCustomRouterService() {
        return isCustomRouterService;
    }


    public ComponentName getRouterServiceComponentName() {
        return routerServiceComponentName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("-------- Sdl App Info ------\n");

        builder.append("Package Name: ");
        builder.append(packageName);

        builder.append("\nRouter Service: ");
        builder.append(this.routerServiceComponentName.getClassName());

        builder.append("\nRouter Service Version: ");
        builder.append(this.routerServiceVersion);

        builder.append("\nCustom Router Service?: ");
        builder.append(this.isCustomRouterService);

        builder.append("\nLast updated: ");
        builder.append(this.lastUpdateTime);

        builder.append("\nVehicle make list: ");
        builder.append(this.vehicleMakesList.toString());

        builder.append("\n-------- Sdl App Info End------");

        return builder.toString();
    }

    public static List<VehicleType> deserializeVehicleMake(XmlResourceParser parser) {
        List<VehicleType> vehicleMakesList = new ArrayList<VehicleType>();
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagname = parser.getName();
                    if (tagname.equalsIgnoreCase("vehicle-type")) {
                        VehicleType vehicleMake = new VehicleType();
                        String make = parser.getAttributeValue(null, "make");
                        if (make != null) {
                            vehicleMake.setMake(make);
                            String model = parser.getAttributeValue(null, "model");
                            if (model != null)
                                vehicleMake.setModel(model);
                            String modelYear = parser.getAttributeValue(null, "modelYear");
                            if (modelYear != null)
                                vehicleMake.setModelYear(modelYear);
                            String trim = parser.getAttributeValue(null, "trim");
                            if (trim != null)
                                vehicleMake.setTrim(trim);
                            vehicleMakesList.add(vehicleMake);
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vehicleMakesList;
    }

    public static boolean checkIfVehicleSupported(List<VehicleType> supportedVehicleList, VehicleType connectedVehicle) {
        if (supportedVehicleList == null || supportedVehicleList.isEmpty() || connectedVehicle == null || connectedVehicle.getStore().isEmpty()) {
            return true;
        }
        if (supportedVehicleList.contains(connectedVehicle)) {
            return true;
        }
        for (VehicleType supportedVehicle: supportedVehicleList) {
            String supportedVehicleMake = supportedVehicle.getMake();
            String connectedVehicleMake = connectedVehicle.getMake();
            if (supportedVehicleMake != null && connectedVehicleMake != null && connectedVehicleMake.equalsIgnoreCase(supportedVehicleMake)) {
                String supportedVehicleModel = supportedVehicle.getModel();
                String connectedVehicleModel = connectedVehicle.getModel();
                if (supportedVehicleModel != null && connectedVehicleModel != null) {
                    if (connectedVehicleModel.equalsIgnoreCase(supportedVehicleModel)) {
                        boolean ret = true;
                        String supportedVehicleModelYear = supportedVehicle.getModelYear();
                        String connectedVehicleModelYear = connectedVehicle.getModelYear();
                        if (supportedVehicleModelYear != null && connectedVehicleModelYear != null) {
                            ret = connectedVehicleModelYear.equalsIgnoreCase(supportedVehicleModelYear);
                        }
                        String supportedVehicleTrim = supportedVehicle.getTrim();
                        String connectedVehicleTrim = connectedVehicle.getTrim();
                        if (supportedVehicleTrim != null && connectedVehicleTrim != null) {
                            ret &= connectedVehicleTrim.equalsIgnoreCase(supportedVehicleTrim);
                        }
                        if (ret) {
                            return true;
                        }
                    }
                }
                else {
                    /* Return true if only make is defined and it matches*/
                    return true;
                }
            }
        }
        return false;
    }

    public List<VehicleType> getVehicleMakesList() {
        return vehicleMakesList;
    }

    /**
     * This comparator will sort a list to find the best router service to start out of the known SDL enabled apps
     */
    public static class BestRouterComparator implements Comparator<SdlAppInfo> {

        @Override
        public int compare(SdlAppInfo one, SdlAppInfo two) {
            if (one != null) {
                if (two != null) {
                    if (one.isCustomRouterService) {
                        if (two.isCustomRouterService) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else if (two.isCustomRouterService) {
                        return -1;

                    }//else, do nothing. Move to version check

                    int versionCompare = two.routerServiceVersion - one.routerServiceVersion;

                    if (versionCompare == 0) { //Versions are equal so lets use the one that has been updated most recently
                        long updateTime = two.lastUpdateTime - one.lastUpdateTime;
                        if (updateTime == 0) {
                            //This is arbitrary, but we want to ensure all lists are sorted in the same order
                            return one.routerServiceComponentName.getPackageName().compareTo(two.routerServiceComponentName.getPackageName());
                        } else {
                            return (updateTime < 0 ? -1 : 1);
                        }
                    } else {
                        return (versionCompare < 0 ? -1 : 1);
                    }

                } else {
                    return -1;
                }
            } else {
                if (two != null) {
                    return 1;
                }
            }
            return 0;
        }


    }
}
