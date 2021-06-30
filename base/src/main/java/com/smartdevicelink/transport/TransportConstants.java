/*
 * Copyright (c) 2017 - 2019, SmartDeviceLink Consortium, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the SmartDeviceLink Consortium, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.smartdevicelink.transport;


/**
 * These constants are shared between the router service and the SDL base service.
 * They are defined as strings/actions/values that both of them can understand.
 * Attempting to use standard HTTP error codes as definitions.
 *
 * @author Joey Grover
 */
public class TransportConstants {
    public static final String START_ROUTER_SERVICE_ACTION = "sdl.router.startservice";
    public static final String ROUTER_SERVICE_ACTION = "com.smartdevicelink.router.service";
    public static final String FOREGROUND_EXTRA = "foreground";
    public static final String CONFIRMED_SDL_DEVICE = "confirmed_sdl_device";

    public static final String BIND_LOCATION_PACKAGE_NAME_EXTRA = "BIND_LOCATION_PACKAGE_NAME_EXTRA";
    public static final String BIND_LOCATION_CLASS_NAME_EXTRA = "BIND_LOCATION_CLASS_NAME_EXTRA";

    public static final String ALT_TRANSPORT_RECEIVER = "com.sdl.android.alttransport";
    public static final String ALT_TRANSPORT_CONNECTION_STATUS_EXTRA = "connection_status";
    public static final int ALT_TRANSPORT_DISCONNECTED = 0;
    public static final int ALT_TRANSPORT_CONNECTED = 1;
    public static final String ALT_TRANSPORT_READ = "read";//Read from the alt transport, goes to the app
    public static final String ALT_TRANSPORT_WRITE = "write";//Write to the alt transport, comes from the app
    public static final String ALT_TRANSPORT_ADDRESS_EXTRA = "altTransportAddress";

    public static final String START_ROUTER_SERVICE_SDL_ENABLED_EXTRA = "sdl_enabled";
    public static final String START_ROUTER_SERVICE_SDL_ENABLED_APP_PACKAGE = "package_name";
    public static final String START_ROUTER_SERVICE_SDL_ENABLED_CMP_NAME = "component_name";
    public static final String START_ROUTER_SERVICE_TRANSPORT_CONNECTED = "transport_connected"; //Extra for the transport that just connected
    public static final String START_ROUTER_SERVICE_SDL_ENABLED_PING = "ping";
    @Deprecated
    public static final String FORCE_TRANSPORT_CONNECTED = "force_connect"; //This is legacy, do not refactor this.
    public static final String ROUTER_SERVICE_VALIDATED = "router_service_validated";

    public static final String CONNECT_AS_CLIENT_BOOLEAN_EXTRA = "connectAsClient";
    public static final String PACKAGE_NAME_STRING = "package.name";
    public static final String APP_ID_EXTRA = "app.id";//Sent as a Long. This is no longer used
    public static final String APP_ID_EXTRA_STRING = "app.id.string";
    public static final String ROUTER_MESSAGING_VERSION = "router.messaging.version";

    public static final String SESSION_ID_EXTRA = "session.id";

    public static final String ENABLE_LEGACY_MODE_EXTRA = "ENABLE_LEGACY_MODE_EXTRA";

    @Deprecated
    public static final String HARDWARE_DISCONNECTED = "hardware.disconect"; //This is legacy, do not refactor this.
    public static final String TRANSPORT_DISCONNECTED = "transport.disconect";
    public static final String HARDWARE_CONNECTED = "hardware.connected";
    public static final String CURRENT_HARDWARE_CONNECTED = "current.hardware.connected";

    public static final String SEND_PACKET_TO_APP_LOCATION_EXTRA_NAME = "senderintent";
    public static final String SEND_PACKET_TO_ROUTER_LOCATION_EXTRA_NAME = "routerintent";


    public static final String BIND_REQUEST_TYPE_CLIENT = "BIND_REQUEST_TYPE_CLIENT";
    public static final String BIND_REQUEST_TYPE_ALT_TRANSPORT = "BIND_REQUEST_TYPE_ALT_TRANSPORT";
    public static final String BIND_REQUEST_TYPE_STATUS = "BIND_REQUEST_TYPE_STATUS";
    public static final String BIND_REQUEST_TYPE_USB_PROVIDER = "BIND_REQUEST_TYPE_USB_PROVIDER";


    public static final String PING_ROUTER_SERVICE_EXTRA = "ping.router.service";

