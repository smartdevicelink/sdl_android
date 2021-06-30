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

package com.smartdevicelink.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.smartdevicelink.R;
import com.smartdevicelink.transport.TransportConstants;

import java.util.ArrayList;
import java.util.List;

public class IntegrationValidator {

    private static final char CHECK_MARK = 0x2713;
    private static final char FAIL_MARK = 0x2715;

    public static final int FLAG_SKIP_ROUTER_SERVICE_CHECK = 0x01;


    public static ValidationResult validate(Context context, Class localRouterClass, int flags) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n-----------------------------------");
        builder.append("\n  Integration Validator Results: \n");
        builder.append("-----------------------------------\n");
        List<ValidationResult> results = new ArrayList<>();

        ValidationResult permissionResults = checkPermissions(context);
        results.add(permissionResults);

        if ((flags & FLAG_SKIP_ROUTER_SERVICE_CHECK) == FLAG_SKIP_ROUTER_SERVICE_CHECK) {
            results.add(new ValidationResult(true, "SdlRouterService checks were not performed yet due to supplied flags"));
        } else {
            if (localRouterClass != null) {
                results.add(checkRoutServiceMetadata(context, localRouterClass));
                results.add(checkRouterServiceIntent(context, localRouterClass));
            } else {
                results.add(new ValidationResult(false, "SdlRouterService is not defined in SdlBroadcastReceiver and therefore some checks were not completed"));
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q || permissionResults.successful) {
            //This is done so that we don't provide incorrect information regarding Android 11
            //and the required new permission that causes the broadcast receiver check to fail.
            results.add(checkBroadcastReceiver(context));
        } else {
            results.add(new ValidationResult(false, "SdlBroadcastReceiver checks were not performed yet due to failing permission check"));
        }

        boolean success = true;
        for (ValidationResult result : results) {
            if (result.successful) {
                builder.append(CHECK_MARK + " ");
            } else {
                success = false;
                builder.append(FAIL_MARK + " ");
            }
            builder.append(result.resultText);
            builder.append("\n\n");


        }

        if (!success) {
            builder.append("Please see the guides for how to fix these issues at www.smartdevicelink.com");
        }

        return new ValidationResult(success, builder.toString());
    }

    private static ValidationResult checkPermissions(Context context) {
        ValidationResult retVal = new ValidationResult(true, "Permission check passed");
        List<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.BLUETOOTH);
        permissionList.add(Manifest.permission.INTERNET);
        permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionList.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissionInfos = packageInfo.requestedPermissions;

            if (permissionInfos != null && permissionInfos.length > 0) {
                String permissionInfo;
                for (String info : permissionInfos) {
                    permissionInfo = info;
                    permissionList.remove(permissionInfo);
                }
            }
        } catch (Exception e) {
        }

        if (!permissionList.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("This application is missing permissions: \n");
            for (String permission : permissionList) {
                builder.append("   - ");
                builder.append(permission);
                builder.append("\n");
            }
            retVal.successful = false;
            retVal.resultText = builder.toString();
        }

        return retVal;
    }

    public static ValidationResult checkBroadcastReceiver(Context context) {
        ValidationResult retVal = new ValidationResult(true, "SdlBroadcastReceiver check passed");
        try {
            Intent intent = new Intent();
            intent.setAction(TransportConstants.START_ROUTER_SERVICE_ACTION);
            List<ResolveInfo> sdlReceivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);
            ActivityInfo[] receivers = packageInfo.receivers;

            if (receivers != null && receivers.length > 0) {
                ActivityInfo receiver;
                for (ActivityInfo activityInfo : receivers) {

                    receiver = activityInfo;
                    if (receiver != null) {
                        int j = 0;
                        for (ResolveInfo sdlReceiver : sdlReceivers) {
                            if (receiver.name.equals(sdlReceiver.activityInfo.name)) {
                                return retVal;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        retVal.successful = false;
        retVal.resultText = "This application has not specified its SDL Receiver properly.";
        return retVal;
    }

    // Check if the service declaration in AndroidManifest has the router service version metadata specified correctly
    private static ValidationResult checkRoutServiceMetadata(Context context, Class localRouterClass) {
        ValidationResult retVal = new ValidationResult(true, "SdlRouterService entry and metadata checks passed");

        ResolveInfo info = context.getPackageManager().resolveService(new Intent(context, localRouterClass), PackageManager.GET_META_DATA);
        if (info != null) {
            if (info.serviceInfo.metaData == null || !info.serviceInfo.metaData.containsKey(context.getString(R.string.sdl_router_service_version_name))) {
                retVal.successful = false;
                retVal.resultText = "This application has not specified its metadata tags for the SdlRouterService.";
            }
        } else {
            retVal.successful = false;
            retVal.resultText = "This application has not specified its SdlRouterService correctly in the manifest.";
        }
        return retVal;
    }

    // Check if the service declaration in AndroidManifest has the intent-filter action specified correctly
    private static ValidationResult checkRouterServiceIntent(Context context, Class localRouterClass) {
        ValidationResult retVal = new ValidationResult(true, "SdlRouterService intent filter check passed");

        boolean serviceFilterHasAction = false;
        String className = localRouterClass.getName();
        List<SdlAppInfo> services = AndroidTools.querySdlAppInfo(context, null, null);
        for (SdlAppInfo sdlAppInfo : services) {
            if (sdlAppInfo != null && sdlAppInfo.getRouterServiceComponentName() != null
                    && className.equals((sdlAppInfo.getRouterServiceComponentName().getClassName()))) {
                serviceFilterHasAction = true;
                break;
            }
        }
        if (!serviceFilterHasAction) {
            retVal.successful = false;
            retVal.resultText = "This application has not specified its intent-filter for the SdlRouterService.";
        }

        return retVal;
    }

    /**
     * Results from a validation check.
     * Includes if the check was successful and a human readable string to describe the results.
     */
    public static final class ValidationResult {
        boolean successful;
        String resultText;

        ValidationResult(boolean successful, String resultText) {
            this.successful = successful;
            this.resultText = resultText;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getResultText() {
            return resultText;
        }
    }
}
