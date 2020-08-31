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

import androidx.annotation.NonNull;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.enums.Result;

import java.util.Hashtable;

/**
 * Subscribe Vehicle Data Response is sent, when SubscribeVehicleData has been called
 * 
 * @since SmartDeviceLink 2.0
 */
public class SubscribeVehicleDataResponse extends RPCResponse {
	public static final String KEY_SPEED = "speed";
	public static final String KEY_RPM = "rpm";
	public static final String KEY_EXTERNAL_TEMPERATURE = "externalTemperature";
	public static final String KEY_PRNDL = "prndl";
	public static final String KEY_TIRE_PRESSURE = "tirePressure";
	public static final String KEY_ENGINE_TORQUE = "engineTorque";
	public static final String KEY_ENGINE_OIL_LIFE = "engineOilLife";
	public static final String KEY_ODOMETER = "odometer";
	public static final String KEY_GPS = "gps";
	public static final String KEY_INSTANT_FUEL_CONSUMPTION = "instantFuelConsumption";
	public static final String KEY_BELT_STATUS = "beltStatus";
	public static final String KEY_BODY_INFORMATION = "bodyInformation";
	public static final String KEY_DEVICE_STATUS = "deviceStatus";
	public static final String KEY_DRIVER_BRAKING = "driverBraking";
	public static final String KEY_WIPER_STATUS = "wiperStatus";
	public static final String KEY_HEAD_LAMP_STATUS = "headLampStatus";
	public static final String KEY_ACC_PEDAL_POSITION = "accPedalPosition";
	public static final String KEY_STEERING_WHEEL_ANGLE = "steeringWheelAngle";
	public static final String KEY_E_CALL_INFO = "eCallInfo";
	public static final String KEY_AIRBAG_STATUS = "airbagStatus";
	public static final String KEY_EMERGENCY_EVENT = "emergencyEvent";
	public static final String KEY_CLUSTER_MODE_STATUS = "clusterModeStatus";
	public static final String KEY_MY_KEY = "myKey";
	public static final String KEY_FUEL_RANGE = "fuelRange";
	public static final String KEY_TURN_SIGNAL = "turnSignal";
	public static final String KEY_ELECTRONIC_PARK_BRAKE_STATUS = "electronicParkBrakeStatus";
    public static final String KEY_CLOUD_APP_VEHICLE_ID = "cloudAppVehicleID";
    public static final String KEY_GEAR_STATUS = "gearStatus";
    public static final String KEY_HANDS_OFF_STEERING = "handsOffSteering";
    public static final String KEY_WINDOW_STATUS = "windowStatus";
    /**
     * @deprecated
     */
    @Deprecated
    public static final String KEY_FUEL_LEVEL = "fuelLevel";
    /**
     * @deprecated
     */
    @Deprecated
    public static final String KEY_FUEL_LEVEL_STATE = "fuelLevel_State";
    public static final String KEY_STABILITY_CONTROLS_STATUS = "stabilityControlsStatus";

	/**
	 * Constructs a new SubscribeVehicleDataResponse object
	 */
    public SubscribeVehicleDataResponse() {
        super(FunctionID.SUBSCRIBE_VEHICLE_DATA.toString());
    }

