package com.smartdevicelink.api;

import android.content.Context;
import android.test.AndroidTestCase;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.SdlProxyBase;
import com.smartdevicelink.proxy.rpc.GetVehicleData;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.TemplateColorScheme;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.Result;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.test.Test;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * This is a unit test class for the SmartDeviceLink library manager class :
 * {@link com.smartdevicelink.api.SdlManager}
 */
public class SdlManagerTests extends AndroidTestCase {

	public static BaseTransportConfig transport = null;
	private Context mTestContext;
	private Vector<AppHMIType> appType;
	private TemplateColorScheme templateColorScheme;
	private int listenerCalledCounter;
	private SdlManager sdlManager;
	private SdlProxyBase sdlProxyBase;

	// transport related
	@SuppressWarnings("FieldCanBeLocal")
	private int TCP_PORT = 12345;
	@SuppressWarnings("FieldCanBeLocal")
	private String DEV_MACHINE_IP_ADDRESS = "0.0.0.0";

	@Override
	public void setUp() throws Exception{
		super.setUp();

		// set transport
		transport = new TCPTransportConfig(TCP_PORT, DEV_MACHINE_IP_ADDRESS, true);

		// add AppTypes
		appType = new Vector<>();
		appType.add(AppHMIType.DEFAULT);

		// Color Scheme
		templateColorScheme = new TemplateColorScheme();
		templateColorScheme.setBackgroundColor(Test.GENERAL_RGBCOLOR);
		templateColorScheme.setPrimaryColor(Test.GENERAL_RGBCOLOR);
		templateColorScheme.setSecondaryColor(Test.GENERAL_RGBCOLOR);

		sdlManager = createSampleManager("heyApp", "123456");
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	// SETUP / HELPERS

	private Context getTestContext() {
		return mTestContext;
	}

	private SdlManager createSampleManager(String appName, String appId){
		SdlManager manager;

		// build manager object - use all setters, will test using getters below
		SdlManager.Builder builder = new SdlManager.Builder();
		builder.setAppId(appId);
		builder.setAppName(appName);
		builder.setShortAppName(appName);
		builder.setAppTypes(appType);
		builder.setTransportType(transport);
		builder.setContext(getTestContext());
		builder.setLanguage(Language.EN_US);
		builder.setDayColorScheme(templateColorScheme);
		builder.setNightColorScheme(templateColorScheme);
		builder.setVrSynonyms(Test.GENERAL_VECTOR_STRING);
		builder.setTtsName(Test.GENERAL_VECTOR_TTS_CHUNKS);
		manager = builder.build();

		// mock SdlProxyBase and set it manually
		sdlProxyBase = mock(SdlProxyBase.class);
		manager.setProxy(sdlProxyBase);

		return manager;
	}

	// TESTS

	public void testNotNull(){
		assertNotNull(createSampleManager("app","123456"));
	}

	public void testMissingAppName() {
		try {
			createSampleManager(null,"123456");
		} catch (IllegalArgumentException ex) {
			assertSame(ex.getMessage(), "You must specify an app name by calling setAppName");
		}
	}

	public void testMissingAppId() {
		try {
			createSampleManager("app",null);
		} catch (IllegalArgumentException ex) {
			assertSame(ex.getMessage(), "You must specify an app ID by calling setAppId");
		}
	}

	public void testManagerSetters() {
		assertEquals("123456", sdlManager.getAppId());
		assertEquals("heyApp", sdlManager.getAppName());
		assertEquals("heyApp", sdlManager.getShortAppName());
		assertEquals(appType, sdlManager.getAppTypes());
		assertEquals(Language.EN_US, sdlManager.getHmiLanguage());
		assertEquals(transport, sdlManager.getTransport());
		assertEquals(templateColorScheme, sdlManager.getDayColorScheme());
		assertEquals(templateColorScheme, sdlManager.getNightColorScheme());
		assertEquals(Test.GENERAL_VECTOR_STRING, sdlManager.getVrSynonyms());
		assertEquals(Test.GENERAL_VECTOR_TTS_CHUNKS, sdlManager.getTtsChunks());
	}

	public void testStartingManager(){
		listenerCalledCounter = 0;

		sdlManager.start(new ManagerListener() {
			@Override
			public void onStart(boolean success) {
				assertTrue(success);
				listenerCalledCounter++;
			}

			@Override
			public void onDestroy() {

			}
		});

		// Create and force all sub managers to be ready manually. Because SdlManager will not start until all sub managers are ready.
		// Note : SdlManager.initialize() will not be called automatically by proxy as in real life because we have mock proxy not a real one
		sdlManager.initialize();
		sdlManager.setState(BaseSubManager.READY);
		// manager.getVideoStreamingManager().transitionToState(BaseSubManager.READY);
		// manager.getAudioStreamManager().transitionToState(BaseSubManager.READY);
		// manager.getLockScreenManager().transitionToState(BaseSubManager.READY);
		// manager.getScreenManager().transitionToState(BaseSubManager.READY);
		sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
		sdlManager.getFileManager().transitionToState(BaseSubManager.READY);

		// Make sure the listener is called exactly once
		assertEquals("Listener was not called or called more/less frequently than expected", listenerCalledCounter, 1);
	}

	public void testSendRPC(){
		listenerCalledCounter = 0;

		// When sdlProxyBase.sendRPCRequest() is called, create a fake success response
		Answer<Void> answer = new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				RPCRequest request = (RPCRequest) args[0];
				RPCResponse response = new RPCResponse(FunctionID.GET_VEHICLE_DATA.toString());
				response.setSuccess(true);
				request.getOnRPCResponseListener().onResponse(0, response);
				return null;
			}
		};
		try {
			doAnswer(answer).when(sdlProxyBase).sendRPCRequest(any(RPCRequest.class));
		} catch (SdlException e) {
			e.printStackTrace();
		}


