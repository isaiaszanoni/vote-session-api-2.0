package com.siccase.vote_session_api.unit.service;

import com.siccase.vote_session_api.enums.DecisionOfTopicEnum;
import com.siccase.vote_session_api.enums.ResponseOptionsEnum;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.model.Result;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.model.Vote;
import com.siccase.vote_session_api.repository.ResultRepository;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.repository.VoteRepository;
import com.siccase.vote_session_api.service.FinishTopicUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of FinishTopicUseCase")
public class FinishTopicUseCaseTest {
    @InjectMocks
    FinishTopicUseCase useCase;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ResultRepository resultRepository;

    private Topic getTopicActive() {
        return Topic.builder()
                .id(UUID.randomUUID())
                .title("Teste")
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .finishAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    @Test
    void closeExpiredTopics_givenMoreThanOneTopicActiveAndFinishAtLessThanEqualNow_shouldCloseAllTopics() {
        // arrange
        List<Topic> topics = List.of(getTopicActive(), getTopicActive(), getTopicActive());

        when(topicRepository.findBySessionStatusAndFinishAtLessThanEqual(
                eq(SessionStatusEnum.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(topics);

        when(resultRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(voteRepository.findAllByTopicId(any())).thenReturn(List.of());
        when(resultRepository.save(any()))
                .thenReturn(Result.builder()
                        .id(UUID.randomUUID())
                        .noVotesPercent(0)
                        .yesVotesPercent(0)
                        .totalOfVotes(0)
                        .build()
                );

        // act
        useCase.closeExpiredTopics();

        // assert
        verify(topicRepository, times(3)).save(any());
        verify(voteRepository, times(3)).findAllByTopicId(any(UUID.class));
        verify(resultRepository, times(3)).save(any(Result.class));
    }

    @Test
    void calculateResult_givenATopicWithoutVotes_shouldReturnResultObjectWithoutException() {
        // arrange
        List<Vote> votes = List.of();
        Topic topic = getTopicActive();
        Result expectedResult = Result.builder()
                .yesVotesPercent(0)
                .noVotesPercent(0)
                .decision(null)
                .topic(topic)
                .build();

        // action
        Result result = useCase.calculateResult(votes, topic);

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    void calculateResult_givenATopicWithMoreYesVotes_shouldReturnResultWithDecisionYes() {
        // arrange
        Topic topic = getTopicActive();
        List<Vote> votes = List.of(
                Vote.builder().vote(ResponseOptionsEnum.SIM).build(),
                Vote.builder().vote(ResponseOptionsEnum.SIM).build(),
                Vote.builder().vote(ResponseOptionsEnum.SIM).build(),
                Vote.builder().vote(ResponseOptionsEnum.SIM).build(),
                Vote.builder().vote(ResponseOptionsEnum.NAO).build()
        );

        // action
        Result result = useCase.calculateResult(votes, topic);

        // assert
        assertEquals(DecisionOfTopicEnum.YES, result.getDecision());
        assertEquals(80.0, result.getYesVotesPercent());
        assertEquals(20.0, result.getNoVotesPercent());
    }

    @Test
    void calculateResult_givenATopicWithMoreNoVotes_shouldReturnResultWithDecisionNo() {
        // arrange
        Topic topic = getTopicActive();
        List<Vote> votes = List.of(
                Vote.builder().vote(ResponseOptionsEnum.NAO).build(),
                Vote.builder().vote(ResponseOptionsEnum.NAO).build(),
                Vote.builder().vote(ResponseOptionsEnum.NAO).build(),
                Vote.builder().vote(ResponseOptionsEnum.NAO).build(),
                Vote.builder().vote(ResponseOptionsEnum.NAO).build(),
                Vote.builder().vote(ResponseOptionsEnum.SIM).build(),
                Vote.builder().vote(ResponseOptionsEnum.SIM).build()
        );

        // act
        Result result = useCase.calculateResult(votes, topic);

        // assert
        assertEquals(DecisionOfTopicEnum.NO, result.getDecision());
        assertEquals(28.57, result.getYesVotesPercent(), 0.01);
        assertEquals(71.43, result.getNoVotesPercent(), 0.01);
    }
}
