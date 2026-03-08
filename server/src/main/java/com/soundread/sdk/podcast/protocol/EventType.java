package com.soundread.sdk.podcast.protocol;

import lombok.Getter;

@Getter
public enum EventType {
    NONE(0),

    // Upstream Connection events
    START_CONNECTION(1),
    FINISH_CONNECTION(2),

    // Downstream Connection events
    CONNECTION_STARTED(50),
    CONNECTION_FAILED(51),
    CONNECTION_FINISHED(52),

    // Upstream Session events
    START_SESSION(100),
    CANCEL_SESSION(101),
    FINISH_SESSION(102),

    // Downstream Session events
    SESSION_STARTED(150),
    SESSION_CANCELED(151),
    SESSION_FINISHED(152),
    SESSION_FAILED(153),
    USAGE_RESPONSE(154),

    // Upstream General events
    TASK_REQUEST(200),

    // Downstream Podcast events
    PODCAST_ROUND_START(360),
    PODCAST_ROUND_RESPONSE(361),
    PODCAST_ROUND_END(362),
    PODCAST_END(363);

    private final int value;

    EventType(int value) {
        this.value = value;
    }

    public static EventType fromValue(int value) {
        for (EventType type : EventType.values()) {
            if (type.value == value)
                return type;
        }
        throw new IllegalArgumentException("Unknown EventType value: " + value);
    }
}