		// Test send RPC request
		final GetVehicleData request = new GetVehicleData();
		request.setGps(true);
		request.setOnRPCResponseListener(new OnRPCResponseListener() {
			@Override
			public void onResponse(int correlationId, RPCResponse response) {
				assertTrue(response.getSuccess());
				listenerCalledCounter++;
			}
		});
		try {
			sdlManager.sendRPC(request);
		} catch (SdlException e) {
			e.printStackTrace();
		}

		// Make sure the listener is called exactly once
		assertEquals("Listener was not called or called more/less frequently than expected", listenerCalledCounter, 1);
	}

	public void testSendRPCs(){
		testSendMultipleRPCs(false);
	}

	public void testSendSequentialRPCs(){
		testSendMultipleRPCs(true);
	}

	private void testSendMultipleRPCs(boolean sequentialSend){
		listenerCalledCounter = 0;

		// When sdlProxyBase.sendRPCRequests() is called, call listener.onFinished() to fake the response
		final Answer<Void> answer = new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				OnMultipleRequestListener listener = (OnMultipleRequestListener) args[1];
				listener.onFinished();
				return null;
			}
		};
		try {
			if (sequentialSend){
				doAnswer(answer).when(sdlProxyBase).sendSequentialRequests(any(List.class), any(OnMultipleRequestListener.class));

			} else {
				doAnswer(answer).when(sdlProxyBase).sendRequests(any(List.class), any(OnMultipleRequestListener.class));
			}
		} catch (SdlException e) {
			e.printStackTrace();
		}


		// Test send RPC requests
		List<RPCRequest> rpcsList = Arrays.asList(new GetVehicleData(), new Show());
		OnMultipleRequestListener onMultipleRequestListener = new OnMultipleRequestListener() {
			@Override
			public void onUpdate(int remainingRequests) { }

			@Override
			public void onFinished() {
				listenerCalledCounter++;
			}

			@Override
			public void onError(int correlationId, Result resultCode, String info) {}

			@Override
			public void onResponse(int correlationId, RPCResponse response) {}
		};
		try {
			if (sequentialSend) {
				sdlManager.sendSequentialRPCs(rpcsList, onMultipleRequestListener);
			} else {
				sdlManager.sendRPCs(rpcsList, onMultipleRequestListener);
			}
		} catch (SdlException e) {
			e.printStackTrace();
		}

		// Make sure the listener is called exactly once
		assertEquals("Listener was not called or called more/less frequently than expected", listenerCalledCounter, 1);
	}

}