    public static final String SDL_NOTIFICATION_CHANNEL_ID = "sdl_notification_channel";
    public static final String SDL_NOTIFICATION_CHANNEL_NAME = "SmartDeviceLink";
    public static final String SDL_ERROR_NOTIFICATION_CHANNEL_ID = "sdl_error_notification_channel";
    public static final int SDL_ERROR_NOTIFICATION_CHANNEL_ID_INT = 99;
    public static final String VEHICLE_INFO = "vehicle_info";
    public static final String UNSUPPORTED_VEHICLE_INFO_REASON = "Vehicle Info is not supported";


    /**
     * This class houses all important router service versions
     */
    public class RouterServiceVersions {
        /**
         * This version of the router service is when app IDs went from Longs to Strings
         */
        public static final int APPID_STRING = 4;
    }


    /*
     * Alt transport
     *
     */

    /**
     * This will be the response when a hardware connect event comes through from an alt transport.
     * This is because it only makes sense to register an alt transport when a connection is established with that
     * transport, not waiting for one.
     */
    public static final int ROUTER_REGISTER_ALT_TRANSPORT_RESPONSE = 0x02;
    public static final int ROUTER_REGISTER_ALT_TRANSPORT_RESPONSE_SUCESS = 0x00;
    /**
     * There is already another alt transport connected, so we are unable to register this one
     */
    public static final int ROUTER_REGISTER_ALT_TRANSPORT_ALREADY_CONNECTED = 0x01;

    /**
     * This means the router service is shutting down for some reason. Most likely
     */
    public static final int ROUTER_SHUTTING_DOWN_NOTIFICATION = 0x0F;

    /**
     * There is a newer service to start up, so this one is shutting down
     */
    public static final int ROUTER_SHUTTING_DOWN_REASON_NEWER_SERVICE = 0x00;

    /*
     * Router to Client binding service
     *
     */

    //WHATS
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int ROUTER_REGISTER_CLIENT = 0x01;
    /**
     * This response message will contain if the registration request was successful or not. If not, the reason will be
     * great or equal to 1 and be descriptive of why it was denied.
     */
    public static final int ROUTER_REGISTER_CLIENT_RESPONSE = 0x02;
    //Response arguments
    public static final int REGISTRATION_RESPONSE_SUCESS = 0x00;
    public static final int REGISTRATION_RESPONSE_DENIED_AUTHENTICATION_FAILED = 0x01;
    public static final int REGISTRATION_RESPONSE_DENIED_NO_CONNECTION = 0x02;
    public static final int REGISTRATION_RESPONSE_DENIED_APP_ID_NOT_INCLUDED = 0x03;
    public static final int REGISTRATION_RESPONSE_DENIED_LEGACY_MODE_ENABLED = 0x04;
    public static final int REGISTRATION_RESPONSE_DENIED_UNKNOWN = 0xFF;

    /**
     * Command to the service to unregister a client, to stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT. Also include the app id as arg1.
     */
    public static final int ROUTER_UNREGISTER_CLIENT = 0x03;
    public static final int ROUTER_UNREGISTER_CLIENT_RESPONSE = 0x04;
    //Response arguments
    public static final int UNREGISTRATION_RESPONSE_SUCESS = 0x00;
    public static final int UNREGISTRATION_RESPONSE_FAILED_APP_ID_NOT_FOUND = 0x01;


    /**
     * what message type to notify apps of a hardware connection event. The connection event will be placed in the bundle
     * attached to the message
     */
    public static final int HARDWARE_CONNECTION_EVENT = 0x05;
    public static final int HARDWARE_CONNECTION_EVENT_CONNECTED = 0x10;
    public static final int HARDWARE_CONNECTION_EVENT_DISCONNECTED = 0x30;


    public static final int ROUTER_REQUEST_BT_CLIENT_CONNECT = 0x10;
    public static final int ROUTER_REQUEST_BT_CLIENT_CONNECT_RESPONSE = 0x11;

    /**
     * This provides the app with an ability to request another session within the router service.
     * A replyTo must be provided or else there won't be a response
     */
    public static final int ROUTER_REQUEST_NEW_SESSION = 0x12;
    //Request arguments
    //See TRANSPORT_TYPE & TRANSPORT_ADDRESS


    public static final int ROUTER_REQUEST_NEW_SESSION_RESPONSE = 0x13;
    //Response arguments
    public static final int ROUTER_REQUEST_NEW_SESSION_RESPONSE_SUCESS = 0x00;
    public static final int ROUTER_REQUEST_NEW_SESSION_RESPONSE_FAILED_APP_NOT_FOUND = 0x01;
    public static final int ROUTER_REQUEST_NEW_SESSION_RESPONSE_FAILED_APP_ID_NOT_INCL = 0x02;

