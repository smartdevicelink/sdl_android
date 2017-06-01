package com.smartdevicelink.test.rpc.enums;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.smartdevicelink.R;
import com.smartdevicelink.proxy.rpc.enums.CompassDirection;

/**
 * This is a unit test class for the SmartDeviceLink library project class : 
 * {@link com.smartdevicelink.rpc.enums.CompassDirection}
 */
public class CompassDirectionTests extends AndroidTestCase {

	/**
	 * Verifies that the enum values are not null upon valid assignment.
	 */
	public void testValidEnums () {	
		String example = mContext.getString(R.string.north_caps);
		CompassDirection enumNorth = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.northwest_caps);
		CompassDirection enumNorthWest = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.west_caps);
		CompassDirection enumWest = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.southwest_caps);
		CompassDirection enumSouthWest = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.south_caps);
		CompassDirection enumSouth = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.southeast_caps);
		CompassDirection enumSouthEast = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.east_caps);
		CompassDirection enumEast = CompassDirection.valueForString(example);
		example = mContext.getString(R.string.northeast_caps);
		CompassDirection enumNorthEast = CompassDirection.valueForString(example);
		
		assertNotNull("NORTH returned null", enumNorth);
		assertNotNull("NORTHWEST returned null", enumNorthWest);
		assertNotNull("WEST returned null", enumWest);
		assertNotNull("SOUTHWEST returned null", enumSouthWest);
		assertNotNull("SOUTH returned null", enumSouth);
		assertNotNull("SOUTHEAST returned null", enumSouthEast);
		assertNotNull("EAST returned null", enumEast);
		assertNotNull("NORTHEAST returned null", enumNorthEast);
	}

	/**
	 * Verifies that an invalid assignment is null.
	 */
	public void testInvalidEnum () {
		String example = mContext.getString(R.string.invalid_enum);
		try {
		    CompassDirection temp = CompassDirection.valueForString(example);
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
		    CompassDirection temp = CompassDirection.valueForString(example);
            assertNull(mContext.getString(R.string.result_of_valuestring_should_be_null), temp);
		}
		catch (NullPointerException exception) {
            fail(mContext.getString(R.string.invalid_enum_throws_illegal_argument_exception));
		}
	}	

	/**
	 * Verifies the possible enum values of CompassDirection.
	 */
	public void testListEnum() {
 		List<CompassDirection> enumValueList = Arrays.asList(CompassDirection.values());

		List<CompassDirection> enumTestList = new ArrayList<CompassDirection>();
		enumTestList.add(CompassDirection.NORTH);
		enumTestList.add(CompassDirection.NORTHWEST);
		enumTestList.add(CompassDirection.WEST);
		enumTestList.add(CompassDirection.SOUTHWEST);
		enumTestList.add(CompassDirection.SOUTH);
		enumTestList.add(CompassDirection.SOUTHEAST);		
		enumTestList.add(CompassDirection.EAST);
		enumTestList.add(CompassDirection.NORTHEAST);	

		assertTrue(mContext.getString(R.string.enum_value_list_does_not_match_enum_class_list),
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}	
}