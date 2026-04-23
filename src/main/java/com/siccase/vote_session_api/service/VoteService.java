package com.siccase.vote_session_api.service;

import com.siccase.vote_session_api.model.Vote;
import com.siccase.vote_session_api.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRepository repository;

    public List<Vote> getAllVotesByTopicId(UUID topicId) {
        return repository.findAllByTopicId(topicId);
    }
}
