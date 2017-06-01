package com.smartdevicelink.test.rpc.enums;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.smartdevicelink.R;
import com.smartdevicelink.proxy.rpc.enums.AmbientLightStatus;

/**
 * This is a unit test class for the SmartDeviceLink library project class : 
 * {@link com.smartdevicelink.rpc.enums.AmbientLightStatus}
 */
public class AmbientLightStatusTests extends AndroidTestCase {

	/**
	 * Verifies that the enum values are not null upon valid assignment.
	 */
	public void testValidEnums () {	
		
		String example = mContext.getString(R.string.day_caps);
		AmbientLightStatus enumDay = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.night_caps);
		AmbientLightStatus enumNight = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.unknown_caps);
		AmbientLightStatus enumUnknown = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.invalid_caps);
		AmbientLightStatus enumInvalid = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.twilight_one_caps);
		AmbientLightStatus enumTwilight1 = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.twilight_two_caps);
		AmbientLightStatus enumTwilight2 = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.twilight_three_caps);
		AmbientLightStatus enumTwilight3 = AmbientLightStatus.valueForString(example);
		example = mContext.getString(R.string.twilight_four_caps);
		AmbientLightStatus enumTwilight4 = AmbientLightStatus.valueForString(example);
			
		assertNotNull("DAY returned null", enumDay);
		assertNotNull("NIGHT returned null", enumNight);
		assertNotNull("UNKNOWN returned null", enumUnknown);
		assertNotNull("INVALID returned null", enumInvalid);
		assertNotNull("TWILIGHT_1 returned null", enumTwilight1);
		assertNotNull("TWILIGHT_2 returned null", enumTwilight2);
		assertNotNull("TWILIGHT_3 returned null", enumTwilight3);
		assertNotNull("TWILIGHT_4 returned null", enumTwilight4);		
	}	
	
	/**
	 * Verifies that an invalid assignment is null.
	 */
	public void testInvalidEnum () {
		String example = mContext.getString(R.string.invalid_enum);
		try {
		    AmbientLightStatus temp = AmbientLightStatus.valueForString(example);
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
		    AmbientLightStatus temp = AmbientLightStatus.valueForString(example);
            assertNull(mContext.getString(R.string.result_of_valuestring_should_be_null), temp);
		}
		catch (NullPointerException exception) {
            fail(mContext.getString(R.string.invalid_enum_throws_illegal_argument_exception));
		}
	}
	
	/**
	 * Verifies the possible enum values of AmbientLightStatus.
	 */
	public void testListEnum() {
 		List<AmbientLightStatus> enumValueList = Arrays.asList(AmbientLightStatus.values()); 		
		List<AmbientLightStatus> enumTestList = new ArrayList<AmbientLightStatus>();
		
		enumTestList.add(AmbientLightStatus.DAY);
		enumTestList.add(AmbientLightStatus.NIGHT);
		enumTestList.add(AmbientLightStatus.UNKNOWN);		
		enumTestList.add(AmbientLightStatus.INVALID);
		enumTestList.add(AmbientLightStatus.TWILIGHT_1);
		enumTestList.add(AmbientLightStatus.TWILIGHT_2);
		enumTestList.add(AmbientLightStatus.TWILIGHT_3);
		enumTestList.add(AmbientLightStatus.TWILIGHT_4);

		assertTrue("Enum value list does not match enum class list.", 
					enumValueList.containsAll(enumTestList) && 
					enumTestList.containsAll(enumValueList));
	}
}