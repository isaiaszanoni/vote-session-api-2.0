package com.siccase.vote_session_api.service;

import com.siccase.vote_session_api.dto.request.StartSessionDTO;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.dto.response.SessionResponseDTO;
import com.siccase.vote_session_api.dto.response.ResultResponseDTO;
import com.siccase.vote_session_api.dto.response.TopicResponseDTO;
import com.siccase.vote_session_api.enums.DecisionOfTopicEnum;
import com.siccase.vote_session_api.enums.ResponseOptionsEnum;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.exception.ResultNotFoundException;
import com.siccase.vote_session_api.exception.SessionNotFinishedException;
import com.siccase.vote_session_api.exception.TopicNotFoundException;
import com.siccase.vote_session_api.mapper.ResultMapper;
import com.siccase.vote_session_api.model.Result;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.model.Vote;
import com.siccase.vote_session_api.repository.ResultRepository;
import com.siccase.vote_session_api.repository.TopicRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {
    private static final long DEFAULT_DURATION_SESSION_MINUTES = 1;

    private final TopicRepository repository;

    private final ResultRepository resultRepository;

    private final VoteService voteService;

    public TopicResponseDTO createTopic(TopicRequestDTO topic) {
        repository.findFirstByTitleAndSessionStatusNot(topic.getTitle(), SessionStatusEnum.FINISHED).ifPresent(existentTopic -> {
            if (existentTopic.getSessionStatus() == SessionStatusEnum.PENDING || existentTopic.getSessionStatus() == SessionStatusEnum.ACTIVE) {
                throw new IllegalStateException("Already exists another topic active or pending with the same title");
            }
        });

        Topic newTopic = Topic.builder()
                .title(topic.getTitle())
                .sessionStatus(SessionStatusEnum.PENDING)
                .build();

        Topic savedTopic = repository.save(newTopic);
        return new TopicResponseDTO(savedTopic.getId(), savedTopic.getTitle(), savedTopic.getSessionStatus());
    }

    public SessionResponseDTO startSession(StartSessionDTO startSession) {
        Topic topic = repository.findById(startSession.getTopicId())
                .orElseThrow(() -> new TopicNotFoundException(startSession.getTopicId()));
        topicCanBeStarted(topic);

        topic.setFinishAt(calculateFinishDate(startSession.getDuration()));
        topic.setStartAt(LocalDateTime.now());
        topic.setSessionStatus(SessionStatusEnum.ACTIVE);

        log.info("Initializing session of topic: {}", topic.getId());
        Topic updatedTopic = repository.save(topic);
        return new SessionResponseDTO(updatedTopic.getId(),
                updatedTopic.getTitle(),
                updatedTopic.getSessionStatus(),
                updatedTopic.getStartAt(),
                updatedTopic.getFinishAt()
        );
    }

    protected void topicCanBeStarted(Topic topic) {
        if (topic == null) {
            throw new IllegalArgumentException("Topic should not be null");
        }

        if (topic.getSessionStatus() == SessionStatusEnum.FINISHED) {
            throw new IllegalStateException("Topic is already finished. Please open another topic and session");
        }

        if (topic.getSessionStatus() == SessionStatusEnum.ACTIVE) {
            throw  new IllegalStateException("Topic is already active. Consider to vote in this session");
        }

        if (topic.getFinishAt() != null) {
            throw new IllegalStateException("Topic is already started");
        }
    }

    public LocalDateTime calculateFinishDate(StartSessionDTO.Duration duration) {
        LocalDateTime now = LocalDateTime.now();

        if (duration == null || duration.getValue() == null || duration.getTimeUnit() == null) {
            log.info("Received session with incomplete value to duration... " +
                    "setting the default time: {} minute", DEFAULT_DURATION_SESSION_MINUTES);
            return now.plusMinutes(DEFAULT_DURATION_SESSION_MINUTES);
        }

        if (duration.getValue() <= 0) {
            throw new IllegalArgumentException("Duration should have value greater than zero");
        }

        return switch (duration.getTimeUnit()) {
            case MINUTE -> now.plusMinutes(duration.getValue());
            case HOUR -> now.plusHours(duration.getValue());
            case DAY -> now.plusDays(duration.getValue());
        };
    }

    public Topic getTopicById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->  new TopicNotFoundException(id));
    }

    public ResultResponseDTO getSessionResultByTopicId(String topicId) {
        UUID topicUUID = UUID.fromString(topicId);
        Result result = resultRepository.findByTopicId(topicUUID).orElseThrow(() -> new ResultNotFoundException(topicUUID));
        return ResultMapper.INSTANCE.resultToResultResponseDTO( result );
    }

    public void validateTopicIsFinished(Topic topic) {
        if (topic.getSessionStatus() != SessionStatusEnum.FINISHED) {
            throw new SessionNotFinishedException("Topic " + topic.getId().toString() + " is not finished");
        }

        if (topic.getFinishAt() == null) {
            throw new IllegalStateException("Topic should have FinishedAt date to get result");
        }
    }

    public void closeExpiredTopics() {
        List<Topic> expiredTopics = repository.findBySessionStatusAndFinishAtBefore(
                SessionStatusEnum.ACTIVE,
                LocalDateTime.now()
        );

        expiredTopics.forEach(this::finishTopic);
    }

    public void finishTopic(Topic topic) {
        topic.setSessionStatus(SessionStatusEnum.FINISHED);
        topic.setFinishAt(LocalDateTime.now());
        repository.save(topic);

        List<Vote> votes = voteService.getAllVotesByTopicId(topic.getId());
        if (votes == null || votes.isEmpty()) {
            return;
        }

        Result result = calculateResult(votes, topic);
        saveResult(result);
    }

    public Result calculateResult(List<Vote> votes, Topic topic) {
        long totalOfVotes = votes.size();
        long yesVotes = votes.stream()
                .filter(vote -> vote.getVote() == ResponseOptionsEnum.SIM)
                .count();
        long noVotes = totalOfVotes - yesVotes;

        double yesPercent = (double) (yesVotes * 100) / totalOfVotes;
        double noPercent = (double) (noVotes * 100) / totalOfVotes;

        DecisionOfTopicEnum decision;
        if (yesVotes > noVotes) {
            decision = DecisionOfTopicEnum.YES;
        } else if (yesVotes < noVotes) {
            decision = DecisionOfTopicEnum.NO;
        } else {
            decision = DecisionOfTopicEnum.WITHDRAW;
        }

        return Result.builder()
                .topic(topic)
                .decision(decision)
                .totalOfVotes(totalOfVotes)
                .yesVotesPercent(yesPercent)
                .noVotesPercent(noPercent)
                .build();
    }

    public Result saveResult(Result result) {
        return resultRepository.save(result);
    }
}
