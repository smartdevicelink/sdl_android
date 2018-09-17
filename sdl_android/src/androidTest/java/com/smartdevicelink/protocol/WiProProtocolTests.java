package com.smartdevicelink.protocol;

import android.test.AndroidTestCase;
import android.util.Log;

import com.smartdevicelink.SdlConnection.SdlConnection;
import com.smartdevicelink.protocol.WiProProtocol.MessageFrameAssembler;
import com.smartdevicelink.protocol.enums.SessionType;
import com.smartdevicelink.test.SampleRpc;
import com.smartdevicelink.test.SdlUnitTestContants;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.RouterServiceValidator;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

/**
 * This is a unit test class for the SmartDeviceLink library project class : 
 * {@link com.smartdevicelink.protocol.BinaryFrameHeader}
 */
public class WiProProtocolTests extends AndroidTestCase {
	
	int max_int = 2147483647;
	byte[] payload;
	MultiplexTransportConfig config;
	SdlConnectionTestClass connection;
	WiProProtocol protocol;
	
	IProtocolListener defaultListener = new IProtocolListener(){
		@Override
		public void onProtocolMessageBytesToSend(SdlPacket packet) {}
		@Override
		public void onProtocolMessageReceived(ProtocolMessage msg) {}
		@Override
		public void onProtocolSessionStarted(SessionType sessionType,byte sessionID, byte version, String correlationID, int hashID,boolean isEncrypted){}
		@Override
		public void onProtocolSessionNACKed(SessionType sessionType,byte sessionID, byte version, String correlationID, List<String> rejectedParams) {}
		@Override
		public void onProtocolSessionEnded(SessionType sessionType,byte sessionID, String correlationID) {}
		@Override
		public void onProtocolSessionEndedNACKed(SessionType sessionType,byte sessionID, String correlationID) {}
		@Override
		public void onProtocolHeartbeat(SessionType sessionType, byte sessionID) {}
		@Override
		public void onProtocolHeartbeatACK(SessionType sessionType,byte sessionID) {}
		@Override
		public void onProtocolServiceDataACK(SessionType sessionType,int dataSize, byte sessionID) {}
		@Override
		public void onResetOutgoingHeartbeat(SessionType sessionType,byte sessionID) {}
		@Override
		public void onResetIncomingHeartbeat(SessionType sessionType,byte sessionID) {}
		@Override
		public void onProtocolError(String info, Exception e) {}
	};
	public static class DidReceiveListener implements IProtocolListener{
		boolean didReceive = false;
		
		public void reset(){
			didReceive = false;
		}
		public boolean didReceive(){
			return didReceive;
		}
		@Override
		public void onProtocolMessageBytesToSend(SdlPacket packet) {}
		@Override
		public void onProtocolMessageReceived(ProtocolMessage msg) {
			didReceive = true;
			Log.d("DidReceiveListener", "RPC Type: " + msg.getRPCType());
			Log.d("DidReceiveListener", "Function Id: " + msg.getFunctionID());
			Log.d("DidReceiveListener", "JSON Size: " + msg.getJsonSize());
		}
		@Override
		public void onProtocolSessionStarted(SessionType sessionType,byte sessionID, byte version, String correlationID, int hashID,boolean isEncrypted){}
		@Override
		public void onProtocolSessionNACKed(SessionType sessionType,byte sessionID, byte version, String correlationID, List<String> rejectedParams) {}
		@Override
		public void onProtocolSessionEnded(SessionType sessionType,byte sessionID, String correlationID) {}
		@Override
		public void onProtocolSessionEndedNACKed(SessionType sessionType,byte sessionID, String correlationID) {}
		@Override
		public void onProtocolHeartbeat(SessionType sessionType, byte sessionID) {}
		@Override
		public void onProtocolHeartbeatACK(SessionType sessionType,byte sessionID) {}
		@Override
		public void onProtocolServiceDataACK(SessionType sessionType,int dataSize, byte sessionID) {}
		@Override
		public void onResetOutgoingHeartbeat(SessionType sessionType,byte sessionID) {}
		@Override
		public void onResetIncomingHeartbeat(SessionType sessionType,byte sessionID) {}
		@Override
		public void onProtocolError(String info, Exception e) {}
	};
	DidReceiveListener onProtocolMessageReceivedListener = new DidReceiveListener();
	
