package com.soundread.sdk.podcast.protocol;

import lombok.Getter;

@Getter
public enum HeaderSizeBits {
    HeaderSize4((byte) 1);

    private final byte value;

    HeaderSizeBits(byte b) {
        this.value = b;
    }

    public static HeaderSizeBits fromValue(int value) {
        for (HeaderSizeBits type : HeaderSizeBits.values()) {
            if (type.value == value)
                return type;
        }
        throw new IllegalArgumentException("Unknown HeaderSizeBits value: " + value);
    }
}
