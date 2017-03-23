package com.smartdevicelink.protocol;

import java.nio.ByteBuffer;

import android.os.Parcel;
import android.os.Parcelable;

import com.smartdevicelink.protocol.enums.FrameType;

public class SdlPacket implements Parcelable {
    public static final int HEADER_SIZE = 12;
    public static final int HEADER_SIZE_V1 = 8;//Backwards

    private static final int ENCRYPTION_MASK = 0x08; //4th lowest bit

    public static final int FRAME_TYPE_CONTROL = 0x00;
    public static final int FRAME_TYPE_SINGLE = 0x01;
    public static final int FRAME_TYPE_FIRST = 0x02;
    public static final int FRAME_TYPE_CONSECUTIVE = 0x03;

    /**
     * Service Type
     */
    public static final int SERVICE_TYPE_CONTROL = 0x00;
    //RESERVED 0x01 - 0x06
    public static final int SERVICE_TYPE_RPC = 0x07;
    //RESERVED 0x08 - 0x09
    public static final int SERVICE_TYPE_PCM = 0x0A;
    public static final int SERVICE_TYPE_VIDEO = 0x0B;
    //RESERVED 0x0C - 0x0E
    public static final int SERVICE_TYPE_BULK_DATA = 0x0F;
    //RESERVED 0x10 - 0xFF


    /**
     * Frame Info
     */
    //Control Frame Info
    public static final int FRAME_INFO_HEART_BEAT = 0x00;
    public static final int FRAME_INFO_START_SERVICE = 0x01;
    public static final int FRAME_INFO_START_SERVICE_ACK = 0x02;
    public static final int FRAME_INFO_START_SERVICE_NAK = 0x03;
    public static final int FRAME_INFO_END_SERVICE = 0x04;
    public static final int FRAME_INFO_END_SERVICE_ACK = 0x05;
    public static final int FRAME_INFO_END_SERVICE_NAK = 0x06;
    //0x07-0xFD are reserved
    public static final int FRAME_INFO_SERVICE_DATA_ACK = 0xFE;
    public static final int FRAME_INFO_HEART_BEAT_ACK = 0xFF;

    public static final int FRAME_INFO_FINAL_CONNESCUTIVE_FRAME = 0x00;

    //Most others
    public static final int FRAME_INFO_RESERVED = 0x00;


    /**
     * package scope
     */
    int version;
    /**
     * Compression Flag, which only available in Protocol Version 1
     * or Encryption Flag, which only available in Protocol Version 2
     */
    boolean encryption;
    int frameType;
    int serviceType;
    int frameInfo;
    int sessionId;
    int dataSize;
    int messageId;
    int priorityCoefficient;
    byte[] payload = null;

    /**
     * This constructor is available as a protected method. A few defaults have been set,
     * however a few things <b>MUST</b> be set before use. The rest will "work"
     * however, it won't be valid data.
     * <p>
     * <p>Frame Type
     * <p>Service Type
     * <p>Frame Info
     * <p>
     */
    protected SdlPacket() {
        //TODO add defaults
        this(1, // version
                false, // encryption
                -1, // frameType, This NEEDS to be set
                -1, // serviceType
                -1, // frameInfo
                0, // sessionId
                0, // dataSize
                0, // messageId
                null // payload
        );

    }


    /**
     * Creates a new packet based on previous packet definitions
     *
     * @param packet other input packet
     */
    SdlPacket(SdlPacket packet) {
        this(packet.version, // version
                packet.encryption, // encryption
                packet.frameType, // frameType, This NEEDS to be set
                packet.serviceType, // serviceType
                packet.frameInfo, // frameInfo
                packet.sessionId, // sessionId
                0, // dataSize
                0, // messageId
                null // payload
        );
    }

    public SdlPacket(int version, boolean encryption, int frameType,
                     int serviceType, int frameInfo, int sessionId,
                     int dataSize, int messageId, byte[] payload) {
        init(version, encryption, frameType,
                serviceType, frameInfo, sessionId,
                dataSize, messageId, payload, 0, payload != null ? payload.length : 0);
    }

    public SdlPacket(int version, boolean encryption, int frameType,
                     int serviceType, int frameInfo, int sessionId,
                     int dataSize, int messageId, byte[] payload, int offset, int bytesToWrite) {
        init(version, encryption, frameType,
                serviceType, frameInfo, sessionId,
                dataSize, messageId, payload, offset, bytesToWrite);
    }

    private void init(int version, boolean encryption, int frameType,
                      int serviceType, int frameInfo, int sessionId,
                      int dataSize, int messageId, byte[] payload, int offset, int bytesToWrite) {
        this.version = version;
        this.encryption = encryption;
        this.frameType = frameType;
        this.serviceType = serviceType;
        this.frameInfo = frameInfo;
        this.sessionId = sessionId;
        this.dataSize = dataSize;
        this.messageId = messageId;
        this.priorityCoefficient = 0;
        if (payload != null) {
            this.payload = new byte[bytesToWrite];
            System.arraycopy(payload, offset, this.payload, 0, bytesToWrite);
        }
    }

    public int getVersion() {
        return version;
    }

    public boolean isEncrypted() {
        return encryption;
    }

