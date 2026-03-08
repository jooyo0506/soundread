package com.soundread.sdk.podcast.protocol;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 火山引擎播客 WebSocket 二进制消息
 */
@Slf4j
@Data
public class PodcastMessage {
    private byte version = VersionBits.Version1.getValue();
    private byte headerSize = HeaderSizeBits.HeaderSize4.getValue();
    private MsgType type;
    private MsgTypeFlagBits flag;
    private byte serialization = SerializationBits.JSON.getValue();
    private byte compression = 0;

    private EventType event;
    private String sessionId;
    private String connectId;
    private int sequence;
    private int errorCode;
    private byte[] payload;

    public PodcastMessage(MsgType type, MsgTypeFlagBits flag) {
        this.type = type;
        this.flag = flag;
    }

    /**
     * 从二进制数据反序列化
     */
    public static PodcastMessage unmarshal(byte[] data) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte typeAndFlag = data[1];
        MsgType type = MsgType.fromValue((typeAndFlag >> 4) & 0x0F);
        MsgTypeFlagBits flag = MsgTypeFlagBits.fromValue(typeAndFlag & 0x0F);

        int versionAndHeaderSize = buffer.get();
        VersionBits.fromValue((versionAndHeaderSize >> 4) & 0x0F);
        HeaderSizeBits hsb = HeaderSizeBits.fromValue(versionAndHeaderSize & 0x0F);

        // Skip second byte (already parsed)
        buffer.get();

        int serializationCompression = buffer.get();
        SerializationBits ser = SerializationBits.fromValue((serializationCompression >> 4) & 0x0F);
        CompressionBits.fromValue(serializationCompression & 0x0F);

        int headerSizeInt = 4 * (int) hsb.getValue();
        int paddingSize = headerSizeInt - 3;
        while (paddingSize > 0) {
            buffer.get();
            paddingSize -= 1;
        }

        PodcastMessage message = new PodcastMessage(type, flag);
        message.setSerialization(ser.getValue());

        // Read event if present
        if (flag == MsgTypeFlagBits.WITH_EVENT) {
            if (buffer.remaining() >= 4) {
                byte[] eventBytes = new byte[4];
                buffer.get(eventBytes);
                ByteBuffer wrapper = ByteBuffer.wrap(eventBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN);
                message.setEvent(EventType.fromValue(wrapper.getInt()));
            }

            if (type != MsgType.ERROR && message.event != null
                    && message.event != EventType.START_CONNECTION
                    && message.event != EventType.FINISH_CONNECTION
                    && message.event != EventType.CONNECTION_STARTED
                    && message.event != EventType.CONNECTION_FAILED
                    && message.event != EventType.CONNECTION_FINISHED) {
                // Read sessionId
                if (buffer.remaining() >= 4) {
                    int sessionIdLength = buffer.getInt();
                    if (sessionIdLength > 0 && buffer.remaining() >= sessionIdLength) {
                        byte[] sessionIdBytes = new byte[sessionIdLength];
                        buffer.get(sessionIdBytes);
                        message.setSessionId(new String(sessionIdBytes, StandardCharsets.UTF_8));
                    }
                }
            }

            if (message.event == EventType.CONNECTION_STARTED
                    || message.event == EventType.CONNECTION_FAILED
                    || message.event == EventType.CONNECTION_FINISHED) {
                if (buffer.remaining() >= 4) {
                    int connectIdLength = buffer.getInt();
                    if (connectIdLength > 0 && buffer.remaining() >= connectIdLength) {
                        byte[] connectIdBytes = new byte[connectIdLength];
                        buffer.get(connectIdBytes);
                        message.setConnectId(new String(connectIdBytes, StandardCharsets.UTF_8));
                    }
                }
            }
        }

        // Read sequence
        if (flag == MsgTypeFlagBits.POSITIVE_SEQ || flag == MsgTypeFlagBits.NEGATIVE_SEQ) {
            if (buffer.remaining() >= 4) {
                byte[] seqBytes = new byte[4];
                buffer.get(seqBytes);
                ByteBuffer wrapper = ByteBuffer.wrap(seqBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN);
                message.setSequence(wrapper.getInt());
            }
        }

        // Read errorCode
        if (type == MsgType.ERROR) {
            if (buffer.remaining() >= 4) {
                byte[] errorCodeBytes = new byte[4];
                buffer.get(errorCodeBytes);
                ByteBuffer wrapper = ByteBuffer.wrap(errorCodeBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN);
                message.setErrorCode(wrapper.getInt());
            }
        }

        // Read payload
        if (buffer.remaining() > 0) {
            int payloadLength = buffer.getInt();
            if (payloadLength > 0 && buffer.remaining() >= payloadLength) {
                byte[] payloadBytes = new byte[payloadLength];
                buffer.get(payloadBytes);
                message.setPayload(payloadBytes);
            }
        }

        return message;
    }

    /**
     * 序列化为二进制数据
     */
    public byte[] marshal() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        buffer.write((version & 0x0F) << 4 | (headerSize & 0x0F));
        buffer.write((type.getValue() & 0x0F) << 4 | (flag.getValue() & 0x0F));
        buffer.write((serialization & 0x0F) << 4 | (compression & 0x0F));

        int headerSizeInt = 4 * (int) headerSize;
        int padding = headerSizeInt - buffer.size();
        while (padding > 0) {
            buffer.write(0);
            padding -= 1;
        }

        if (event != null) {
            buffer.write(ByteBuffer.allocate(4).putInt(event.getValue()).array());
        }

        if (sessionId != null) {
            byte[] sessionIdBytes = sessionId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(sessionIdBytes.length).array());
            buffer.write(sessionIdBytes);
        }

        if (connectId != null) {
            byte[] connectIdBytes = connectId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(connectIdBytes.length).array());
            buffer.write(connectIdBytes);
        }

        if (sequence != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(sequence).array());
        }

        if (errorCode != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(errorCode).array());
        }

        if (payload != null && payload.length > 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(payload.length).array());
            buffer.write(payload);
        }

        return buffer.toByteArray();
    }

    /**
     * 获取 payload 的文本内容
     */
    public String getPayloadText() {
        return payload != null ? new String(payload, StandardCharsets.UTF_8) : null;
    }

    @Override
    public String toString() {
        if (type == MsgType.AUDIO_ONLY_SERVER || type == MsgType.AUDIO_ONLY_CLIENT) {
            return String.format("Msg[%s, event=%s, payloadSize=%d]", type, event,
                    payload != null ? payload.length : 0);
        }
        if (type == MsgType.ERROR) {
            return String.format("Msg[ERROR, event=%s, errorCode=%d, payload=%s]", event, errorCode, getPayloadText());
        }
        return String.format("Msg[%s, event=%s, payload=%s]", type, event,
                payload != null ? (payload.length > 200 ? payload.length + "bytes" : getPayloadText()) : "null");
    }
}