	/**
	 * <p>Constructs a new SubscribeVehicleDataResponse object indicated by the Hashtable
	 * parameter</p>
	 * 
	 * 
	 * @param hash The Hashtable to use
	 */
    public SubscribeVehicleDataResponse(Hashtable<String, Object> hash) {
        super(hash);
    }
	/**
	 * Constructs a new SubscribeVehicleDataResponse object
	 * @param success whether the request is successfully processed
	 * @param resultCode whether the request is successfully processed
	 */
	public SubscribeVehicleDataResponse(@NonNull Boolean success, @NonNull Result resultCode) {
		this();
		setSuccess(success);
		setResultCode(resultCode);
	}
    /**
     * Sets Gps
     * @param gps a VehicleDataResult related to GPS
     */
    public SubscribeVehicleDataResponse setGps( VehicleDataResult gps) {
        setParameters(KEY_GPS, gps);
        return this;
    }
    /**
     * Gets Gps
     * @return a VehicleDataResult related to GPS
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getGps() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_GPS);
    }
    /**
     * Sets Speed
     * @param speed a VehicleDataResult related to speed
     */
    public SubscribeVehicleDataResponse setSpeed( VehicleDataResult speed) {
        setParameters(KEY_SPEED, speed);
        return this;
    }
    /**
     * Gets Speed
     * @return a VehicleDataResult related to speed
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getSpeed() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_SPEED);
    }
    /**
     * Sets rpm
     * @param rpm a VehicleDataResult related to RPM
     */
    public SubscribeVehicleDataResponse setRpm( VehicleDataResult rpm) {
        setParameters(KEY_RPM, rpm);
        return this;
    }
    /**
     * Gets rpm
     * @return a VehicleDataResult related to RPM
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getRpm() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_RPM);
    }
    /**
     * Sets the fuelLevel.
     *
     * @param fuelLevel The fuel level in the tank (percentage). This parameter is deprecated starting RPC Spec
     * 7.0, please see fuelRange.
     */
    @Deprecated
    public SubscribeVehicleDataResponse setFuelLevel( VehicleDataResult fuelLevel) {
        setParameters(KEY_FUEL_LEVEL, fuelLevel);
        return this;
    }
    /**
     * Gets the fuelLevel.
     *
     * @return VehicleDataResult The fuel level in the tank (percentage). This parameter is deprecated starting RPC Spec
     * 7.0, please see fuelRange.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public VehicleDataResult getFuelLevel() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_FUEL_LEVEL);
    }
    /**
     * Sets Fuel Level State
     * @param fuelLevelState a VehicleDataResult related to FuelLevel State
     */
    @Deprecated
    public SubscribeVehicleDataResponse setFuelLevelState( VehicleDataResult fuelLevelState) {
        setParameters(KEY_FUEL_LEVEL_STATE, fuelLevelState);
        return this;
    }
    /**
     * Gets Fuel Level State
     * @return a VehicleDataResult related to FuelLevel State
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public VehicleDataResult getFuelLevelState() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_FUEL_LEVEL_STATE);
    }
    /**
     * Sets Instant Fuel Consumption
     * @param instantFuelConsumption a VehicleDataResult related to instant fuel consumption
     */
    public SubscribeVehicleDataResponse setInstantFuelConsumption( VehicleDataResult instantFuelConsumption) {
        setParameters(KEY_INSTANT_FUEL_CONSUMPTION, instantFuelConsumption);
        return this;
    }
    /**
     * Gets Instant Fuel Consumption
     * @return a VehicleDataResult related to instant fuel consumption
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getInstantFuelConsumption() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_INSTANT_FUEL_CONSUMPTION);
    }
    /**
     * Sets External Temperature
     * @param externalTemperature a VehicleDataResult related to external temperature
     */
    public SubscribeVehicleDataResponse setExternalTemperature( VehicleDataResult externalTemperature) {
        setParameters(KEY_EXTERNAL_TEMPERATURE, externalTemperature);
        return this;
    }
    /**
     * Gets External Temperature
     * @return a VehicleDataResult related to external temperature
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getExternalTemperature() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_EXTERNAL_TEMPERATURE);
    }
    /**
     * Sets the prndl.
     *
     * @param prndl See PRNDL. This parameter is deprecated and it is now covered in `gearStatus`
     * @deprecated in SmartDeviceLink 7.0.0
     */
    @Deprecated
    public SubscribeVehicleDataResponse setPrndl( VehicleDataResult prndl) {
        setParameters(KEY_PRNDL, prndl);
        return this;
    }
    /**
     * Gets the prndl.
     *
     * @return VehicleDataResult See PRNDL. This parameter is deprecated and it is now covered in `gearStatus`
     * @deprecated in SmartDeviceLink 7.0.0
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public VehicleDataResult getPrndl() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_PRNDL);
    }
    /**
     * Sets Tire Pressure
     * @param tirePressure a VehicleDataResult related to tire pressure
     */
    public SubscribeVehicleDataResponse setTirePressure( VehicleDataResult tirePressure) {
        setParameters(KEY_TIRE_PRESSURE, tirePressure);
        return this;
    }
    /**
     * Gets Tire Pressure
     * @return a VehicleDataResult related to tire pressure
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getTirePressure() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_TIRE_PRESSURE);
    }
    /**
     * Sets Odometer
     * @param odometer a VehicleDataResult related to the odometer
     */
    public SubscribeVehicleDataResponse setOdometer( VehicleDataResult odometer) {
        setParameters(KEY_ODOMETER, odometer);
        return this;
    }
    /**
     * Gets Odometer
     * @return a VehicleDataResult related to the odometer
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getOdometer() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_ODOMETER);
    }
    /**
     * Sets Belt Status
     * @param beltStatus a VehicleDataResult related to the seat belt status
     */
    public SubscribeVehicleDataResponse setBeltStatus( VehicleDataResult beltStatus) {
        setParameters(KEY_BELT_STATUS, beltStatus);
        return this;
    }
    /**
     * Gets Belt Status
     * @return a VehicleDataResult related to the seat belt status
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getBeltStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_BELT_STATUS);
    }
    /**
     * Sets Body Information
     * @param bodyInformation a VehicleDataResult related to the body info
     */
    public SubscribeVehicleDataResponse setBodyInformation( VehicleDataResult bodyInformation) {
        setParameters(KEY_BODY_INFORMATION, bodyInformation);
        return this;
    }
    /**
     * Gets Body Information
     * @return a VehicleDataResult related to the body info
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getBodyInformation() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_BODY_INFORMATION);
    }
    /**
     * Sets Device Status
     * @param deviceStatus a VehicleDataResult related to the device status of the connected device
     */
    public SubscribeVehicleDataResponse setDeviceStatus( VehicleDataResult deviceStatus) {
        setParameters(KEY_DEVICE_STATUS, deviceStatus);
        return this;
    }
    /**
     * Gets Device Status
     * @return a VehicleDataResult related to the device status of the connected device
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getDeviceStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_DEVICE_STATUS);
    }
    /**
     * Sets Driver Braking
     * @param driverBraking a VehicleDataResult related to the driver breaking status
     */
    public SubscribeVehicleDataResponse setDriverBraking( VehicleDataResult driverBraking) {
        setParameters(KEY_DRIVER_BRAKING, driverBraking);
        return this;
    }
    /**
     * Gets Driver Braking
     * @return a VehicleDataResult related to the driver breaking status
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getDriverBraking() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_DRIVER_BRAKING);
    }
    /**
     * Sets Wiper Status
     * @param wiperStatus a VehicleDataResult related to the wiper status
     */
    public SubscribeVehicleDataResponse setWiperStatus( VehicleDataResult wiperStatus) {
        setParameters(KEY_WIPER_STATUS, wiperStatus);
        return this;
    }
    /**
     * Gets Wiper Status
     * @return a VehicleDataResult related to the wiper status
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getWiperStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_WIPER_STATUS);
    }
    /**
     * Sets Head Lamp Status
     * @param headLampStatus a VehicleDataResult related to the headlamp status
     */
    public SubscribeVehicleDataResponse setHeadLampStatus( VehicleDataResult headLampStatus) {
        setParameters(KEY_HEAD_LAMP_STATUS, headLampStatus);
        return this;
    }
    /**
     * Gets Head Lamp Status
     * @return a VehicleDataResult related to the headlamp status
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getHeadLampStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_HEAD_LAMP_STATUS);
    }
    /**
     * Sets Engine Torque
     * @param engineTorque a VehicleDataResult related to the engine's torque
     */
    public SubscribeVehicleDataResponse setEngineTorque( VehicleDataResult engineTorque) {
        setParameters(KEY_ENGINE_TORQUE, engineTorque);
        return this;
    }
    /**
     * Gets Engine Torque
     * @return a VehicleDataResult related to the engine's torque
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getEngineTorque() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_ENGINE_TORQUE);
    }
    /**
     * Sets Engine Oil Life
     * @param engineOilLife a VehicleDataResult related to the engine's oil life
     */
    public SubscribeVehicleDataResponse setEngineOilLife( VehicleDataResult engineOilLife) {
        setParameters(KEY_ENGINE_OIL_LIFE, engineOilLife);
        return this;
    }
    /**
     * Gets Engine Oil Life
     * @return a VehicleDataResult related to the engine's oil life
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getEngineOilLife() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_ENGINE_OIL_LIFE);
    }
    /**
     * Sets AccPedal Position
     * @param accPedalPosition a VehicleDataResult related to the accelerator pedal's position
     */
    public SubscribeVehicleDataResponse setAccPedalPosition( VehicleDataResult accPedalPosition) {
        setParameters(KEY_ACC_PEDAL_POSITION, accPedalPosition);
        return this;
    }
    /**
     * Gets AccPedal Position
     * @return a VehicleDataResult related to the accelerator pedal's position
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getAccPedalPosition() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_ACC_PEDAL_POSITION);
    }

    public SubscribeVehicleDataResponse setSteeringWheelAngle( VehicleDataResult steeringWheelAngle) {
        setParameters(KEY_STEERING_WHEEL_ANGLE, steeringWheelAngle);
        return this;
    }

    @SuppressWarnings("unchecked")
    public VehicleDataResult getSteeringWheelAngle() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_STEERING_WHEEL_ANGLE);
    }

    public SubscribeVehicleDataResponse setECallInfo( VehicleDataResult eCallInfo) {
        setParameters(KEY_E_CALL_INFO, eCallInfo);
        return this;
    }
    @SuppressWarnings("unchecked")
    public VehicleDataResult getECallInfo() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_E_CALL_INFO);
    }
    public SubscribeVehicleDataResponse setAirbagStatus( VehicleDataResult airbagStatus) {
        setParameters(KEY_AIRBAG_STATUS, airbagStatus);
        return this;
    }
    @SuppressWarnings("unchecked")
    public VehicleDataResult getAirbagStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_AIRBAG_STATUS);
    }
    public SubscribeVehicleDataResponse setEmergencyEvent( VehicleDataResult emergencyEvent) {
        setParameters(KEY_EMERGENCY_EVENT, emergencyEvent);
        return this;
    }
    @SuppressWarnings("unchecked")
    public VehicleDataResult getEmergencyEvent() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_EMERGENCY_EVENT);
    }
    public SubscribeVehicleDataResponse setClusterModeStatus( VehicleDataResult clusterModeStatus) {
        setParameters(KEY_CLUSTER_MODE_STATUS, clusterModeStatus);
        return this;
    }
    @SuppressWarnings("unchecked")
    public VehicleDataResult getClusterModeStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_CLUSTER_MODE_STATUS);
    }
    public SubscribeVehicleDataResponse setMyKey( VehicleDataResult myKey) {
        setParameters(KEY_MY_KEY, myKey);
        return this;
    }
    @SuppressWarnings("unchecked")
    public VehicleDataResult getMyKey() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_MY_KEY);
    }

    /**
     * Sets the fuelRange.
     *
     * @param fuelRange The fuel type, estimated range in KM, fuel level/capacity and fuel level state for the
     * vehicle. See struct FuelRange for details.
     * @since SmartDeviceLink 5.0.0
     */
    public SubscribeVehicleDataResponse setFuelRange( VehicleDataResult fuelRange) {
        setParameters(KEY_FUEL_RANGE, fuelRange);
        return this;
    }

