package com.smartdevicelink.managers.lockscreen;

import android.content.Context;

import com.smartdevicelink.AndroidTestCase2;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.interfaces.ISdl;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.enums.DriverDistractionState;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.LockScreenStatus;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.test.Test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * This is a unit test class for the SmartDeviceLink library manager class :
 * {@link com.smartdevicelink.managers.lockscreen.LockScreenManager}
 */
public class LockScreenManagerTests extends AndroidTestCase2 {

	private LockScreenManager lockScreenManager;
	private OnRPCNotificationListener onDDListener;

	@Override
	public void setUp() throws Exception{
		super.setUp();

		ISdl internalInterface = mock(ISdl.class);

		Answer<Void> onDDStatusAnswer = new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				onDDListener = (OnRPCNotificationListener) args[1];
				return null;
			}
		};
		doAnswer(onDDStatusAnswer).when(internalInterface).addOnRPCNotificationListener(eq(FunctionID.ON_DRIVER_DISTRACTION), any(OnRPCNotificationListener.class));

		Context context =  getContext();
		// create config
		LockScreenConfig lockScreenConfig = new LockScreenConfig();
		lockScreenConfig.setCustomView(Test.GENERAL_INT);
		lockScreenConfig.setAppIcon(Test.GENERAL_INT);
		lockScreenConfig.setBackgroundColor(Test.GENERAL_INT);
		lockScreenConfig.showDeviceLogo(true);
		lockScreenConfig.setEnabled(true);
		lockScreenConfig.showInOptionalState(true);
		lockScreenConfig.enableDismissGesture(true);

		lockScreenManager = new LockScreenManager(lockScreenConfig, context, internalInterface);
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testVariables() {
		assertEquals(Test.GENERAL_INT, lockScreenManager.customView);
		assertEquals(Test.GENERAL_INT, lockScreenManager.lockScreenIcon);
		assertEquals(Test.GENERAL_INT, lockScreenManager.lockScreenColor);
		assertTrue(lockScreenManager.deviceLogoEnabled);
		assertTrue(lockScreenManager.lockScreenEnabled);
		assertNull(lockScreenManager.deviceLogo);
		assertTrue(lockScreenManager.showInOptionalState);
		assertTrue(lockScreenManager.enableDismissGesture);
	}

	public void testGetLockScreenStatusHmiNoneDDOff(){
		lockScreenManager.driverDistStatus = false;
		lockScreenManager.hmiLevel = HMILevel.HMI_NONE;
		assertEquals(LockScreenStatus.OFF, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiBackgroundDDOff(){
		lockScreenManager.driverDistStatus = false;
		lockScreenManager.hmiLevel = HMILevel.HMI_BACKGROUND;
		assertEquals(LockScreenStatus.OFF, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiNoneDDOn(){
		lockScreenManager.driverDistStatus = true;
		lockScreenManager.hmiLevel = HMILevel.HMI_BACKGROUND;
		assertEquals(LockScreenStatus.REQUIRED, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiFullDDOff(){
		lockScreenManager.driverDistStatus = false;
		lockScreenManager.hmiLevel = HMILevel.HMI_FULL;
		assertEquals(LockScreenStatus.OPTIONAL, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiFullDDOn(){
		lockScreenManager.driverDistStatus = true;
		lockScreenManager.hmiLevel = HMILevel.HMI_FULL;
		assertEquals(LockScreenStatus.REQUIRED, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiLimitedDDOff(){
		lockScreenManager.driverDistStatus = false;
		lockScreenManager.hmiLevel = HMILevel.HMI_LIMITED;
		assertEquals(LockScreenStatus.OPTIONAL, lockScreenManager.getLockScreenStatus());
	}

	public void testGetLockScreenStatusHmiLimitedDDOn(){
		lockScreenManager.driverDistStatus = true;
		lockScreenManager.hmiLevel = HMILevel.HMI_LIMITED;
		assertEquals(LockScreenStatus.REQUIRED, lockScreenManager.getLockScreenStatus());
	}

	public void testLockScreenDismissibleWithEnableTrueAndDismissibilityTrue(){
		lockScreenManager.enableDismissGesture = true;
		OnDriverDistraction onDriverDistraction = new OnDriverDistraction();
		onDriverDistraction.setLockscreenDismissibility(true);
		onDriverDistraction.setState(DriverDistractionState.DD_ON);
		onDDListener.onNotified(onDriverDistraction);
		assertTrue(lockScreenManager.enableDismissGesture);
		assertTrue(lockScreenManager.mIsLockscreenDismissible);
	}

	public void testLockScreenDismissibleWithEnableFalseAndDismissibilityFalse(){
		lockScreenManager.enableDismissGesture = false;
		OnDriverDistraction onDriverDistraction = new OnDriverDistraction();
		onDriverDistraction.setLockscreenDismissibility(true);
		onDriverDistraction.setState(DriverDistractionState.DD_ON);
		onDDListener.onNotified(onDriverDistraction);
		assertFalse(lockScreenManager.enableDismissGesture);
		assertFalse(lockScreenManager.mIsLockscreenDismissible);
	}

	public void testLockScreenDismissibleWithEnableTrueAndDismissibilityFalse(){
		lockScreenManager.enableDismissGesture = true;
		OnDriverDistraction onDriverDistraction = new OnDriverDistraction();
		onDriverDistraction.setLockscreenDismissibility(false);
		onDriverDistraction.setState(DriverDistractionState.DD_ON);
		onDDListener.onNotified(onDriverDistraction);
		assertTrue(lockScreenManager.enableDismissGesture);
		assertFalse(lockScreenManager.mIsLockscreenDismissible);
	}

	public void testLockScreenDismissibleWithEnableFalseAndDismissibilityTrue(){
		lockScreenManager.enableDismissGesture = false;
		OnDriverDistraction onDriverDistraction = new OnDriverDistraction();
		onDriverDistraction.setLockscreenDismissibility(true);
		onDriverDistraction.setState(DriverDistractionState.DD_ON);
		onDDListener.onNotified(onDriverDistraction);
		assertFalse(lockScreenManager.enableDismissGesture);
		assertFalse(lockScreenManager.mIsLockscreenDismissible);
	}

}
