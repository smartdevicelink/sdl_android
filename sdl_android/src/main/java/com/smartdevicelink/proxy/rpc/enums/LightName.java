package com.smartdevicelink.proxy.rpc.enums;

public enum LightName {
	/*Common Single Light*/
	FRONT_LEFT_HIGH_BEAM,
	FRONT_RIGHT_HIGH_BEAM,
	FRONT_LEFT_LOW_BEAM,
	FRONT_RIGHT_LOW_BEAM,
	FRONT_LEFT_PARKING_LIGHT,
	FRONT_RIGHT_PARKING_LIGHT,
	FRONT_LEFT_FOG_LIGHT,
	FRONT_RIGHT_FOG_LIGHT,
	FRONT_LEFT_DAYTIME_RUNNING_LIGHT,
	FRONT_RIGHT_DAYTIME_RUNNING_LIGHT,
	FRONT_LEFT_TURN_LIGHT,
	FRONT_RIGHT_TURN_LIGHT,
	REAR_LEFT_FOG_LIGHT,
	REAR_RIGHT_FOG_LIGHT,
	REAR_LEFT_TAIL_LIGHT,
	REAR_RIGHT_TAIL_LIGHT,
	REAR_LEFT_BREAK_LIGHT,
	REAR_RIGHT_BREAK_LIGHT,
	REAR_LEFT_TURN_LIGHT,
	REAR_RIGHT_TURN_LIGHT,
	REAR_REGISTRATION_PLATE_LIGHT,

	/**
	 * Include all high beam lights: front_left and front_right.
	 */
	HIGH_BEAMS,
	/**
	 * Include all low beam lights: front_left and front_right.
	 */
	LOW_BEAMS,
	/**
	 * Include all fog lights: front_left, front_right, rear_left and rear_right.
	 */
	FOG_LIGHTS,
	/**
	 * Include all daytime running lights: front_left and front_right.
	 */
	RUNNING_LIGHTS,
	/**
	 * Include all parking lights: front_left and front_right.
	 */
	PARKING_LIGHTS,
	/**
	 * Include all brake lights: rear_left and rear_right.
	 */
	BRAKE_LIGHTS,
	REAR_REVERSING_LIGHTS,
	SIDE_MARKER_LIGHTS,

	/**
	 * Include all left turn signal lights: front_left, rear_left, left_side and mirror_mounted.
	 */
	LEFT_TURN_LIGHTS,
	/**
	 * Include all right turn signal lights: front_right, rear_right, right_side and mirror_mounted.
	 */
	RIGHT_TURN_LIGHTS,
	/**
	 * Include all hazard lights: front_left, front_right, rear_left and rear_right.
	 */
	HAZARD_LIGHTS,

	/*Interior Lights by common function groups*/

	AMBIENT_LIGHTS,
	OVERHEAD_LIGHTS,
	READING_LIGHTS,
	TRUNK_LIGHTS,


	/*Lights by location*/

	/**
	 * Include exterior lights located in front of the vehicle. For example, fog lights and low beams.
	 */
	EXTERIOR_FRONT_LIGHTS,
	/**
	 * Include exterior lights located at the back of the vehicle. For example, license plate lights, reverse lights, cargo lights, bed lights an trailer assist lights.
	 */
	EXTERIOR_REAR_LIGHTS,
	/**
	 * Include exterior lights located at the left side of the vehicle. For example, left puddle lights and spot lights.
	 */
	EXTERIOR_LEFT_LIGHTS,
	/**
	 * Include exterior lights located at the right side of the vehicle. For example, right puddle lights and spot lights.
	 */
	EXTERIOR_RIGHT_LIGHTS,
	;

	public static LightName valueForString(String value) {
		try {
			return valueOf(value);
		} catch (Exception e) {
			return null;
		}
	}
}
