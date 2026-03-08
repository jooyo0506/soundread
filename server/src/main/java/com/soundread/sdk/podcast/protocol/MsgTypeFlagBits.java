package com.soundread.sdk.podcast.protocol;

import lombok.Getter;

@Getter
public enum MsgTypeFlagBits {
    NO_SEQ((byte) 0),
    POSITIVE_SEQ((byte) 0b1),
    LAST_NO_SEQ((byte) 0b10),
    NEGATIVE_SEQ((byte) 0b11),
    WITH_EVENT((byte) 0b100);

    private final byte value;

    MsgTypeFlagBits(byte value) {
        this.value = value;
    }

    public static MsgTypeFlagBits fromValue(int value) {
        for (MsgTypeFlagBits flag : MsgTypeFlagBits.values()) {
            if (flag.value == value)
                return flag;
        }
        throw new IllegalArgumentException("Unknown MsgTypeFlagBits value: " + value);
    }
}
