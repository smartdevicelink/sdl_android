package com.smartdevicelink.proxy.callbacks;

public class InternalProxyMessage {
	private String _functionName;
	public static final String OnProxyError = "OnProxyError";
	public static final String OnProxyOpened = "OnProxyOpened";
	public static final String OnProxyClosed = "OnProxyClosed";
	
	public InternalProxyMessage(String functionName) {
		//this(functionName, null, null);
		this._functionName = functionName;
	}
	
	public String getFunctionName() {
		return _functionName;
	}
}