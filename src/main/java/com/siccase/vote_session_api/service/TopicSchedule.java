package com.siccase.vote_session_api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TopicSchedule {
    private final FinishTopicUseCase finishTopic;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void finishTopics() {
        log.info("Initializing routine to finish topics");
        finishTopic.closeExpiredTopics();
    }
}
