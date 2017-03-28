package com.smartdevicelink.Dispatcher;

public interface IDispatchingStrategy<T> {
	void dispatch(T message);
	
	void handleDispatchingError(String info, Exception ex);
	
	void handleQueueingError(String info, Exception ex);
}
