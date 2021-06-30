/*
 * Copyright (c) 2019 Livio, Inc.
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

package com.smartdevicelink.managers;

import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.util.SystemInfo;

public interface SdlManagerListener extends BaseSdlManagerListener {

    /**
     * Called when a manager is ready for use
     */
    void onStart();

    /**
     * Called when the manager is destroyed
     */
    void onDestroy();

    /**
     * Called when the manager encounters an error
     *
     * @param info info regarding the error
     * @param e    the exception
     */
    void onError(SdlManager manager, String info, Exception e);

    /**
     * Called when the SDL manager detected a language mismatch. In case of a language mismatch the
     * manager should change the apps registration by updating the lifecycle configuration to the
     * specified language. If the app can support the specified language it should return an Object
     * of LifecycleConfigurationUpdate, otherwise it should return null to indicate that the language
     * is not supported.
     *
     * @param language    The VR+TTS language of the connected head unit the manager is trying to update the configuration.
     * @param hmiLanguage The HMI display language of the connected head unit the manager is trying to update the configuration.
     * @return An object of LifecycleConfigurationUpdate if the head unit language is supported,
     * otherwise null to indicate that the language is not supported.
     */
    LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language, Language hmiLanguage);

    /**
     * A way to determine if this SDL session should continue to be active while
     * connected to the determined system information of the vehicle.
     * @param systemInfo systemInfo - the system information of the vehicle that this session is currently active on.
     * @return Return true if this session should continue, false if the session should end
     */
    boolean onSystemInfoReceived(SystemInfo systemInfo);
}