    /**
     * Gets the fuelRange.
     *
     * @return VehicleDataResult The fuel type, estimated range in KM, fuel level/capacity and fuel level state for the
     * vehicle. See struct FuelRange for details.
     * @since SmartDeviceLink 5.0.0
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getFuelRange() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_FUEL_RANGE);
    }

    /**
     * Sets turnSignal
     * @param turnSignal a VehicleDataResult related to the turn signal status
     */
    public SubscribeVehicleDataResponse setTurnSignal( VehicleDataResult turnSignal) {
        setParameters(KEY_TURN_SIGNAL, turnSignal);
        return this;
    }

    /**
     * Gets turnSignal
     * @return a VehicleDataResult related to the turn signal status
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getTurnSignal() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_TURN_SIGNAL);
    }

    /**
     * Sets electronicParkBrakeStatus
     * @param electronicParkBrakeStatus a VehicleDataResult related to the electronic park brake status
     */
    public SubscribeVehicleDataResponse setElectronicParkBrakeStatus( VehicleDataResult electronicParkBrakeStatus) {
        setParameters(KEY_ELECTRONIC_PARK_BRAKE_STATUS, electronicParkBrakeStatus);
        return this;
    }

    /**
     * Gets electronicParkBrakeStatus
     * @return a VehicleDataResult related to the electronic park brake status
     */
    public VehicleDataResult getElectronicParkBrakeStatus(){
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_ELECTRONIC_PARK_BRAKE_STATUS);
    }

    /**
     * Sets cloudAppVehicleID
     * @param cloudAppVehicleID a VehicleDataResult related to the cloud app vehicle ID
     */
    public SubscribeVehicleDataResponse setCloudAppVehicleID( VehicleDataResult cloudAppVehicleID) {
        setParameters(KEY_CLOUD_APP_VEHICLE_ID, cloudAppVehicleID);
        return this;
    }

    /**
     * Gets a VehicleDataResult for the subscription response of the CloudAppVehicleID vehicle data item.
     * @return a VehicleDataResult related to the cloud app vehicle ID
     */
    public VehicleDataResult getCloudAppVehicleID(){
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_CLOUD_APP_VEHICLE_ID);
    }

    /**
     * Sets a value for OEM Custom VehicleData.
     * @param vehicleDataName a String value
     * @param vehicleDataState a VehicleDataResult value
     */
    public SubscribeVehicleDataResponse setOEMCustomVehicleData( String vehicleDataName, VehicleDataResult vehicleDataState) {
        setParameters(vehicleDataName, vehicleDataState);
        return this;
    }

    /**
     * Gets a VehicleDataResult for the vehicle data item.
     * @return a VehicleDataResult related to the vehicle data
     */
    public VehicleDataResult getOEMCustomVehicleData(String vehicleDataName){
        return (VehicleDataResult) getObject(VehicleDataResult.class, vehicleDataName);
    }

    /**
     * Sets the handsOffSteering.
     *
     * @param handsOffSteering To indicate whether driver hands are off the steering wheel
     * @since SmartDeviceLink 7.0.0
     */
    public SubscribeVehicleDataResponse setHandsOffSteering( VehicleDataResult handsOffSteering) {
        setParameters(KEY_HANDS_OFF_STEERING, handsOffSteering);
        return this;
    }

    /**
     * Gets the handsOffSteering.
     *
     * @return VehicleDataResult To indicate whether driver hands are off the steering wheel
     * @since SmartDeviceLink 7.0.0
     */
    public VehicleDataResult getHandsOffSteering() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_HANDS_OFF_STEERING);
    }
    /**
     * Sets the gearStatus.
     *
     * @param gearStatus See GearStatus
     * @since SmartDeviceLink 7.0.0
     */
    public SubscribeVehicleDataResponse setGearStatus( VehicleDataResult gearStatus) {
        setParameters(KEY_GEAR_STATUS, gearStatus);
        return this;
    }

    /**
     * Gets the gearStatus.
     *
     * @return VehicleDataResult See GearStatus
     * @since SmartDeviceLink 7.0.0
     */
    @SuppressWarnings("unchecked")
    public VehicleDataResult getGearStatus(){
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_GEAR_STATUS);
    }

    /**
     * Sets the windowStatus.
     *
     * @param windowStatus See WindowStatus
     * @since SmartDeviceLink 7.0.0
     */
    public SubscribeVehicleDataResponse setWindowStatus( VehicleDataResult windowStatus) {
        setParameters(KEY_WINDOW_STATUS, windowStatus);
        return this;
    }

    /**
     * Gets the windowStatus.
     *
     * @return VehicleDataResult See WindowStatus
     * @since SmartDeviceLink 7.0.0
     */
    public VehicleDataResult getWindowStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_WINDOW_STATUS);
    }

    /**
     * Gets the stabilityControlsStatus.
     *
     * @return VehicleDataResult See StabilityControlsStatus
     * @since SmartDeviceLink 7.0.0
     */
    public VehicleDataResult getStabilityControlsStatus() {
        return (VehicleDataResult) getObject(VehicleDataResult.class, KEY_STABILITY_CONTROLS_STATUS);
    }

    /**
     * Sets the stabilityControlsStatus.
     *
     * @param stabilityControlsStatus See StabilityControlsStatus
     * @since SmartDeviceLink 7.0.0
     */
    public SubscribeVehicleDataResponse setStabilityControlsStatus( VehicleDataResult stabilityControlsStatus) {
        setParameters(KEY_STABILITY_CONTROLS_STATUS, stabilityControlsStatus);
        return this;
    }
}
