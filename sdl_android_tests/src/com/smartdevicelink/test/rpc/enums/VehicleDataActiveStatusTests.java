package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.VehicleDataActiveStatus;

public class VehicleDataActiveStatusTests extends TestCase {

	public void testValidEnums () {	
		String example = "INACTIVE_NOT_CONFIRMED";
		VehicleDataActiveStatus enumInactiveNotConfirmed = VehicleDataActiveStatus.valueForString(example);
		example = "INACTIVE_CONFIRMED";
		VehicleDataActiveStatus enumInactiveConfirmed = VehicleDataActiveStatus.valueForString(example);
		example = "ACTIVE_NOT_CONFIRMED";
		VehicleDataActiveStatus enumActiveNotConfirmed = VehicleDataActiveStatus.valueForString(example);
		example = "ACTIVE_CONFIRMED";
		VehicleDataActiveStatus enumActiveConfirmed = VehicleDataActiveStatus.valueForString(example);
		example = "FAULT";
		VehicleDataActiveStatus enumFault = VehicleDataActiveStatus.valueForString(example);
		
		assertNotNull("INACTIVE_NOT_CONFIRMED returned null", enumInactiveNotConfirmed);
		assertNotNull("INACTIVE_CONFIRMED returned null", enumInactiveConfirmed);
		assertNotNull("ACTIVE_NOT_CONFIRMED returned null", enumActiveNotConfirmed);
		assertNotNull("ACTIVE_CONFIRMED returned null", enumActiveConfirmed);
		assertNotNull("FAULT returned null", enumFault);
	}
	
	public void testInvalidEnum () {
		String example = "InACtivE_NoT_ConFIRmED";
		try {
		    VehicleDataActiveStatus temp = VehicleDataActiveStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    VehicleDataActiveStatus temp = VehicleDataActiveStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<VehicleDataActiveStatus> enumValueList = Arrays.asList(VehicleDataActiveStatus.values());

		List<VehicleDataActiveStatus> enumTestList = new ArrayList<VehicleDataActiveStatus>();
		enumTestList.add(VehicleDataActiveStatus.INACTIVE_NOT_CONFIRMED);
		enumTestList.add(VehicleDataActiveStatus.INACTIVE_CONFIRMED);
		enumTestList.add(VehicleDataActiveStatus.ACTIVE_NOT_CONFIRMED);
		enumTestList.add(VehicleDataActiveStatus.ACTIVE_CONFIRMED);
		enumTestList.add(VehicleDataActiveStatus.FAULT);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
