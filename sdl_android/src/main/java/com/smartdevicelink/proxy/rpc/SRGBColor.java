package com.smartdevicelink.proxy.rpc;

import android.support.annotation.NonNull;

import com.smartdevicelink.proxy.RPCStruct;

import java.util.Hashtable;

public class SRGBColor extends RPCStruct {
	public static final String KEY_RED = "red";
	public static final String KEY_GREEN = "green";
	public static final String KEY_BLUE = "blue";

	/**
	 * Constructs a newly allocated SRGBColor object
	 */
	public SRGBColor() {
	}

	/**
	 * Constructs a newly allocated SRGBColor object indicated by the Hashtable parameter
	 * @param hash The Hashtable to use
	 */
	public SRGBColor(Hashtable<String, Object> hash) {
		super(hash);
	}

	/**
	 * Constructs a newly allocated SRGBColor object
	 * @param red   Min: 0  Max: 255
	 * @param green Min: 0  Max: 255
	 * @param blue  Min: 0  Max: 255
	 */
	public SRGBColor(@NonNull Integer red, @NonNull Integer green, @NonNull Integer blue) {
		this();
		setRed(red);
		setGreen(green);
		setBlue(blue);
	}

	/**
	 * Sets the red portion of the SRGBColor class
	 *
	 * @param red
	 */
	public void setRed(@NonNull Integer red) {
		setValue(KEY_RED, red);
	}

	/**
	 * Gets the red portion of the SRGBColor class
	 *
	 * @return Integer
	 */
	public Integer getRed() {
		return getInteger(KEY_RED);
	}

	/**
	 * Sets the green portion of the SRGBColor class
	 *
	 * @param green
	 */
	public void setGreen(@NonNull Integer green) {
		setValue(KEY_GREEN, green);
	}

	/**
	 * Gets the green portion of the SRGBColor class
	 *
	 * @return Integer
	 */
	public Integer getGreen() {
		return getInteger(KEY_GREEN);
	}

	/**
	 * Sets the blue portion of the SRGBColor class
	 *
	 * @param blue
	 */
	public void setBlue(@NonNull Integer blue) {
		setValue(KEY_BLUE, blue);
	}

	/**
	 * Gets the blue portion of the SRGBColor class
	 *
	 * @return Integer
	 */
	public Integer getBlue() {
		return getInteger(KEY_BLUE);
	}

}
