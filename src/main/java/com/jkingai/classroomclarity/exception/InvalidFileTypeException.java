package com.jkingai.classroomclarity.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(String receivedContentType) {
        super("Only PDF files are accepted. Received: " + receivedContentType);
    }
}
