package com.siccase.vote_session_api.validation;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.exception.SessionExpiredException;
import com.siccase.vote_session_api.exception.SessionNotActiveException;
import com.siccase.vote_session_api.model.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TopicValidation {

    public void validateTopicIsActive(Topic topic) {
        if (topic.getSessionStatus() != SessionStatusEnum.ACTIVE) {
            throw new SessionNotActiveException("Topic session is not active");
        }

        if (topic.getFinishAt().isBefore(LocalDateTime.now())) {
            throw new SessionExpiredException("Session has ended");
        }
    }
}
