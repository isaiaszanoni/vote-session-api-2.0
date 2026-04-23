package com.siccase.vote_session_api.service;

import com.siccase.vote_session_api.dto.response.FinishedTopicResponseDTO;
import com.siccase.vote_session_api.enums.DecisionOfTopicEnum;
import com.siccase.vote_session_api.enums.ResponseOptionsEnum;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.exception.SessionNotActiveException;
import com.siccase.vote_session_api.model.Result;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.model.Vote;
import com.siccase.vote_session_api.repository.ResultRepository;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinishTopicUseCase {
    private final TopicRepository topicRepository;

    private final VoteRepository voteRepository;

    private final ResultRepository resultRepository;

    public FinishedTopicResponseDTO closeTopicById(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + topicId));

        if (topic.isFinished()) {
            throw new SessionNotActiveException("Topic session is not active");
        }
        Result result = finishTopic(topic);
        return new FinishedTopicResponseDTO(result.getTopic().getId().toString(), result.getTopic().getSessionStatus());
    }

    public void closeExpiredTopics() {
        List<Topic> expiredTopics = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(
                SessionStatusEnum.ACTIVE,
                LocalDateTime.now()
        );

        expiredTopics.forEach(this::finishTopic);
    }

    public Result finishTopic(Topic topic) {
        topic.setSessionStatus(SessionStatusEnum.FINISHED);
        topic.setFinishAt(LocalDateTime.now());
        topicRepository.save(topic);
        List<Vote> votes = voteRepository.findAllByTopicId(topic.getId());

        Result result = calculateResult(votes, topic);
        Result savedResult = saveResult(result);
        log.info("New result: {} for Topic: {}", savedResult.getId().toString(), result.getTopic().getId());
        return savedResult;
    }

    public Result calculateResult(List<Vote> votes, Topic topic) {
        Result result = Result.builder()
                .topic(topic)
                .decision(null)
                .totalOfVotes(0L)
                .yesVotesPercent(0)
                .noVotesPercent(0)
                .build();

        long totalOfVotes = votes.size();
        if (totalOfVotes == 0) {
            return result;
        }
        result.setTotalOfVotes(totalOfVotes);

        long yesVotes = votes.stream()
                .filter(vote -> vote.getVote() == ResponseOptionsEnum.SIM)
                .count();
        long noVotes = totalOfVotes - yesVotes;
        double yesPercent = (double) (yesVotes * 100) / totalOfVotes;
        double noPercent = (double) (noVotes * 100) / totalOfVotes;
        result.setYesVotesPercent(yesPercent);
        result.setNoVotesPercent(noPercent);

        DecisionOfTopicEnum decision;
        if (yesVotes > noVotes) {
            decision = DecisionOfTopicEnum.YES;
        } else if (yesVotes < noVotes) {
            decision = DecisionOfTopicEnum.NO;
        } else {
            decision = DecisionOfTopicEnum.TIE;
        }
        result.setDecision(decision);

        return result;
    }

    public Result saveResult(Result result) {
        return resultRepository.save(result);
    }
}
