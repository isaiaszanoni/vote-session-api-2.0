package com.siccase.vote_session_api.controller;

import com.siccase.vote_session_api.dto.response.ResponseDTO;
import com.siccase.vote_session_api.exception.MemberAlreadyVoteException;
import com.siccase.vote_session_api.exception.ResultNotFoundException;
import com.siccase.vote_session_api.exception.SessionExpiredException;
import com.siccase.vote_session_api.exception.SessionNotActiveException;
import com.siccase.vote_session_api.exception.SessionNotFinishedException;
import com.siccase.vote_session_api.exception.TopicNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = {TopicNotFoundException.class})
    public ResponseEntity<?> handleTopicNotFound(TopicNotFoundException topicNotFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO(404, topicNotFoundException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException illegalArgumentException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, illegalArgumentException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {IllegalStateException.class})
    public ResponseEntity<?> IllegalStateException(IllegalStateException illegalStateException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, illegalStateException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException constraintViolationException) {
        // TODO: create message more user friendly
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(HttpStatus.BAD_REQUEST.value(), constraintViolationException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {SessionNotActiveException.class})
    public ResponseEntity<?> handleSessionNotActiveException(SessionNotActiveException sessionNotActiveException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, sessionNotActiveException.getMessage(), LocalDateTime.now(),null));
    }

    @ExceptionHandler(value = {SessionNotFinishedException.class})
    public ResponseEntity<?> handleSessionNotFinishedException(SessionNotFinishedException sessionNotFinishedException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, sessionNotFinishedException.getMessage(), LocalDateTime.now(),null));
    }

    @ExceptionHandler(value = {SessionExpiredException.class})
    public ResponseEntity<?> handleSessionExpired(SessionExpiredException sessionExpiredException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, sessionExpiredException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {MemberAlreadyVoteException.class})
    public ResponseEntity<?> handleMemberAlreadyVote(MemberAlreadyVoteException memberAlreadyVoteException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDTO(400, memberAlreadyVoteException.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(value = {ResultNotFoundException.class})
    public ResponseEntity<?> handleResultNotFound(ResultNotFoundException resultNotFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDTO(404, resultNotFoundException.getMessage(), LocalDateTime.now(), null));
    }

}
