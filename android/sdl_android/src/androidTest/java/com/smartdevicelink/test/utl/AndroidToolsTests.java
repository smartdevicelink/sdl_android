package com.smartdevicelink.test.utl;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smartdevicelink.proxy.rpc.VehicleType;
import com.smartdevicelink.util.AndroidTools;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class AndroidToolsTests {

    @Test
    public void testIsServiceExportedNormal() {

        try {
            AndroidTools.isServiceExported(getInstrumentation().getTargetContext(), new ComponentName(getInstrumentation().getTargetContext(), "test"));
        } catch (Exception e) {
            Assert.fail("Exception during normal test: " + e.getMessage());
        }

    }

    @Test
    public void testIsServiceExportedNull() {

        try {
            AndroidTools.isServiceExported(getInstrumentation().getTargetContext(), null);
            Assert.fail("Proccessed null data");
        } catch (Exception e) {

        }

    }

    @Test
    public void testVehicleTypeSave() throws JSONException {
        Context mMockContext = mock(Context.class);
        VehicleType mMockVehicleType = new VehicleType();
        String mAddress = "1234";

        mMockVehicleType.setMake("Ford");
        mMockVehicleType.setTrim("GT");
        mMockVehicleType.setModel("Mustang");
        mMockVehicleType.setModelYear("2019");

        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(editor.commit()).thenReturn(true);

        SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        when(mMockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);
        when(sharedPrefs.edit()).thenReturn(editor);
        when(sharedPrefs.getString(mAddress, null)).thenReturn(mMockVehicleType.serializeJSON().toString());

        AndroidTools.saveVehicleType(mMockContext, mMockVehicleType, mAddress);
        VehicleType type = new VehicleType(AndroidTools.getVehicleTypeFromPrefs(mMockContext, mAddress));

        org.junit.Assert.assertEquals(type.getMake(), mMockVehicleType.getMake());
        org.junit.Assert.assertEquals(type.getModel(), mMockVehicleType.getModel());
        org.junit.Assert.assertEquals(type.getModelYear(), mMockVehicleType.getModelYear());
        org.junit.Assert.assertEquals(type.getTrim(), mMockVehicleType.getTrim());
    }

}
