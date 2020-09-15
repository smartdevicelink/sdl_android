package com.smartdevicelink.managers.screen;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.smartdevicelink.managers.ISdl;

/**
 * <strong>SubscribeButtonManager</strong> <br>
 *
 * Note: This class must be accessed through the SdlManager. Do not instantiate it by itself. <br>
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SubscribeButtonManager extends BaseSubscribeButtonManager {

    SubscribeButtonManager(@NonNull ISdl internalInterface) {
        super(internalInterface);
    }
}
