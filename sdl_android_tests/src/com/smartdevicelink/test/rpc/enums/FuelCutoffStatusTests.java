package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.FuelCutoffStatus;

public class FuelCutoffStatusTests extends TestCase {

	public void testValidEnums () {	
		String example = "TERMINATE_FUEL";
		FuelCutoffStatus enumTerminateFuel = FuelCutoffStatus.valueForString(example);
		example = "NORMAL_OPERATION";
		FuelCutoffStatus enumNormalOperation = FuelCutoffStatus.valueForString(example);
		example = "FAULT";
		FuelCutoffStatus enumFault = FuelCutoffStatus.valueForString(example);
		
		assertNotNull("TERMINATE_FUEL returned null", enumTerminateFuel);
		assertNotNull("NORMAL_OPERATION returned null", enumNormalOperation);
		assertNotNull("FAULT returned null", enumFault);
	}
	
	public void testInvalidEnum () {
		String example = "tErmINAte_FueL";
		try {
		    FuelCutoffStatus temp = FuelCutoffStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    FuelCutoffStatus temp = FuelCutoffStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<FuelCutoffStatus> enumValueList = Arrays.asList(FuelCutoffStatus.values());

		List<FuelCutoffStatus> enumTestList = new ArrayList<FuelCutoffStatus>();
		enumTestList.add(FuelCutoffStatus.TERMINATE_FUEL);
		enumTestList.add(FuelCutoffStatus.NORMAL_OPERATION);
		enumTestList.add(FuelCutoffStatus.FAULT);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
