package com.smartdevicelink.proxy.rpc;

import java.util.Hashtable;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCRequest;

import static android.provider.Contacts.SettingsColumns.KEY;
import static com.smartdevicelink.proxy.constants.Names.choiceSet;
import static com.smartdevicelink.proxy.constants.Names.externalTemperature;
import static com.smartdevicelink.proxy.constants.Names.instantFuelConsumption;
import static com.smartdevicelink.proxy.constants.Names.prndl;
import static com.smartdevicelink.proxy.constants.Names.vin;
import static com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet.KEY_CHOICE_SET;

/**
 * Non periodic vehicle data read request.
 * 
 * <p><b>Parameter List</b></p>
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 *                 <th>Reg.</th>
 *               <th>Notes</th>
 * 			<th>Version</th>
 * 		</tr>
 * 		<tr>
 * 			<td>gps</td>
 * 			<td>Boolean</td>
 * 			<td>GPS data. See {@linkplain GPS data }for details</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0 </td>
 * 		</tr>
 * 		<tr>
 * 			<td>speed</td>
 * 			<td>Boolean</td>
 * 			<td>The vehicle speed in kilometers per hour</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>rpm</td>
 * 			<td>Boolean</td>
 * 			<td>The number of revolutions per minute of the engine</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>fuelLevel</td>
 * 			<td>Boolean</td>
 * 			<td>The fuel level in the tank (percentage)</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>fuelLevel_State</td>
 * 			<td>Boolean</td>
 * 			<td>The fuel level state</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>instantFuelConsumption</td>
 * 			<td>Boolean</td>
 * 			<td>The instantaneous fuel consumption in microlitres</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>externalTemperature</td>
 * 			<td>Boolean</td>
 * 			<td>The external temperature in degrees celsius</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>vin</td>
 * 			<td>Boolean</td>
 * 			<td>Vehicle identification number</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>prndl</td>
 * 			<td>Boolean</td>
 * 			<td>Currently selected gear.</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>tirePressure</td>
 * 			<td>Boolean</td>
 * 			<td>Tire pressure status</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>odometer</td>
 * 			<td>Boolean</td>
 * 			<td>Odometer in km</td>
 *                 <td>N</td>
 *                 <td>Max Length: 500</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>beltStatus</td>
 * 			<td>Boolean</td>
 * 			<td>The status of the seat belts</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>bodyInformation</td>
 * 			<td>Boolean</td>
 * 			<td>The body information including ignition status and internal temp</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0 </td>
 * 		</tr>
 * 		<tr>
 * 			<td>deviceStatus</td>
 * 			<td>Boolean</td>
 * 			<td>The device status including signal and battery strength</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>driverBraking</td>
 * 			<td>Boolean</td>
 * 			<td>The status of the brake pedal</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>wiperStatus</td>
 * 			<td>Boolean</td>
 * 			<td>The status of the wipers</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>headLampStatus</td>
 * 			<td>Boolean</td>
 * 			<td>Status of the head lamps</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>engineTorque</td>
 * 			<td>Boolean</td>
 * 			<td>Torque value for engine (in Nm) on non-diesel variants</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>accPedalPosition</td>
 * 			<td>Boolean</td>
 * 			<td>Accelerator pedal position (percentage depressed)</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>steeringWheelAngle</td>
 * 			<td>Boolean</td>
 * 			<td>Current angle of the steering wheel (in deg)</td>
 *                 <td>N</td>
 *                 <td>Subscribable</td>
 * 			<td>SmartDeviceLink 2.0</td>
 * 		</tr>
 *  </table>
 *  
 *  
 *  <p><b>Response</b></p>
 *  
 * <p> <b>Non-default Result Codes:</b></p>
 * <p> SUCCESS</p>
 * <p> INVALID_DATA</p>
 *  <p>OUT_OF_MEMORY</p>
 *  <p>TOO_MANY_PENDING_REQUESTS</p>
 *  <p>APPLICATION_NOT_REGISTERED</p>
 * <p> GENERIC_ERROR</p>
 * <p> REJECTED</p>
 * <p> VEHICLE_DATA_NOT_ALLOWED</p>
 *  <p>VEHICLE_DATA_NOT_AVAILABLE</p>
 * <p> USER_DISALLOWED</p>
 * 
 * @see SubscribeVehicleData
 * @see UnsubscribeVehicleData
 * @since SmartDeviceLink 2.0
 * 
 *
 */
