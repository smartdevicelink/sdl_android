package com.smartdevicelink.test.rpc.enums;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.smartdevicelink.R;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;

/**
 * This is a unit test class for the SmartDeviceLink library project class : 
 * {@link com.smartdevicelink.rpc.enums.HmiLevel}
 */
public class HmiLevelTests extends AndroidTestCase {

	/**
	 * Verifies that the enum values are not null upon valid assignment.
	 */
	public void testValidEnums () {	
		String example = mContext.getString(R.string.full_caps);
		HMILevel enumFull = HMILevel.valueForString(example);
		example = mContext.getString(R.string.limited_caps);
		HMILevel enumLimited = HMILevel.valueForString(example);
		example = mContext.getString(R.string.background_caps);
		HMILevel enumBackground = HMILevel.valueForString(example);
		example = mContext.getString(R.string.none_caps);
		HMILevel enumNone = HMILevel.valueForString(example);
		
		assertNotNull("FULL returned null", enumFull);
		assertNotNull("LIMITED returned null", enumLimited);
		assertNotNull("BACKGROUND returned null", enumBackground);
		assertNotNull("NONE returned null", enumNone);
	}

	/**
	 * Verifies that an invalid assignment is null.
	 */
	public void testInvalidEnum () {
		String example = mContext.getString(R.string.invalid_enum);
		try {
		    HMILevel temp = HMILevel.valueForString(example);
            assertNull(mContext.getString(R.string.result_of_valuestring_should_be_null), temp);
		}
		catch (IllegalArgumentException exception) {
            fail(mContext.getString(R.string.invalid_enum_throws_illegal_argument_exception));
		}
	}

	/**
	 * Verifies that a null assignment is invalid.
	 */
	public void testNullEnum () {
		String example = null;
		try {
		    HMILevel temp = HMILevel.valueForString(example);
            assertNull(mContext.getString(R.string.result_of_valuestring_should_be_null), temp);
		}
		catch (NullPointerException exception) {
            fail(mContext.getString(R.string.invalid_enum_throws_illegal_argument_exception));
		}
	}

	/**
	 * Verifies the possible enum values of HMILevel.
	 */
	public void testListEnum() {
 		List<HMILevel> enumValueList = Arrays.asList(HMILevel.values());

		List<HMILevel> enumTestList = new ArrayList<HMILevel>();
		enumTestList.add(HMILevel.HMI_FULL);
		enumTestList.add(HMILevel.HMI_LIMITED);
		enumTestList.add(HMILevel.HMI_BACKGROUND);
		enumTestList.add(HMILevel.HMI_NONE);
		
		assertTrue(mContext.getString(R.string.enum_value_list_does_not_match_enum_class_list),
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}	
}