/*
 * Copyright (c) 2017 - 2020, SmartDeviceLink Consortium, Inc.
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
 * Neither the name of the SmartDeviceLink Consortium Inc. nor the names of
 * its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
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
package com.smartdevicelink.proxy.rpc;

import androidx.annotation.NonNull;

import com.smartdevicelink.proxy.RPCStruct;

import java.util.Hashtable;

/**
 * Describes the status of a parameter of seat.
 *
 * <p><b>Parameter List</b></p>
 *
 * <table border="1" rules="all">
 *  <tr>
 *      <th>Param Name</th>
 *      <th>Type</th>
 *      <th>Description</th>
 *      <th>Required</th>
 *      <th>Notes</th>
 *      <th>Version Available</th>
 *  </tr>
 *  <tr>
 *      <td>seatLocation</td>
 *      <td>SeatLocation</td>
 *      <td></td>
 *      <td>Y</td>
 *      <td></td>
 *      <td></td>
 *  </tr>
 *  <tr>
 *      <td>conditionActive</td>
 *      <td>Boolean</td>
 *      <td></td>
 *      <td>Y</td>
 *      <td></td>
 *      <td></td>
 *  </tr>
 * </table>
 *
 * @since SmartDeviceLink 7.1.0
 */
public class SeatStatus extends RPCStruct {
    public static final String KEY_SEAT_LOCATION = "seatLocation";
    public static final String KEY_CONDITION_ACTIVE = "conditionActive";

    /**
     * Constructs a new SeatStatus object
     */
    public SeatStatus() { }

    /**
     * Constructs a new SeatStatus object indicated by the Hashtable parameter
     *
     * @param hash The Hashtable to use
     */
    public SeatStatus(Hashtable<String, Object> hash) {
        super(hash);
    }

    /**
     * Constructs a new SeatStatus object
     *
     * @param seatLocation
     * @param conditionActive
     */
    public SeatStatus(@NonNull SeatLocation seatLocation, @NonNull Boolean conditionActive) {
        this();
        setSeatLocation(seatLocation);
        setConditionActive(conditionActive);
    }

    /**
     * Sets the seatLocation.
     *
     * @param seatLocation
     */
    public SeatStatus setSeatLocation(@NonNull SeatLocation seatLocation) {
        setValue(KEY_SEAT_LOCATION, seatLocation);
        return this;
    }

    /**
     * Gets the seatLocation.
     *
     * @return SeatLocation
     */
    public SeatLocation getSeatLocation() {
        return (SeatLocation) getObject(SeatLocation.class, KEY_SEAT_LOCATION);
    }

    /**
     * Sets the conditionActive.
     *
     * @param conditionActive
     */
    public SeatStatus setConditionActive(@NonNull Boolean conditionActive) {
        setValue(KEY_CONDITION_ACTIVE, conditionActive);
        return this;
    }

    /**
     * Gets the conditionActive.
     *
     * @return Boolean
     */
    public Boolean getConditionActive() {
        return getBoolean(KEY_CONDITION_ACTIVE);
    }
}
