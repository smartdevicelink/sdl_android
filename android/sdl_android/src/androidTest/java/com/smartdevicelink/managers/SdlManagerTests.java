package com.smartdevicelink.managers;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.livio.taskmaster.Taskmaster;
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.permission.PermissionManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.GetAppServiceDataResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleData;
import com.smartdevicelink.proxy.rpc.OnAppServiceData;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.TemplateColorScheme;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.listeners.OnMultipleRequestListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.test.TestValues;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;
import com.smartdevicelink.util.SystemInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is a unit test class for the SmartDeviceLink library manager class :
 * {@link com.smartdevicelink.managers.SdlManager}
 */
@RunWith(AndroidJUnit4.class)
public class SdlManagerTests {

    public static BaseTransportConfig transport = null;
    private Context mTestContext;
    private Vector<AppHMIType> appType;
    private TemplateColorScheme templateColorScheme;
    private int listenerCalledCounter;
    private SdlManager sdlManager;
    private ISdl internalInterface;

    // transport related
    @SuppressWarnings("FieldCanBeLocal")
    private int TCP_PORT = 12345;
    @SuppressWarnings("FieldCanBeLocal")
    private String DEV_MACHINE_IP_ADDRESS = "0.0.0.0";

    @Before
    public void setUp() throws Exception {
        mTestContext = Mockito.mock(Context.class);

        // set transport
        transport = new TCPTransportConfig(TCP_PORT, DEV_MACHINE_IP_ADDRESS, true);

        // add AppTypes
        appType = new Vector<>();
        appType.add(AppHMIType.DEFAULT);

        // Color Scheme
        templateColorScheme = new TemplateColorScheme();
        templateColorScheme.setBackgroundColor(TestValues.GENERAL_RGBCOLOR);
        templateColorScheme.setPrimaryColor(TestValues.GENERAL_RGBCOLOR);
        templateColorScheme.setSecondaryColor(TestValues.GENERAL_RGBCOLOR);

        sdlManager = createSampleManager("heyApp", "123456", TestValues.GENERAL_LOCKSCREENCONFIG);
    }

    // SETUP / HELPERS

    private Context getTestContext() {
        return mTestContext;
    }

    private SdlManager createSampleManager(String appName, String appId, LockScreenConfig lockScreenConfig) {
        SdlManager manager;

        SdlManagerListener listener = new SdlManagerListener() {
            @Override
            public void onStart() {
                listenerCalledCounter++;
            }

            @Override
            public void onDestroy() {

            }

            @Override
            public void onError(SdlManager manager, String info, Exception e) {

            }

            @Override
            public LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language, Language hmiLanguage) {
                return null;
            }

            @Override
            public boolean onSystemInfoReceived(SystemInfo systemInfo) {
                return true;
            }
        };

        // build manager object - use all setters, will test using getters below
        SdlManager.Builder builder = new SdlManager.Builder(getTestContext(), appId, appName, listener);
        builder.setShortAppName(appName);
        builder.setAppTypes(appType);
        builder.setTransportType(transport);
        builder.setLanguage(Language.EN_US);
        builder.setDayColorScheme(templateColorScheme);
        builder.setNightColorScheme(templateColorScheme);
        builder.setVrSynonyms(TestValues.GENERAL_VECTOR_STRING);
        builder.setTtsName(TestValues.GENERAL_VECTOR_TTS_CHUNKS);
        builder.setLockScreenConfig(lockScreenConfig);
        builder.setMinimumProtocolVersion(TestValues.GENERAL_VERSION);
        builder.setMinimumRPCVersion(TestValues.GENERAL_VERSION);
        builder.setContext(mTestContext);
        manager = builder.build();

        // mock internalInterface and set it manually
        internalInterface = mock(ISdl.class);
        manager.set_internalInterface(internalInterface);
        PermissionManager permissionManager = mock(PermissionManager.class);

        when(internalInterface.getPermissionManager()).thenReturn(permissionManager);
        when(internalInterface.getTaskmaster()).thenReturn(new Taskmaster.Builder().build());
        manager._internalInterface = internalInterface;

