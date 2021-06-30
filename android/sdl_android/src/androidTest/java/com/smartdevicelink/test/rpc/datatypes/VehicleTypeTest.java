package com.smartdevicelink.test.rpc.datatypes;

import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.test.JsonUtils;
import com.smartdevicelink.test.TestValues;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

public class VehicleTypeTest extends TestCase {

    private VehicleType msg;

    @Override
    public void setUp() {
        msg = new VehicleType();

        msg.setModel(TestValues.GENERAL_STRING);
        msg.setMake(TestValues.GENERAL_STRING);
        msg.setTrim(TestValues.GENERAL_STRING);
        msg.setModelYear(TestValues.GENERAL_STRING);
    }

    /**
     * Tests the expected values of the RPC message.
     */
    public void testRpcValues() {
        // Test Values
        String year = msg.getModelYear();
        String trim = msg.getTrim();
        String make = msg.getMake();
        String model = msg.getModel();

        // Valid Tests
        assertEquals(TestValues.MATCH, TestValues.GENERAL_STRING, year);
        assertEquals(TestValues.MATCH, TestValues.GENERAL_STRING, model);
        assertEquals(TestValues.MATCH, TestValues.GENERAL_STRING, make);
        assertEquals(TestValues.MATCH, TestValues.GENERAL_STRING, trim);

        // Invalid/Null Tests
        VehicleType msg = new VehicleType();
        assertNotNull(TestValues.NOT_NULL, msg);

        assertNull(TestValues.NULL, msg.getModel());
        assertNull(TestValues.NULL, msg.getMake());
        assertNull(TestValues.NULL, msg.getModelYear());
        assertNull(TestValues.NULL, msg.getTrim());
    }

    public void testJson() {
        JSONObject reference = new JSONObject();

        try {
            reference.put(VehicleType.KEY_MODEL, TestValues.GENERAL_STRING);
            reference.put(VehicleType.KEY_MAKE, TestValues.GENERAL_STRING);
            reference.put(VehicleType.KEY_MODEL_YEAR, TestValues.GENERAL_STRING);
            reference.put(VehicleType.KEY_TRIM, TestValues.GENERAL_STRING);

            JSONObject underTest = msg.serializeJSON();
            assertEquals(TestValues.MATCH, reference.length(), underTest.length());

            Iterator<?> iterator = reference.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                assertEquals(TestValues.MATCH, JsonUtils.readObjectFromJsonObject(reference, key), JsonUtils.readObjectFromJsonObject(underTest, key));
            }
        } catch (JSONException e) {
            fail(TestValues.JSON_FAIL);
        }
    }

    public void testHashMapConstructor(){
        Hashtable<String, Object> store = msg.getStore();
        HashMap<String, Object> hashMap = new HashMap(store);
        VehicleType type = new VehicleType(hashMap);

        Assert.assertEquals(type.getMake(), msg.getMake());
        Assert.assertEquals(type.getModel(), msg.getModel());
        Assert.assertEquals(type.getModelYear(), msg.getModelYear());
        Assert.assertEquals(type.getTrim(), msg.getTrim());
    }
}