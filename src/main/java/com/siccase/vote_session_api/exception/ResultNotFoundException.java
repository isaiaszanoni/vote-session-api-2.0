package com.siccase.vote_session_api.exception;

import java.util.UUID;

public class ResultNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Result not found with id: %s";

    public ResultNotFoundException(UUID topicId) {
        super(String.format(MESSAGE, topicId));
    }
}
