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

import com.smartdevicelink.managers.file.filetypes.SdlArtworkTests;
import com.smartdevicelink.proxy.rpc.enums.MenuLayout;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.test.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class MenuCellTests {

    private MenuSelectionListener menuSelectionListener = new MenuSelectionListener() {
        @Override
        public void onTriggered(TriggerSource trigger) {
            // stuff
        }
    };

    @Test
    public void testSettersAndGetters() {
        // set everything
        MenuCell menuCell = new MenuCell(TestValues.GENERAL_STRING, null, null, menuSelectionListener);
        menuCell.setIcon(TestValues.GENERAL_ARTWORK);
        menuCell.setVoiceCommands(TestValues.GENERAL_STRING_LIST);
        menuCell.setMenuSelectionListener(menuSelectionListener);
        menuCell.setSubMenuLayout(TestValues.GENERAL_MENU_LAYOUT);
        menuCell.setSecondaryText(TestValues.GENERAL_STRING);
        menuCell.setTertiaryText(TestValues.GENERAL_STRING);
        menuCell.setSecondaryArtwork(TestValues.GENERAL_ARTWORK);
        menuCell.setUniqueTitle(TestValues.GENERAL_STRING);

        // use getters and assert equality
        assertEquals(menuCell.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell.getIcon(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell.getVoiceCommands(), TestValues.GENERAL_STRING_LIST);
        assertEquals(menuCell.getMenuSelectionListener(), menuSelectionListener);
        assertEquals(menuCell.getCellId(), TestValues.GENERAL_MENU_MAX_ID);
        assertEquals(menuCell.getParentCellId(), TestValues.GENERAL_MENU_MAX_ID);
        assertEquals(menuCell.getSubMenuLayout(), TestValues.GENERAL_MENU_LAYOUT);
        assertEquals(menuCell.getSecondaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell.getTertiaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell.getSecondaryArtwork(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell.getUniqueTitle(), TestValues.GENERAL_STRING);
    }

    @Test
    public void testConstructors() {
        // first constructor was tested in previous method, use the last two here

        MenuCell menuCell3 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        assertEquals(menuCell3.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell3.getIcon(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell3.getVoiceCommands(), TestValues.GENERAL_STRING_LIST);
        assertEquals(menuCell3.getMenuSelectionListener(), menuSelectionListener);
        assertEquals(menuCell3.getUniqueTitle(), TestValues.GENERAL_STRING);

        MenuCell menuCell4 = new MenuCell(TestValues.GENERAL_STRING, null, null, menuSelectionListener);
        assertEquals(menuCell4.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell4.getMenuSelectionListener(), menuSelectionListener);
        assertEquals(menuCell4.getUniqueTitle(), TestValues.GENERAL_STRING);

        MenuCell menuCell5 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_MENU_LAYOUT, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_MENUCELL_LIST);
        assertEquals(menuCell5.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell5.getIcon(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell5.getSubMenuLayout(), TestValues.GENERAL_MENU_LAYOUT);
        assertEquals(menuCell5.getSubCells(), TestValues.GENERAL_MENUCELL_LIST);
        assertEquals(menuCell5.getUniqueTitle(), TestValues.GENERAL_STRING);


        MenuCell menuCell6 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_STRING, TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        assertEquals(menuCell6.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell6.getIcon(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell6.getSecondaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell6.getTertiaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell6.getSecondaryArtwork(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell6.getVoiceCommands(), TestValues.GENERAL_STRING_LIST);
        assertEquals(menuCell6.getMenuSelectionListener(), menuSelectionListener);

        MenuCell menuCell7 = new MenuCell(TestValues.GENERAL_STRING, null, null, null, null, null, menuSelectionListener);
        assertEquals(menuCell7.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell7.getMenuSelectionListener(), menuSelectionListener);

        MenuCell menuCell8 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_STRING, TestValues.GENERAL_STRING, TestValues.GENERAL_MENU_LAYOUT, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_MENUCELL_LIST);
        assertEquals(menuCell8.getTitle(), TestValues.GENERAL_STRING);
        assertEquals(menuCell8.getIcon(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell8.getSecondaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell8.getTertiaryText(), TestValues.GENERAL_STRING);
        assertEquals(menuCell8.getSecondaryArtwork(), TestValues.GENERAL_ARTWORK);
        assertEquals(menuCell8.getSubMenuLayout(), TestValues.GENERAL_MENU_LAYOUT);
        assertEquals(menuCell8.getSubCells(), TestValues.GENERAL_MENUCELL_LIST);
    }

    @Test
    public void testEquality() {
        MenuCell menuCell1 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        MenuCell menuCell1_1 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        menuCell1.setSubCells(Collections.singletonList(menuCell1_1));

        MenuCell menuCell2 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        MenuCell menuCell2_1 = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        menuCell2.setSubCells(Collections.singletonList(menuCell2_1));

        // these are the same object, should be equal.
        assertEquals(menuCell1, menuCell1);

        // Make sure these are marked as equals, even though they are different objects
        assertEquals(menuCell1, menuCell2);

        MenuCell menuCell3 = new MenuCell(TestValues.GENERAL_STRING, null, TestValues.GENERAL_STRING_LIST, menuSelectionListener);

        // these should be different
        assertNotEquals(menuCell1, menuCell3);

        menuCell1_1.setTitle("new title");

        // Make sure sub cells are compared
        assertNotEquals(menuCell1, menuCell2);
    }

    @Test
    public void testClone() {
        MenuCell original = new MenuCell(TestValues.GENERAL_STRING, TestValues.GENERAL_ARTWORK, TestValues.GENERAL_STRING_LIST, menuSelectionListener);
        MenuCell clone = original.clone();

        assertNotNull(clone);
        assertNotSame(original, clone);

        assertEquals(original.getTitle(), clone.getTitle());
        assertEquals(original.getCellId(), clone.getCellId());
        assertEquals(original.getParentCellId(), clone.getParentCellId());
        assertEquals(original.getUniqueTitle(), clone.getUniqueTitle());
        assertEquals(original.getSecondaryText(), clone.getSecondaryText());
        assertEquals(original.getTertiaryText(), clone.getTertiaryText());

        SdlArtworkTests.equalTest(original.getIcon(), clone.getIcon());
        SdlArtworkTests.equalTest(original.getSecondaryArtwork(), clone.getSecondaryArtwork());

        //Test subcells
        List<MenuCell> subcells = new ArrayList<>();
        subcells.add(original.clone());
        subcells.add(clone.clone());

        original = new MenuCell(TestValues.GENERAL_STRING, MenuLayout.LIST, TestValues.GENERAL_ARTWORK, subcells);
        clone = original.clone();

        assertNotNull(original.getSubCells());
        assertNotNull(clone.getSubCells());
        assertNotSame(original.getSubCells(), clone.getSubCells());

        List<MenuCell> originalSubCells = original.getSubCells();
        List<MenuCell> cloneSubCells = clone.getSubCells();

        assertEquals(originalSubCells.size(), cloneSubCells.size());

        for (int i = 0; i < originalSubCells.size(); i++) {

            assertNotNull(originalSubCells.get(i));
            assertNotNull(cloneSubCells.get(i));

            assertNotSame(originalSubCells.get(i), cloneSubCells.get(i));
        }
    }
}
