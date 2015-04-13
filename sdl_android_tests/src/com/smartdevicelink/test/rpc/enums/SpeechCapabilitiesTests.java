package com.smartdevicelink.test.rpc.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;

public class SpeechCapabilitiesTests extends TestCase {

	public void testValidEnums () {	
		String example = "TEXT";
		SpeechCapabilities enumText = SpeechCapabilities.valueForString(example);
		example = "SAPI_PHONEMES";
		SpeechCapabilities enumSapiPhonemes = SpeechCapabilities.valueForString(example);
		example = "LHPLUS_PHONEMES";
		SpeechCapabilities enumLhplusPhonemes = SpeechCapabilities.valueForString(example);
		example = "PRE_RECORDED";
		SpeechCapabilities enumPreRecorded = SpeechCapabilities.valueForString(example);
		example = "SILENCE";
		SpeechCapabilities enumSilence = SpeechCapabilities.valueForString(example);
		
		assertNotNull("TEXT returned null", enumText);
		assertNotNull("SAPI_PHONEMES returned null", enumSapiPhonemes);
		assertNotNull("LHPLUS_PHONEMES returned null", enumLhplusPhonemes);
		assertNotNull("PRE_RECORDED returned null", enumPreRecorded);
		assertNotNull("SILENCE returned null", enumSilence);
	}
	
	public void testInvalidEnum () {
		String example = "teXT";
		try {
		    SpeechCapabilities temp = SpeechCapabilities.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (IllegalArgumentException exception) {
            fail("Invalid enum throws IllegalArgumentException.");
		}
	}
	
	public void testNullEnum () {
		String example = null;
		try {
		    SpeechCapabilities temp = SpeechCapabilities.valueForString(example);
            assertNull("Result of valueForString should be null.", temp);
		}
		catch (NullPointerException exception) {
            fail("Null string throws NullPointerException.");
		}
	}	
	
	public void testListEnum() {
 		List<SpeechCapabilities> enumValueList = Arrays.asList(SpeechCapabilities.values());

		List<SpeechCapabilities> enumTestList = new ArrayList<SpeechCapabilities>();
		enumTestList.add(SpeechCapabilities.TEXT);
		enumTestList.add(SpeechCapabilities.SAPI_PHONEMES);
		enumTestList.add(SpeechCapabilities.LHPLUS_PHONEMES);
		enumTestList.add(SpeechCapabilities.PRE_RECORDED);
		enumTestList.add(SpeechCapabilities.SILENCE);

		assertTrue("Enum value list does not match enum class list", 
				enumValueList.containsAll(enumTestList) && enumTestList.containsAll(enumValueList));
	}
	
}