public class GetVehicleData extends RPCRequest {
	public static final String KEY_SPEED = "speed";
	public static final String KEY_RPM = "rpm";
	public static final String KEY_EXTERNAL_TEMPERATURE = "externalTemperature";
	public static final String KEY_FUEL_LEVEL = "fuelLevel";
	public static final String KEY_VIN = "vin";
	public static final String KEY_PRNDL = "prndl";
	public static final String KEY_TIRE_PRESSURE = "tirePressure";
	public static final String KEY_ENGINE_TORQUE = "engineTorque";
	public static final String KEY_ODOMETER = "odometer";
	public static final String KEY_GPS = "gps";
	public static final String KEY_FUEL_LEVEL_STATE = "fuelLevel_State";
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
	/**
	 * Constructs a new GetVehicleData object
	 */

    public GetVehicleData() {
        super(FunctionID.GET_VEHICLE_DATA.toString());
    }
    /**
	* <p>
	* Constructs a new GetVehicleDta object indicated by the Hashtable
	* parameter
	* </p>
	* 
	* @param hash
	*            The Hashtable to use
	*/

    public GetVehicleData(Hashtable<String, Object> hash) {
        super(hash);
    }
    public void setGps(Boolean gps) {
        setParameter(KEY_GPS, gps);
    }
    public Boolean getGps() {
        return getBoolean(KEY_GPS);
    }
    public void setSpeed(Boolean speed) {
        setParameter(KEY_SPEED, speed);
    }
    public Boolean getSpeed() {
        return getBoolean(KEY_SPEED);
    }
    public void setRpm(Boolean rpm) {
        setParameter(KEY_RPM, rpm);
    }
    public Boolean getRpm() {
        return getBoolean(KEY_RPM);
    }
    public void setFuelLevel(Boolean fuelLevel) {
        setParameter(KEY_FUEL_LEVEL, fuelLevel);
    }
    public Boolean getFuelLevel() {
        return getBoolean(KEY_FUEL_LEVEL);
    }
    @Deprecated
    public void setFuelLevel_State(Boolean fuelLevel_State) {
        setFuelLevelState(fuelLevel_State);
    }
    @Deprecated
    public Boolean getFuelLevel_State() {
        return getFuelLevelState();
    }
    public void setFuelLevelState(Boolean fuelLevelState) {
        setParameter(KEY_FUEL_LEVEL_STATE, fuelLevelState);
    }
    public Boolean getFuelLevelState() {
        return getBoolean(KEY_FUEL_LEVEL_STATE);
    }
    public void setInstantFuelConsumption(Boolean instantFuelConsumption) {
        setParameter(KEY_INSTANT_FUEL_CONSUMPTION, instantFuelConsumption);
    }
    public Boolean getInstantFuelConsumption() {
        return getBoolean(KEY_INSTANT_FUEL_CONSUMPTION);
    }
    public void setExternalTemperature(Boolean externalTemperature) {
        setParameter(KEY_EXTERNAL_TEMPERATURE, externalTemperature);
    }
    public Boolean getExternalTemperature() {
        return getBoolean(KEY_EXTERNAL_TEMPERATURE);
    }
    
    public void setVin(Boolean vin) {
        setParameter(KEY_VIN, vin);
    }
    public Boolean getVin() {
        return getBoolean(KEY_VIN);
    }
    