    /**
     * This provides the app with an ability to request another session within the router service.
     * A replyTo must be provided or else there won't be a response
     */
    public static final int ROUTER_REMOVE_SESSION = 0x14;
    public static final int ROUTER_REMOVE_SESSION_RESPONSE = 0x15;
    //Response arguments
    public static final int ROUTER_REMOVE_SESSION_RESPONSE_SUCESS = 0x00;
    public static final int ROUTER_REMOVE_SESSION_RESPONSE_FAILED_APP_NOT_FOUND = 0x01;
    public static final int ROUTER_REMOVE_SESSION_RESPONSE_FAILED_APP_ID_NOT_INCL = 0x02;
    public static final int ROUTER_REMOVE_SESSION_RESPONSE_FAILED_SESSION_NOT_FOUND = 0x03;
    public static final int ROUTER_REMOVE_SESSION_RESPONSE_FAILED_SESSION_ID_NOT_INCL = 0x04;
    /**
     * Command to have router service to send a packet
     */
    public static final int ROUTER_SEND_PACKET = 0x20;

    //response
    /**
     * Router has received a packet and sent it to the client
     */
    public static final int ROUTER_RECEIVED_PACKET = 0x26;
    //response

    /**
     * Command to tell router service details of secondary transport
     */
    public static final int ROUTER_REQUEST_SECONDARY_TRANSPORT_CONNECTION = 0x30;

    //BUNDLE EXTRAS

    public static final String FORMED_PACKET_EXTRA_NAME = "packet";

    public static final String BYTES_TO_SEND_EXTRA_NAME = "bytes";
    public static final String BYTES_TO_SEND_EXTRA_OFFSET = "offset";
    public static final String BYTES_TO_SEND_EXTRA_COUNT = "count";
    public static final String BYTES_TO_SEND_FLAGS = "flags";

    public static final String PACKET_PRIORITY_COEFFICIENT = "priority_coefficient";

    public static final String TRANSPORT_TYPE = "transport_type";
    public static final String TRANSPORT_ADDRESS = "transport_address";

    public static final int BYTES_TO_SEND_FLAG_NONE = 0x00;
    public static final int BYTES_TO_SEND_FLAG_SDL_PACKET_INCLUDED = 0x01;
    public static final int BYTES_TO_SEND_FLAG_LARGE_PACKET_START = 0x02;
    public static final int BYTES_TO_SEND_FLAG_LARGE_PACKET_CONT = 0x04;
    public static final int BYTES_TO_SEND_FLAG_LARGE_PACKET_END = 0x08;

    public static final String CONNECTED_DEVICE_STRING_EXTRA_NAME = "devicestring";

    public static final int PACKET_SENDING_ERROR_NOT_REGISTERED_APP = 0x00;
    public static final int PACKET_SENDING_ERROR_NOT_CONNECTED = 0x01;
    public static final int PACKET_SENDING_ERROR_UKNOWN = 0xFF;

    public static final String ROUTER_SERVICE_VERSION = "router_service_version";

    /**
     * Status binder
     */

    public static final int ROUTER_STATUS_CONNECTED_STATE_REQUEST = 0x01;
    public static final int ROUTER_STATUS_CONNECTED_STATE_RESPONSE = 0x02;
    /**
     * This flag when used to check router status will trigger the router service in sending out a ping that if it is connected to a device
     */
    public static final int ROUTER_STATUS_FLAG_TRIGGER_PING = 0x02;


    /**
     * Usb Transfer binder
     */

    public static final int USB_CONNECTED_WITH_DEVICE = 0x55;
    public static final int ROUTER_USB_ACC_RECEIVED = 0x56;


    /**
     * Multiple-transports related constants
     */
    public static final String IAP_BLUETOOTH = "IAP_BLUETOOTH";
    public static final String IAP_USB = "IAP_USB";
    public static final String IAP_USB_HOST_MODE = "TCP_WIFI";
    public static final String IAP_CARPLAY = "IAP_CARPLAY";
    public static final String SPP_BLUETOOTH = "SPP_BLUETOOTH";
    public static final String AOA_USB = "AOA_USB";
    public static final String TCP_WIFI = "TCP_WIFI";


    /**
     * Broadcast action: sent when a USB accessory is attached.
     * <p>
     * UsbManager.EXTRA_ACCESSORY extra contains UsbAccessory object that has
     * been attached.
     */
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "com.smartdevicelink.USB_ACCESSORY_ATTACHED";


}