	public void testBase(){
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		
	}
	
	public void testVersion(){
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		
		wiProProtocol.setVersion((byte)0x01);
		assertEquals((byte)0x01,wiProProtocol.getVersion());
		
		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x02);
		assertEquals((byte)0x02,wiProProtocol.getVersion());
		
		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x03);
		assertEquals((byte)0x03,wiProProtocol.getVersion());
		
		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x04);
		assertEquals((byte)0x04,wiProProtocol.getVersion());

		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x05);
		assertEquals((byte)0x05,wiProProtocol.getVersion());

		//If we get newer than 5, it should fall back to 5
		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x06);
		assertEquals((byte)0x05,wiProProtocol.getVersion());
		
		//Is this right?
		wiProProtocol = new WiProProtocol(defaultListener);
		wiProProtocol.setVersion((byte)0x00);
		assertEquals((byte)0x01,wiProProtocol.getVersion());
	}
	
	public void testMtu(){
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		
		wiProProtocol.setVersion((byte)0x01);
		 
		try{
			assertEquals(wiProProtocol.getMtu(), 1500-8);

			//Version 2
			wiProProtocol.setVersion((byte)0x02);
			assertEquals(wiProProtocol.getMtu(), 1500-12);
			
			//Version 3
			wiProProtocol.setVersion((byte)0x03);
			assertEquals(wiProProtocol.getMtu(), 131072);

			//Version 4
			wiProProtocol.setVersion((byte)0x04);
			assertEquals(wiProProtocol.getMtu(), 131072);
			
			//Version 5
			wiProProtocol.setVersion((byte)0x05);
			assertEquals(wiProProtocol.getMtu(), 131072);

			//Version 5+
			wiProProtocol.setVersion((byte)0x06);
			assertEquals(wiProProtocol.getMtu(), 131072);
			
		}catch(Exception e){
			Assert.fail("Exceptin during reflection");
		}

	}
	
	public void testHandleFrame(){
		SampleRpc sampleRpc = new SampleRpc(4);
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		MessageFrameAssembler assembler = wiProProtocol.new MessageFrameAssembler();
		try{
			assembler.handleFrame(sampleRpc.toSdlPacket());
		}catch(Exception e){
			Assert.fail("Exceptin during handleFrame - " + e.toString());
		}
	}
	public void testHandleFrameCorrupt(){
		SampleRpc sampleRpc = new SampleRpc(4);
		BinaryFrameHeader header = sampleRpc.getBinaryFrameHeader(true);
		header.setJsonSize(Integer.MAX_VALUE);
		sampleRpc.setBinaryFrameHeader(header);
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		MessageFrameAssembler assembler = wiProProtocol.new MessageFrameAssembler();
		try{
			assembler.handleFrame(sampleRpc.toSdlPacket());
		}catch(Exception e){
			Assert.fail("Exceptin during handleFrame - " + e.toString());
		}
	}
	
	public void testHandleSingleFrameMessageFrame(){
		SampleRpc sampleRpc = new SampleRpc(4);
		WiProProtocol wiProProtocol = new WiProProtocol(defaultListener);
		MessageFrameAssembler assembler = wiProProtocol.new MessageFrameAssembler();

		
		try{
			Method  method = assembler.getClass().getDeclaredMethod ("handleSingleFrameMessageFrame", SdlPacket.class);
			method.setAccessible(true);	
			method.invoke (assembler, sampleRpc.toSdlPacket());	
		}catch(Exception e){
			Assert.fail("Exceptin during handleSingleFrameMessageFrame - " + e.toString());
		}	
	}
	
	public void testHandleSingleFrameMessageFrameCorruptBfh(){
		SampleRpc sampleRpc = new SampleRpc(4);
		
		//Create a corrupted header
		BinaryFrameHeader header = sampleRpc.getBinaryFrameHeader(true);
		header.setJsonSize(5);
		header.setJsonData(new byte[5]);
		header.setJsonSize(Integer.MAX_VALUE);	
		sampleRpc.setBinaryFrameHeader(header);
			
		SdlPacket packet = sampleRpc.toSdlPacket();
				
		BinaryFrameHeader binFrameHeader = BinaryFrameHeader.parseBinaryHeader(packet.payload);
		assertNull(binFrameHeader);
		
		WiProProtocol wiProProtocol = new WiProProtocol(onProtocolMessageReceivedListener);
		
		
		wiProProtocol.handlePacketReceived(packet);
		assertFalse(onProtocolMessageReceivedListener.didReceive());
		
		onProtocolMessageReceivedListener.reset();
		MessageFrameAssembler assembler =wiProProtocol.getFrameAssemblerForFrame(packet);// wiProProtocol.new MessageFrameAssembler();
		assertNotNull(assembler);
		assembler.handleFrame(packet);
		assertFalse(onProtocolMessageReceivedListener.didReceive());
		
		try{
			Method  method = assembler.getClass().getDeclaredMethod("handleSingleFrameMessageFrame", SdlPacket.class);
			method.setAccessible(true);	
			method.invoke (assembler, sampleRpc.toSdlPacket());	
		}catch(Exception e){
			Assert.fail("Exceptin during handleSingleFrameMessageFrame - " + e.toString());
		}	
	}
	

	
	public void setUp(){
		config = new MultiplexTransportConfig(this.mContext,SdlUnitTestContants.TEST_APP_ID);
		connection = new SdlConnectionTestClass(config, null);
		protocol = new WiProProtocol(connection);
	}
	
	public void testNormalCase(){
		setUp();
		payload = new byte[]{0x00,0x02,0x05,0x01,0x01,0x01,0x05,0x00};
		byte sessionID = 1, version = 1;
		int messageID = 1;
		boolean encrypted = false;
		SdlPacket sdlPacket = SdlPacketFactory.createMultiSendDataFirst(SessionType.RPC, sessionID, messageID, version, payload, encrypted);
		MessageFrameAssembler assembler = protocol.getFrameAssemblerForFrame(sdlPacket);
		
		assertNotNull(assembler);
		
		OutOfMemoryError oom_error = null;
		NullPointerException np_exception = null;
		try{
			assembler.handleMultiFrameMessageFrame(sdlPacket);
		}catch(OutOfMemoryError e){
			oom_error = e;
		}catch(NullPointerException z){
			np_exception = z;
		}catch(Exception e){
			e.printStackTrace();
			assertNotNull(null);
		}
		
		assertNull(np_exception);
		assertNull(oom_error);
		
		payload = new byte[23534];
		sdlPacket = SdlPacketFactory.createMultiSendDataRest(SessionType.RPC, sessionID, payload.length, (byte) 3, messageID, version, payload, 0, 1500, encrypted);
		assembler = protocol.getFrameAssemblerForFrame(sdlPacket);
		try{
			assembler.handleMultiFrameMessageFrame(sdlPacket);
		}catch(OutOfMemoryError e){
			oom_error = e;
		}catch(NullPointerException z){
			np_exception = z;
		}catch(Exception e){
			assertNotNull(null);
		}
		
		assertNull(np_exception);
		assertNull(oom_error);
	}
	
	public void testOverallocatingAccumulator(){
		setUp();
		ByteArrayOutputStream builder = new ByteArrayOutputStream();
		for(int i = 0; i < 8; i++){
			builder.write(0x0F);
		}
		payload = builder.toByteArray();
		byte sessionID = 1, version = 1;
		int messageID = 1;
		boolean encrypted = false;
		SdlPacket sdlPacket = SdlPacketFactory.createMultiSendDataFirst(SessionType.RPC, sessionID, messageID, version, payload, encrypted);
		MessageFrameAssembler assembler = protocol.getFrameAssemblerForFrame(sdlPacket);
		
		OutOfMemoryError oom_error = null;
		NullPointerException np_exception = null;
		try{
			assembler.handleMultiFrameMessageFrame(sdlPacket);
		}catch(OutOfMemoryError e){
			oom_error = e;
		}catch(NullPointerException z){
			np_exception = z;
		}catch(Exception e){
			assertNotNull(null);
		}
		
		assertNull(np_exception);
		assertNull(oom_error);

		payload = new byte[23534];
		sdlPacket = SdlPacketFactory.createMultiSendDataRest(SessionType.RPC, sessionID, payload.length, (byte) 3, messageID, version, payload, 0, 1500, encrypted);
		assembler = protocol.getFrameAssemblerForFrame(sdlPacket);
		
		try{
			assembler.handleMultiFrameMessageFrame(sdlPacket);
		}catch(OutOfMemoryError e){
			oom_error = e;
		}catch(NullPointerException z){
			np_exception = z;
		}catch(Exception e){
			assertNotNull(null);
		}
		
		assertNull(np_exception);
		assertNull(oom_error);
		
	}

	/**
	 * Test data being null in SendMessage() method
	 *
	 * @see WiProProtocol#SendMessage(ProtocolMessage)
	 */
	public void testNullDataInSendMessage() {
		Object protocolMsg = null;
		Field dataField = null;
		Field msgLockField = null;
		try {
			protocolMsg = Class.forName("com.smartdevicelink.protocol.ProtocolMessage").newInstance();
			dataField = ProtocolMessage.class.getDeclaredField("_data");
			dataField.setAccessible(true);
			// Send null data
			dataField.set(protocolMsg, null);
			// Set _version and _messageLocks fields properly so we can progress to the later part of the method being tested
			WiProProtocol protocol = new WiProProtocol(defaultListener);
			protocol.setVersion((byte)0x01);
			Hashtable<Byte, Object> table = new Hashtable<>();
			table.put((byte)0x00, new Object());
			msgLockField = WiProProtocol.class.getDeclaredField("_messageLocks");
			msgLockField.setAccessible(true);
			msgLockField.set(protocol, table);
			protocol.SendMessage((ProtocolMessage)protocolMsg);
		} catch (Exception e) {
			Assert.fail("Exception in testNullDataInSendMessage(), " + e);
		}
	}
	
	protected class SdlConnectionTestClass extends SdlConnection{
		protected boolean connected = false;
		public SdlConnectionTestClass(BaseTransportConfig transportConfig) {
			super(transportConfig);
		}

		protected SdlConnectionTestClass(BaseTransportConfig transportConfig,RouterServiceValidator rsvp){
			super(transportConfig,rsvp);
		}
		
		@Override
		public void onTransportConnected() {
			super.onTransportConnected();
			connected = true;
		}

		@Override
		public void onTransportDisconnected(String info) {
			connected = false;
			//Grab a currently running router service
			RouterServiceValidator rsvp2 = new RouterServiceValidator(mContext);
			rsvp2.setFlags(RouterServiceValidator.FLAG_DEBUG_NONE);
			assertTrue(rsvp2.validate());
			assertNotNull(rsvp2.getService());
			SdlConnectionTestClass.cachedMultiConfig.setService(rsvp2.getService());
			super.onTransportDisconnected(info);
		}

		@Override
		public void onTransportError(String info, Exception e) {
			connected = false;
			super.onTransportError(info, e);
		}
	}
}
