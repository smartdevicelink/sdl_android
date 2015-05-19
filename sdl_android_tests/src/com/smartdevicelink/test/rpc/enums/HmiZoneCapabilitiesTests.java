package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.HmiZoneCapabilities;

public class HmiZoneCapabilitiesTests extends TestCase {

	public void testValidEnums () {	
		String example = "FRONT";
		HmiZoneCapabilities enumFront = HmiZoneCapabilities.valueForString(example);
		example = "BACK";
		HmiZoneCapabilities enumBack = HmiZoneCapabilities.valueForString(example);
		
		assertNotNull("FRONT returned null", enumFront);
		assertNotNull("BACK returned null", enumBack);
	}
	
	public void testInvalidEnum () {
		String example = "fROnT";
		try {
		    HmiZoneCapabilities temp = HmiZoneCapabilities.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    HmiZoneCapabilities temp = HmiZoneCapabilities.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<HmiZoneCapabilities> enumValueList = Arrays.asList(HmiZoneCapabilities.values());

		List<HmiZoneCapabilities> enumTestList = new ArrayList<HmiZoneCapabilities>();
		enumTestList.add(HmiZoneCapabilities.FRONT);
		enumTestList.add(HmiZoneCapabilities.BACK);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
