package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.PermissionStatus;

public class PermissionStatusTests extends TestCase {

	public void testValidEnums () {	
		String example = "ALLOWED";
		PermissionStatus enumAllowed = PermissionStatus.valueForString(example);
		example = "DISALLOWED";
		PermissionStatus enumDisallowed = PermissionStatus.valueForString(example);
		example = "USER_DISALLOWED";
		PermissionStatus enumUserDisallowed = PermissionStatus.valueForString(example);
		example = "USER_CONSENT_PENDING";
		PermissionStatus enumUserConsentPending = PermissionStatus.valueForString(example);
		
		assertNotNull("ALLOWED returned null", enumAllowed);
		assertNotNull("DISALLOWED returned null", enumDisallowed);
		assertNotNull("USER_DISALLOWED returned null", enumUserDisallowed);
		assertNotNull("USER_CONSENT_PENDING returned null", enumUserConsentPending);
	}
	
	public void testInvalidEnum () {
		String example = "aLloWed";
		try {
		    PermissionStatus temp = PermissionStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    PermissionStatus temp = PermissionStatus.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<PermissionStatus> enumValueList = Arrays.asList(PermissionStatus.values());

		List<PermissionStatus> enumTestList = new ArrayList<PermissionStatus>();
		enumTestList.add(PermissionStatus.ALLOWED);
		enumTestList.add(PermissionStatus.DISALLOWED);
		enumTestList.add(PermissionStatus.USER_DISALLOWED);
		enumTestList.add(PermissionStatus.USER_CONSENT_PENDING);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
