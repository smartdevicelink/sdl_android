package com.smartdevicelink.transport;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.test.ext.junit.runners.AndroidJUnit4;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MultiplexTransportTest {

	RouterServiceValidator rsvp;

	
	@Before
	public void setUp() throws Exception {
		rsvp = new RouterServiceValidator(getInstrumentation().getTargetContext());
		rsvp.setFlags(RouterServiceValidator.FLAG_DEBUG_NONE);
	}
	// test for setting error state.
	@Test
	public void testSetState() {
		MultiplexBluetoothTransport btTransport = new MultiplexBluetoothTransport(new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message message) {
				assertNotNull(message);
				if (message.arg1 == MultiplexBaseTransport.STATE_ERROR) {
					assertNotNull(message.getData());
					assertEquals(MultiplexBaseTransport.REASON_SPP_ERROR, message.getData().getByte(MultiplexBaseTransport.ERROR_REASON_KEY));
				} else {
					//It will first listen before the error state
					assertEquals(MultiplexBaseTransport.STATE_LISTEN, message.arg1);
				}
			}
		});
		btTransport.start();
		final Bundle bundle = new Bundle();
		bundle.putByte(MultiplexBaseTransport.ERROR_REASON_KEY, MultiplexBaseTransport.REASON_SPP_ERROR);
		btTransport.setState(MultiplexBaseTransport.STATE_ERROR, bundle);
	}
}
