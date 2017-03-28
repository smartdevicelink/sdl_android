package com.smartdevicelink.SdlConnection;

import com.smartdevicelink.protocol.ProtocolMessage;
import com.smartdevicelink.protocol.enums.SessionType;


public interface ISdlConnectionListener {
	void onTransportDisconnected(String info);
	
	void onTransportError(String info, Exception e);
	
	void onProtocolMessageReceived(ProtocolMessage msg);
	
	void onProtocolSessionStartedNACKed(SessionType sessionType,
			byte sessionID, byte version, String correlationID);	
	
	void onProtocolSessionStarted(SessionType sessionType,
			byte sessionID, byte version, String correlationID, int hashID, boolean isEncrypted);
	
	void onProtocolSessionEnded(SessionType sessionType,
			byte sessionID, String correlationID);
	
	void onProtocolSessionEndedNACKed(SessionType sessionType,
	byte sessionID, String correlationID);
	
	void onProtocolError(String info, Exception e);
	
	void onHeartbeatTimedOut(byte sessionID);
	
	void onProtocolServiceDataACK(SessionType sessionType, int dataSize, byte sessionID);
}
