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

import java.util.Arrays;
import java.util.List;

import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.ADD;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.DELETE;
import static com.smartdevicelink.managers.screen.menu.DynamicMenuUpdateAlgorithm.MenuCellState.KEEP;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DynamicMenuUpdateRunScoreTests {

    @Test
    public void testSettersAndGetters() {

        // set everything - we only use the constructor to set variables in the Menu Manager
        List<DynamicMenuUpdateAlgorithm.MenuCellState> oldStatus = Arrays.asList(KEEP, DELETE);
        List<DynamicMenuUpdateAlgorithm.MenuCellState> updatedStatus = Arrays.asList(KEEP, ADD);
        DynamicMenuUpdateRunScore runScore = new DynamicMenuUpdateRunScore(oldStatus, updatedStatus, TestValues.GENERAL_INT);

        // use getters and assert equality
        assertEquals(runScore.getScore(), TestValues.GENERAL_INT);
        assertEquals(runScore.getOldStatus(), oldStatus);
        assertEquals(runScore.getUpdatedStatus(), updatedStatus);
    }

}
