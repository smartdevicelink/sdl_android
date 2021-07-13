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
 * Created by brettywhite on 7/18/19 3:28 PM
 *
 */

package com.smartdevicelink.managers.screen.menu;

import androidx.annotation.NonNull;

import com.smartdevicelink.proxy.rpc.enums.MenuLayout;

public class MenuConfiguration {

    private MenuLayout mainMenuLayout, submenuLayout;

    /**
     * Create a MenuConfiguration Object
     *
     * @param mainMenuLayout - the layout of the main menu. If `null`, it will default to whatever the head unit uses
     * @param submenuLayout  - the layout of the main menu. If `null`, it will default to whatever the head unit uses
     */
    public MenuConfiguration(MenuLayout mainMenuLayout, MenuLayout submenuLayout) {
        setMenuLayout(mainMenuLayout);
        setSubMenuLayout(submenuLayout);
    }

    /**
     * Changes the default main menu layout.
     *
     * @param mainMenuLayout - the layout of the main menu
     */
    private void setMenuLayout(MenuLayout mainMenuLayout) {
        this.mainMenuLayout = mainMenuLayout;
    }

    /**
     * Changes the default main menu layout.
     *
     * @return - the layout of the main menu
     */
    public MenuLayout getMenuLayout() {
        return this.mainMenuLayout;
    }

    /**
     * Changes the default submenu layout. To change this for an individual submenu, set the
     * `menuLayout` property on the `MenuCell` constructor for creating a cell with sub-cells.
     *
     * @param submenuLayout - the MenuLayout for this sub menu
     */
    private void setSubMenuLayout(MenuLayout submenuLayout) {
        this.submenuLayout = submenuLayout;
    }

    /**
     * Changes the default submenu layout. To change this for an individual submenu, set the
     * `menuLayout` property on the `MenuCell` constructor for creating a cell with sub-cells.
     *
     * @return - the MenuLayout for this sub menu
     */
    public MenuLayout getSubMenuLayout() {
        return submenuLayout;
    }

    /**
     * @return A string description of the cell, useful for debugging.
     */
    @Override
    @NonNull
    public String toString() {
        return "MenuConfiguration: MenuLayout = " + this.mainMenuLayout + " | SubMenuLayout = " + this.submenuLayout;
    }

    /**
     * Note: You should compare using the {@link #equals(Object)} method. <br>
     * Hash the parameters of the object and return the result for comparison
     * @return the hash code as an int
     */
    @Override
    public int hashCode() {
        int result = 1;
        result += ((getMenuLayout() == null) ? 0 : Integer.rotateLeft(getMenuLayout().hashCode(), 1));
        result += ((getSubMenuLayout() == null) ? 0 : Integer.rotateLeft(getSubMenuLayout().hashCode(), 2));
        return result;
    }

    /**
     * Uses our custom hashCode for MenuConfiguration objects
     *
     * @param o - The object to compare
     * @return boolean of whether the objects are the same or not
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        // if this is the same memory address, its the same
        if (this == o) return true;
        // if this is not an instance of this class, not the same
        if (!(o instanceof MenuConfiguration)) return false;
        // if we get to this point, create the hashes and compare them
        return hashCode() == o.hashCode();
    }

}
