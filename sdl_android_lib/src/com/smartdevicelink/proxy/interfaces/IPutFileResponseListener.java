package com.smartdevicelink.proxy.interfaces;

import com.smartdevicelink.proxy.rpc.PutFileResponse;

public interface IPutFileResponseListener {
	void onPutFileResponse(PutFileResponse response);
	
	void onPutFileStreamError(Exception e, String info);
}
