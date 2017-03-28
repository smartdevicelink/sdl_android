package com.smartdevicelink.protocol.heartbeat;

/**
 * Listener (aka delegate) that actually knows how to work with the outside
 * world.
 */
public interface IHeartbeatMonitorListener {
    /**
     * The listener should create and send a heartbeat message.
     *
     * @param monitor the caller
     */
    void sendHeartbeat(IHeartbeatMonitor monitor);

    /**
     * Notifies the listener that the heartbeat timed out.
     *
     * @param monitor the caller
     */
    void heartbeatTimedOut(IHeartbeatMonitor monitor);
}
