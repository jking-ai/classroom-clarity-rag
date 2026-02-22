package com.jkingai.classroomclarity.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String error,
        String message,
        OffsetDateTime timestamp,
        String path
) {
    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, OffsetDateTime.now(), path);
    }
}