        return manager;
    }

    // TESTS

    @Test
    public void testNotNull() {
        assertNotNull(createSampleManager("app", "123456", TestValues.GENERAL_LOCKSCREENCONFIG));
    }

    @Test
    public void testMissingAppName() {
        try {
            createSampleManager(null, "123456", TestValues.GENERAL_LOCKSCREENCONFIG);
        } catch (IllegalArgumentException ex) {
            assertSame(ex.getMessage(), "You must specify an app name by calling setAppName");
        }
    }

    @Test
    public void testMissingAppId() {
        try {
            createSampleManager("app", null, TestValues.GENERAL_LOCKSCREENCONFIG);
        } catch (IllegalArgumentException ex) {
            assertSame(ex.getMessage(), "You must specify an app ID by calling setAppId");
        }
    }

    @Test
    public void testManagerSetters() {
        assertEquals("123456", sdlManager.getAppId());
        assertEquals("heyApp", sdlManager.getAppName());
        assertEquals("heyApp", sdlManager.getShortAppName());
        assertEquals(appType, sdlManager.getAppTypes());
        assertEquals(Language.EN_US, sdlManager.getHmiLanguage());
        assertEquals(Language.EN_US, sdlManager.getLanguage());
        assertEquals(transport, sdlManager.getTransport());
        assertEquals(templateColorScheme, sdlManager.getDayColorScheme());
        assertEquals(templateColorScheme, sdlManager.getNightColorScheme());
        assertEquals(TestValues.GENERAL_VECTOR_STRING, sdlManager.getVrSynonyms());
        assertEquals(TestValues.GENERAL_VECTOR_TTS_CHUNKS, sdlManager.getTtsChunks());
        assertEquals(TestValues.GENERAL_LOCKSCREENCONFIG, sdlManager.getLockScreenConfig());
        assertEquals(TestValues.GENERAL_VERSION, sdlManager.getMinimumProtocolVersion());
        assertEquals(TestValues.GENERAL_VERSION, sdlManager.getMinimumRPCVersion());
    }

    @Test
    public void testStartingManager() {
        listenerCalledCounter = 0;

        try {
            sdlManager.start();
        } catch (Exception e) {
        }

        // Create and force all sub managers to be ready manually. Because SdlManager will not start until all sub managers are ready.
        // Note: SdlManager.initialize() will not be called automatically by proxy as in real life because we have mock proxy not a real one
        sdlManager.initialize();

        // Set all sub managers' states to ready
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.READY);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.READY);

        // Make sure the listener is called exactly once
        assertEquals("Listener was not called or called more/less frequently than expected", 1, listenerCalledCounter);
    }

    @Test
    public void testManagerStates() {
        SdlManager sdlManager = createSampleManager("test", "00000", new LockScreenConfig());
        sdlManager.initialize();


        // Case 1-A:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.READY);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.checkState();
        assertEquals(BaseSubManager.READY, sdlManager.getState());


        // Case 1-B:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.READY);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_NEVER);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.checkState();
        assertEquals(BaseSubManager.READY, sdlManager.getState());


        // Case 2-A:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getFileManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.checkState();
        assertEquals(BaseSubManager.ERROR, sdlManager.getState());


        // Case 1-B:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getFileManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_NEVER);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.checkState();
        assertEquals(BaseSubManager.ERROR, sdlManager.getState());


        // Case 3-A:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getFileManager().transitionToState(BaseSubManager.READY);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.LIMITED);
        sdlManager.checkState();
        assertEquals(BaseSubManager.SETTING_UP, sdlManager.getState());


        // Case 3-B:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getFileManager().transitionToState(BaseSubManager.READY);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_NEVER);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.checkState();
        assertEquals(BaseSubManager.SETTING_UP, sdlManager.getState());


        // Case 4-A:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.checkState();
        assertEquals(BaseSubManager.LIMITED, sdlManager.getState());


        // Case 4-B:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_NEVER);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.checkState();
        assertEquals(BaseSubManager.LIMITED, sdlManager.getState());


        // Case 5-A:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.LIMITED);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.READY);
        sdlManager.checkState();
        assertEquals(BaseSubManager.LIMITED, sdlManager.getState());


        // Case 5-B:
        sdlManager.getPermissionManager().transitionToState(BaseSubManager.READY);
        sdlManager.getFileManager().transitionToState(BaseSubManager.LIMITED);
        sdlManager.getScreenManager().transitionToState(BaseSubManager.ERROR);
        sdlManager.getLockScreenConfig().setDisplayMode(LockScreenConfig.DISPLAY_MODE_NEVER);
        sdlManager.getLockScreenManager().transitionToState(BaseSubManager.SETTING_UP);
        sdlManager.checkState();
        assertEquals(BaseSubManager.LIMITED, sdlManager.getState());


        // Case 6
        sdlManager.dispose();
        assertEquals(BaseSubManager.SHUTDOWN, sdlManager.getState());
    }

    @Test
    public void testSendRPC() {
        listenerCalledCounter = 0;

        // When internalInterface.sendRPC() is called, create a fake success response
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
        doAnswer(answer).when(internalInterface).sendRPC(any(RPCMessage.class));


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

        sdlManager.sendRPC(request);

        // Make sure the listener is called exactly once
        assertEquals("Listener was not called or called more/less frequently than expected", 1, listenerCalledCounter);
    }

    @Test
    public void testSendRPCs() {
        testSendMultipleRPCs(false);
    }

    @Test
    public void testSendSequentialRPCs() {
        testSendMultipleRPCs(true);
    }

    private void testSendMultipleRPCs(boolean sequentialSend) {
        listenerCalledCounter = 0;

        // When internalInterface.sendRPCs() is called, call listener.onFinished() to fake the response
        final Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                OnMultipleRequestListener listener = (OnMultipleRequestListener) args[1];
                listener.onFinished();
                return null;
            }
        };

        if (sequentialSend) {
            doAnswer(answer).when(internalInterface).sendSequentialRPCs(any(List.class), any(OnMultipleRequestListener.class));

        } else {
            doAnswer(answer).when(internalInterface).sendRPCs(any(List.class), any(OnMultipleRequestListener.class));
        }


        // Test send RPC requests
        List<RPCMessage> rpcsList = Arrays.asList(new GetVehicleData(), new Show(), new OnAppServiceData(), new GetAppServiceDataResponse());
        OnMultipleRequestListener onMultipleRequestListener = new OnMultipleRequestListener() {
            @Override
            public void onUpdate(int remainingRequests) {
            }

            @Override
            public void onFinished() {
                listenerCalledCounter++;
            }

            @Override
            public void onResponse(int correlationId, RPCResponse response) {
            }
        };
        if (sequentialSend) {
            sdlManager.sendSequentialRPCs(rpcsList, onMultipleRequestListener);
        } else {
            sdlManager.sendRPCs(rpcsList, onMultipleRequestListener);
        }


        // Make sure the listener is called exactly once
        assertEquals("Listener was not called or called more/less frequently than expected", 1, listenerCalledCounter);
    }
}
