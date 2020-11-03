/*
 * Copyright (c)  2019 Livio, Inc.
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
 *
 * Created by brettywhite on 8/2/19 11:27 AM
 *
 */

package com.smartdevicelink.managers.lifecycle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smartdevicelink.test.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class LifecycleConfigurationUpdateTest {

    @Test
    public void testIndividualSettersAndGetters() {

        LifecycleConfigurationUpdate lifecycleConfigurationUpdate = new LifecycleConfigurationUpdate();

        lifecycleConfigurationUpdate.setAppName(TestValues.GENERAL_STRING);
        lifecycleConfigurationUpdate.setShortAppName(TestValues.GENERAL_STRING);
        lifecycleConfigurationUpdate.setTtsName(TestValues.GENERAL_VECTOR_TTS_CHUNKS);
        lifecycleConfigurationUpdate.setVoiceRecognitionCommandNames(TestValues.GENERAL_VECTOR_STRING);

        assertEquals(TestValues.GENERAL_STRING, lifecycleConfigurationUpdate.getAppName());
        assertEquals(TestValues.GENERAL_STRING, lifecycleConfigurationUpdate.getShortAppName());
        assertEquals(TestValues.GENERAL_VECTOR_TTS_CHUNKS, lifecycleConfigurationUpdate.getTtsName());
        assertEquals(TestValues.GENERAL_VECTOR_STRING, lifecycleConfigurationUpdate.getVoiceRecognitionCommandNames());
    }

    @Test
    public void testHelperConstructor() {

        LifecycleConfigurationUpdate lifecycleConfigurationUpdate = new LifecycleConfigurationUpdate(TestValues.GENERAL_STRING, TestValues.GENERAL_STRING, TestValues.GENERAL_VECTOR_TTS_CHUNKS, TestValues.GENERAL_VECTOR_STRING);

        assertEquals(TestValues.GENERAL_STRING, lifecycleConfigurationUpdate.getAppName());
        assertEquals(TestValues.GENERAL_STRING, lifecycleConfigurationUpdate.getShortAppName());
        assertEquals(TestValues.GENERAL_VECTOR_TTS_CHUNKS, lifecycleConfigurationUpdate.getTtsName());
        assertEquals(TestValues.GENERAL_VECTOR_STRING, lifecycleConfigurationUpdate.getVoiceRecognitionCommandNames());
    }


}
