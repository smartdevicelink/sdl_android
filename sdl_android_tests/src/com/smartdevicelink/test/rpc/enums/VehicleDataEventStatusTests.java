package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.VehicleDataEventStatus;

public class VehicleDataEventStatusTests extends TestCase {

	public void testValidEnums () {	
		String example = "NO_EVENT";
		VehicleDataEventStatus enumNoEvent = VehicleDataEventStatus.valueForString(example);
		example = "NO";
		VehicleDataEventStatus enumNo = VehicleDataEventStatus.valueForString(example);
		example = "YES";
		VehicleDataEventStatus enumYes = VehicleDataEventStatus.valueForString(example);
		example = "NOT_SUPPORTED";
		VehicleDataEventStatus enumNotSupported = VehicleDataEventStatus.valueForString(example);
		example = "FAULT";
		VehicleDataEventStatus enumFault = VehicleDataEventStatus.valueForString(example);
		
		assertNotNull("NO_EVENT returned null", enumNoEvent);
		assertNotNull("NO returned null", enumNo);
		assertNotNull("YES returned null", enumYes);
		assertNotNull("NOT_SUPPORTED returned null", enumNotSupported);
		assertNotNull("FAULT returned null", enumFault);
	}
	
	public void testInvalidEnum () {
		String example = "no_EveNT";
		try {
		    VehicleDataEventStatus temp = VehicleDataEventStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    VehicleDataEventStatus temp = VehicleDataEventStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<VehicleDataEventStatus> enumValueList = Arrays.asList(VehicleDataEventStatus.values());

		List<VehicleDataEventStatus> enumTestList = new ArrayList<VehicleDataEventStatus>();
		enumTestList.add(VehicleDataEventStatus.NO_EVENT);
		enumTestList.add(VehicleDataEventStatus.NO);
		enumTestList.add(VehicleDataEventStatus.YES);
		enumTestList.add(VehicleDataEventStatus.NOT_SUPPORTED);
		enumTestList.add(VehicleDataEventStatus.FAULT);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
