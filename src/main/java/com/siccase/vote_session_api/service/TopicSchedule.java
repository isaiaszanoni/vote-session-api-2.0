package com.siccase.vote_session_api.service;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.model.Topic;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TopicSchedule {
    private final TopicService topicService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void finishTopics() {
        log.info("Initializing routine to finish topics");
        topicService.closeExpiredTopics();
    }
}
