package com.sdl.hellosdlandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.transport.SdlBroadcastReceiver;
import com.smartdevicelink.transport.SdlRouterService;
import com.smartdevicelink.transport.TransportConstants;
import com.smartdevicelink.util.AndroidTools;
import com.smartdevicelink.util.DebugTool;

import java.util.HashMap;
import java.util.List;

public class SdlReceiver extends SdlBroadcastReceiver {
    private static final String TAG = "SdlBroadcastReceiver";

    @Override
    public void onSdlEnabled(Context context, Intent intent) {
        DebugTool.logInfo(TAG, "SDL Enabled");

        if (!IsVehicleTypeSupported(context, intent)) {
            DebugTool.logError(TAG, "Vehicle type is not supported. Exiting");
            return;
        }

        intent.setClass(context, SdlService.class);

        // SdlService needs to be foregrounded in Android O and above
        // This will prevent apps in the background from crashing when they try to start SdlService
        // Because Android O doesn't allow background apps to start background services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private boolean IsVehicleTypeSupported(Context context, Intent intent) {
        if (!intent.hasExtra(TransportConstants.VEHICLE_INFO)) {
            DebugTool.logInfo(TAG, "Vehicle type is not supplied by the intent");
            return true;
        }

        HashMap<String, Object> vehicleInfoStore = (HashMap<String, Object>) intent.getSerializableExtra(TransportConstants.VEHICLE_INFO);

        VehicleType vehicleType;
        if (vehicleInfoStore == null || vehicleInfoStore.isEmpty()) {
            vehicleType = null;
        } else {
            vehicleType = new VehicleType(vehicleInfoStore);
        }

        if (vehicleType == null) {
            DebugTool.logInfo(TAG, "Vehicle type is empty");
            return true;
        }

        List<VehicleType> supportedList = AndroidTools.getVehicleTypesFromManifest(context, com.sdl.hellosdlandroid.SdlRouterService.class, R.string.sdl_oem_vehicle_type_filter_name);
        return AndroidTools.isSupportableVehicleType(supportedList, vehicleType);
    }

    @Override
    public Class<? extends SdlRouterService> defineLocalSdlRouterClass() {
        return com.sdl.hellosdlandroid.SdlRouterService.class;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent); // Required if overriding this method
    }
}