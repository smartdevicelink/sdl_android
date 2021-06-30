/*
 * Copyright (c) 2017 - 2019, SmartDeviceLink Consortium, Inc.
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
 * Neither the name of the SmartDeviceLink Consortium, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
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

import com.smartdevicelink.proxy.RPCStruct;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * Describes the type of vehicle the mobile phone is connected with.
 * <p><b>Parameter List</b></p>
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 * 			<th>SmartDeviceLink Ver. Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>make</td>
 * 			<td>String</td>
 * 			<td>Make of the vehicle
 * 				 <ul>
 * 					<li>Maxlength = 500</li>
 * 				 </ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>Model</td>
 * 			<td>String</td>
 * 			<td>Model of the vehicle, e.g. Fiesta
 * 				 <ul>
 * 					<li>Maxlength = 500</li>
 * 				 </ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>modelYear</td>
 * 			<td>String</td>
 * 			<td>Model Year of the vehicle, e.g. 2013
 * 				 <ul>
 * 					<li>Maxlength = 500</li>
 * 				 </ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>trim</td>
 * 			<td>String</td>
 * 			<td>Trim of the vehicle, e.g. SE
 * 				 <ul>
 * 					<li>Maxlength = 500</li>
 * 				 </ul>
 * 			</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 *  </table>
 *
 * @since SmartDeviceLink 2.0
 */
public class VehicleType extends RPCStruct {
    public static final String KEY_MAKE = "make";
    public static final String KEY_MODEL = "model";
    public static final String KEY_MODEL_YEAR = "modelYear";
    public static final String KEY_TRIM = "trim";

    /**
     * Constructs a newly allocated VehicleType object
     */
    public VehicleType() {
    }

    /**
     * Constructs a newly allocated VehicleType object indicated by the Hashtable parameter
     *
     * @param hash The Hashtable to use
     */
    public VehicleType(Hashtable<String, Object> hash) {
        super(hash);
    }

    public VehicleType(HashMap<String, Object> hash) {
        super(new Hashtable<>(hash));
    }

    /**
     * get the make of the vehicle
     *
     * @return the make of the vehicle
     */
    public String getMake() {
        return getString(KEY_MAKE);
    }

    /**
     * set the make of the vehicle
     *
     * @param make the make of the vehicle
     */
    public VehicleType setMake(String make) {
        setValue(KEY_MAKE, make);
        return this;
    }

    /**
     * get the model of the vehicle
     *
     * @return the model of the vehicle
     */
    public String getModel() {
        return getString(KEY_MODEL);
    }

    /**
     * set the model of the vehicle
     *
     * @param model the model of the vehicle
     */
    public VehicleType setModel(String model) {
        setValue(KEY_MODEL, model);
        return this;
    }

    /**
     * get the model year of the vehicle
     *
     * @return the model year of the vehicle
     */
    public String getModelYear() {
        return getString(KEY_MODEL_YEAR);
    }

    /**
     * set the model year of the vehicle
     *
     * @param modelYear the model year of the vehicle
     */
    public VehicleType setModelYear(String modelYear) {
        setValue(KEY_MODEL_YEAR, modelYear);
        return this;
    }

    /**
     * get the trim of the vehicle
     *
     * @return the trim of the vehicle
     */
    public String getTrim() {
        return getString(KEY_TRIM);
    }

    /**
     * set the trim of the vehicle
     *
     * @param trim the trim of the vehicle
     */
    public VehicleType setTrim(String trim) {
        setValue(KEY_TRIM, trim);
        return this;
    }
}