    public FrameType getFrameType() {
        switch (frameType) {
            case FRAME_TYPE_CONTROL:
                return FrameType.Control;
            case FRAME_TYPE_FIRST:
                return FrameType.First;
            case FRAME_TYPE_CONSECUTIVE:
                return FrameType.Consecutive;
            case FRAME_TYPE_SINGLE:
            default:
                return FrameType.Single;
        }
    }

    public int getServiceType() {
        return serviceType;
    }

    public int getFrameInfo() {
        return frameInfo;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getMessageId() {
        return messageId;
    }

    public long getDataSize() {
        return dataSize;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] constructPacket() {
        return constructPacket(version, encryption, frameType,
                serviceType, frameInfo, sessionId,
                dataSize, messageId, payload);
    }

    public void setPayload(byte[] bytes) {
        this.payload = bytes;
    }

    /**
     * Set the priority for this packet.
     * The lower the number the higher the priority. <br>0 is the highest priority and the default.
     *
     * @param priority priority value
     */
    public void setPriorityCoefficient(int priority) {
        this.priorityCoefficient = priority;
    }

    /**
     * get the priority for this packet.
     *
     * @return priority value, The lower the number the higher the priority.
     */
    public int getPriorityCoefficient() {
        return this.priorityCoefficient;
    }

    /**
     * FIXME: change to private in future release
     * This method takes in the various components to the SDL packet structure
     * and creates a new byte array that can be sent via the transport
     *
     * @param version
     * @param encryption
     * @param frameType
     * @param serviceType
     * @param controlFrameInfo
     * @param sessionId
     * @param dataSize
     * @param messageId
     * @param payload
     *
     * @return byte array which represents this packet
     */
    public static byte[] constructPacket(int version, boolean encryption, int frameType,
                                      int serviceType, int controlFrameInfo, int sessionId,
                                      int dataSize, int messageId, byte[] payload) {
        ByteBuffer builder;
        switch (version) {
            case 1:
                builder = ByteBuffer.allocate(HEADER_SIZE_V1 + dataSize);
                break;
            default:
                builder = ByteBuffer.allocate(HEADER_SIZE + dataSize);
                break;
        }

        builder.put((byte) ((version << 4) + getEncryptionBit(encryption) + frameType));
        builder.put((byte) serviceType);
        builder.put((byte) controlFrameInfo);
        builder.put((byte) sessionId);

        builder.put((byte) ((dataSize & 0xFF000000) >> 24));
        builder.put((byte) ((dataSize & 0x00FF0000) >> 16));
        builder.put((byte) ((dataSize & 0x0000FF00) >> 8));
        builder.put((byte) ((dataSize & 0x000000FF)));

        if (version > 1) {
            // Version 1 did not include this part of the header
            builder.put((byte) ((messageId & 0xFF000000) >> 24));
            builder.put((byte) ((messageId & 0x00FF0000) >> 16));
            builder.put((byte) ((messageId & 0x0000FF00) >> 8));
            builder.put((byte) ((messageId & 0x000000FF)));
        }

        if (payload != null && payload.length > 0) {
            builder.put(payload);
        }

        return builder.array();
    }


    /**
     * FIXME: change to private in future release
     * @param encryption
     * @return
     */
    public static int getEncryptionBit(boolean encryption) {
        if (encryption) {
            return ENCRYPTION_MASK;
        } else {
            return 0;
        }
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("***** Sdl Packet ******")
                .append("\nVersion:  ").append(version)
                .append("\nEncryption:  ").append(encryption)
                .append("\nFrameType:  ").append(frameType)
                .append("\nServiceType:  ").append(serviceType)
                .append("\nFrameInfo:  ").append(frameInfo)
                .append("\nSessionId:  ").append(sessionId)
                .append("\nDataSize:  ").append(dataSize);
        if (version > 1) {
            builder.append("\nMessageId:  ").append(messageId);
        }
        builder.append("\n***** Sdl Packet  End******");
        return builder.toString();
    }

    public SdlPacket(Parcel p) {
        this.version = p.readInt();
        this.encryption = (p.readInt() == 0);
        this.frameType = p.readInt();
        this.serviceType = p.readInt();
        this.frameInfo = p.readInt();
        this.sessionId = p.readInt();
        this.dataSize = p.readInt();
        this.messageId = p.readInt();
        if (p.readInt() == 1) {
            //We should have a payload attached
            payload = new byte[dataSize];
            p.readByteArray(payload);
        }
        this.priorityCoefficient = p.readInt();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(version);
        dest.writeInt(encryption ? 1 : 0);
        dest.writeInt(frameType);
        dest.writeInt(serviceType);
        dest.writeInt(frameInfo);
        dest.writeInt(sessionId);
        dest.writeInt(dataSize);
        dest.writeInt(messageId);
        dest.writeInt(payload != null ? 1 : 0);
        if (payload != null) {
            dest.writeByteArray(payload);
        }
        dest.writeInt(priorityCoefficient);
    }

    public static final Parcelable.Creator<SdlPacket> CREATOR = new Parcelable.Creator<SdlPacket>() {
        public SdlPacket createFromParcel(Parcel in) {
            return new SdlPacket(in);
        }

        @Override
        public SdlPacket[] newArray(int size) {
            return new SdlPacket[size];
        }

    };


}
