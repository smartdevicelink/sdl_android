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

package com.smartdevicelink.managers.screen.menu;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smartdevicelink.test.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class VoiceCommandTests {

    private VoiceCommandSelectionListener voiceCommandSelectionListener = new VoiceCommandSelectionListener() {
        @Override
        public void onVoiceCommandSelected() {
            // Stuffs
        }
    };

    @Test
    public void testSettersAndGetters() {
        VoiceCommand voiceCommand = new VoiceCommand(TestValues.GENERAL_STRING_LIST, voiceCommandSelectionListener);

        assertEquals(voiceCommand.getVoiceCommands(), TestValues.GENERAL_STRING_LIST);
        assertEquals(voiceCommand.getVoiceCommandSelectionListener(), voiceCommandSelectionListener);
    }

    @Test
    public void testDuplicateStrings() {
        List<String> voiceCommandsList = new ArrayList<>();
        voiceCommandsList.add("Test1");
        voiceCommandsList.add("Test1");
        voiceCommandsList.add("Test1");
        VoiceCommand voiceCommand = new VoiceCommand(voiceCommandsList, voiceCommandSelectionListener);

        assertEquals(1, voiceCommand.getVoiceCommands().size());
        assertEquals("Test1", voiceCommand.getVoiceCommands().get(0));

        voiceCommandsList = new ArrayList<>();
        voiceCommandsList.add("Test1");
        voiceCommandsList.add("Test2");
        voiceCommandsList.add("Test1");
        VoiceCommand voiceCommand2 = new VoiceCommand(voiceCommandsList, voiceCommandSelectionListener);

        assertEquals(2, voiceCommand2.getVoiceCommands().size());
        assertEquals("Test1", voiceCommand2.getVoiceCommands().get(0));
        assertEquals("Test2", voiceCommand2.getVoiceCommands().get(1));
    }
}