    public void setPrndl(Boolean prndl) {
        setParameter(KEY_PRNDL, prndl);
    }
    public Boolean getPrndl() {
        return getBoolean(KEY_PRNDL);
    }
    public void setTirePressure(Boolean tirePressure) {
        setParameter(KEY_TIRE_PRESSURE, tirePressure);
    }
    public Boolean getTirePressure() {
        return getBoolean(KEY_TIRE_PRESSURE);
    }
    public void setOdometer(Boolean odometer) {
        setParameter(KEY_ODOMETER, odometer);
    }
    public Boolean getOdometer() {
        return getBoolean(KEY_ODOMETER);
    }
    public void setBeltStatus(Boolean beltStatus) {
        setParameter(KEY_BELT_STATUS, beltStatus);
    }
    public Boolean getBeltStatus() {
        return getBoolean(KEY_BELT_STATUS);
    }
    public void setBodyInformation(Boolean bodyInformation) {
        setParameter(KEY_BODY_INFORMATION, bodyInformation);
    }
    public Boolean getBodyInformation() {
        return getBoolean(KEY_BODY_INFORMATION);
    }
    public void setDeviceStatus(Boolean deviceStatus) {
        setParameter(KEY_DEVICE_STATUS, deviceStatus);
    }
    public Boolean getDeviceStatus() {
        return getBoolean(KEY_DEVICE_STATUS);
    }
    public void setDriverBraking(Boolean driverBraking) {
        setParameter(KEY_DRIVER_BRAKING, driverBraking);
    }
    public Boolean getDriverBraking() {
        return getBoolean(KEY_DRIVER_BRAKING);
    }
    public void setWiperStatus(Boolean wiperStatus) {
        setParameter(KEY_WIPER_STATUS, wiperStatus);
    }
    public Boolean getWiperStatus() {
        return getBoolean(KEY_WIPER_STATUS);
    }
    public void setHeadLampStatus(Boolean headLampStatus) {
        setParameter(KEY_HEAD_LAMP_STATUS, headLampStatus);
    }
    public Boolean getHeadLampStatus() {
        return getBoolean(KEY_HEAD_LAMP_STATUS);
    }
    public void setEngineTorque(Boolean engineTorque) {
        setParameter(KEY_ENGINE_TORQUE, engineTorque);
    }
    public Boolean getEngineTorque() {
        return getBoolean(KEY_ENGINE_TORQUE);
    }
    public void setAccPedalPosition(Boolean accPedalPosition) {
        setParameter(KEY_ACC_PEDAL_POSITION, accPedalPosition);
    }
    public Boolean getAccPedalPosition() {
        return getBoolean(KEY_ACC_PEDAL_POSITION);
    }
        
    public void setSteeringWheelAngle(Boolean steeringWheelAngle) {
        setParameter(KEY_STEERING_WHEEL_ANGLE, steeringWheelAngle);
    }
    public Boolean getSteeringWheelAngle() {
        return getBoolean(KEY_STEERING_WHEEL_ANGLE);
    }                
    public void setECallInfo(Boolean eCallInfo) {
        setParameter(KEY_E_CALL_INFO, eCallInfo);
    }
    public Boolean getECallInfo() {
        return getBoolean(KEY_E_CALL_INFO);
    }    
    
    
    public void setAirbagStatus(Boolean airbagStatus) {
        setParameter(KEY_AIRBAG_STATUS, airbagStatus);
    }
    public Boolean getAirbagStatus() {
        return getBoolean(KEY_AIRBAG_STATUS);
    }
    public void setEmergencyEvent(Boolean emergencyEvent) {
        setParameter(KEY_EMERGENCY_EVENT, emergencyEvent);
    }
    public Boolean getEmergencyEvent() {
        return getBoolean(KEY_EMERGENCY_EVENT);
    }
    public void setClusterModeStatus(Boolean clusterModeStatus) {
        setParameter(KEY_CLUSTER_MODE_STATUS, clusterModeStatus);
    }
    public Boolean getClusterModeStatus() {
        return getBoolean(KEY_CLUSTER_MODE_STATUS);
    }
    public void setMyKey(Boolean myKey) {
        setParameter(KEY_MY_KEY, myKey);
    }
    public Boolean getMyKey() {
        return getBoolean(KEY_MY_KEY);
    }        
}
