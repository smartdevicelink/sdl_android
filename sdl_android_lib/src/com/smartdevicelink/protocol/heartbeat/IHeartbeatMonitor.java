package com.smartdevicelink.protocol.heartbeat;


public interface IHeartbeatMonitor {
    /**
     * Starts the monitor. If the monitor is already started, nothing happens.
     */
    void start();

    /**
     * Stops the monitor. Does nothing if it is already stopped.
     */
    void stop();

    /**
     * Returns the heartbeat messages interval.
     *
     * @return interval in milliseconds
     */
    int getInterval();

    /**
     * Sets the interval for sending heartbeat messages if nothing is sent over
     * transport.
     *
     * @param interval interval in milliseconds (min/max values depend on
     *                 concrete implementations)
     */
    void setInterval(int interval);

    /**
     * Returns the listener.
     *
     * @return the listener
     */
    IHeartbeatMonitorListener getListener();

    /**
     * Sets the heartbeat's listener.
     *
     * @param listener the new listener
     */
    void setListener(IHeartbeatMonitorListener listener);

    /**
     * Notifies the monitor about sent/received messages.
     */
    void notifyTransportActivity();

    /**
     * Notifies the monitor about a received heartbeat ACK message.
     */
    void heartbeatACKReceived();
    
    /**
     * Notifies the monitor about a received heartbeat message.
     */
    void heartbeatReceived();
}