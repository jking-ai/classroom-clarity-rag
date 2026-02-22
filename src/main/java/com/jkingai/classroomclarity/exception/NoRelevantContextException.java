package com.jkingai.classroomclarity.exception;

public class NoRelevantContextException extends RuntimeException {

    public NoRelevantContextException(double threshold) {
        super("No document chunks met the similarity threshold of " + threshold
                + " for the given question. Try lowering the threshold or uploading more relevant documents.");
    }
}
