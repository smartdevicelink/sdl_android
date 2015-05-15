package com.smartdevicelink.transport;

/**
 * Defines available types of the transports.
 */
public enum TransportType {
	/**
	 * Experimental multiplexing (only supports bluetooth at the moment)
	 */
	MULTIPLEX,
	/**
	 * Transport type is Bluetooth.
	 */
	BLUETOOTH,
	
	/**
	 * Transport type is TCP.
	 */
	TCP,
	USB
}
