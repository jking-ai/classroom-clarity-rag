package com.jkingai.classroomclarity.exception;

public class DocumentLimitExceededException extends RuntimeException {
    public DocumentLimitExceededException(int maxCount) {
        super("Document limit reached. Maximum allowed: " + maxCount
                + ". Delete existing documents before uploading new ones.");
    }
}
