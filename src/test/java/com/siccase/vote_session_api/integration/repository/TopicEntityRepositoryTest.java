package com.siccase.vote_session_api.integration.repository;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class TopicEntityRepositoryTest {
    @Autowired
    private TopicRepository topicRepository;

    private Topic createTopic(SessionStatusEnum status, LocalDateTime finishAt)  {
        return Topic.builder()
                .title("Titulo")
                .sessionStatus(status)
                .startAt(LocalDateTime.now())
                .finishAt(finishAt)
                .build();
    }

    private Topic createAndPersistTopic(SessionStatusEnum status, LocalDateTime finishAt)  {
        return topicRepository.save(createTopic(status, finishAt));
    }

    @Test
    void injectedComponentsAreNotNull() {
        assertThat(topicRepository).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = SessionStatusEnum.class,
            names = {"FINISHED", "PENDING"}
    )
    void findFirstByTitleAndSessionStatusNot_givenAExistentTitle_shouldFindTopicCorrectly(SessionStatusEnum statusToExclude) {
        // arrange
        Topic savedTopic = createAndPersistTopic(SessionStatusEnum.ACTIVE, LocalDateTime.now());

        // act
        Optional<Topic> foundTopic = topicRepository.findFirstByTitleAndSessionStatusNot(savedTopic.getTitle(), statusToExclude);

        // assert
        assertThat(foundTopic)
                .isPresent()
                .get()
                .extracting(Topic::getId)
                .isEqualTo(savedTopic.getId());
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_whenTopicFinishedInPast_shouldFindIt() {
        // arrange
        LocalDateTime now = LocalDateTime.now();
        Topic savedTopic = createAndPersistTopic(SessionStatusEnum.FINISHED, now.minusHours(1));

        // act
        var finishedTopics = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.FINISHED, now);

        // assert
        assertThat(finishedTopics)
                .isNotNull()
                .hasSize(1);
        assertThat(finishedTopics.getFirst())
                .extracting(Topic::getId, Topic::getTitle)
                .containsExactly(savedTopic.getId(), savedTopic.getTitle());
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_givenTopicWithFinishedAtInPast_shouldFindActiveTopic() {
        Topic activeTopic = Topic.builder()
                .title("Active topic status")
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .finishAt(LocalDateTime.now().minusDays(1))
                .build();
        topicRepository.save(activeTopic);

        var activeTopics = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.ACTIVE, LocalDateTime.now().plusDays(1));

        assertThat(activeTopics)
                .isNotNull()
                .hasSize(1)
                .extracting(Topic::getTitle)
                .containsExactly(activeTopic.getTitle());
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_givenTopicWithFinishedAtInPast_shouldFindPendingTopic() {
        Topic pendingTopic = Topic.builder()
                .title("Active topic status")
                .sessionStatus(SessionStatusEnum.PENDING)
                .finishAt(LocalDateTime.now().minusDays(1))
                .build();
        topicRepository.save(pendingTopic);

        var activeTopics = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.PENDING, LocalDateTime.now().plusDays(1));

        assertThat(activeTopics)
                .isNotNull()
                .hasSize(1)
                .extracting(Topic::getTitle)
                .containsExactly(pendingTopic.getTitle());
    }

    @Test
    public void findBySessionStatusAndFinishAtLessThanEqual_givenATopicWithFinishedAtEqualsValue_shouldFindTopicCorrectly() {
        // arrange
        String topicTitle = "Active topic should be finished";
        LocalDateTime cutoffTime = LocalDateTime.parse("2026-04-08T15:00:00");

        topicRepository.save(
                Topic.builder()
                        .title(topicTitle)
                        .sessionStatus(SessionStatusEnum.ACTIVE)
                        .finishAt(cutoffTime)
                        .build()
        );

        // act
        List<Topic> result = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(
                SessionStatusEnum.ACTIVE,
                cutoffTime
        );

        // assert
        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .extracting(Topic::getTitle)
                .containsExactly(topicTitle);
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_whenTopicsHaveFutureFinishAt_shouldReturnEmpty() {
        // arrange
        createAndPersistTopic(SessionStatusEnum.ACTIVE, LocalDateTime.now().plusDays(1));

        // act
        List<Topic> result = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.ACTIVE, LocalDateTime.now());

        // assert
        assertThat(result)
                .isEmpty();
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_whenNoTopicsHaveASpecificStatus_shouldReturnEmpty() {
        // arrange
        Topic topicFinished = createTopic(SessionStatusEnum.FINISHED, LocalDateTime.now().minusDays(1));
        Topic topicPending = createTopic(SessionStatusEnum.PENDING, LocalDateTime.now().minusDays(1));
        topicRepository.saveAll(List.of(topicFinished, topicPending));

        // act
        List<Topic> result = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.ACTIVE, LocalDateTime.now());

        // assert
        assertThat(result)
                .isEmpty();
    }

    @Test
    @Transactional
    public void findBySessionStatusAndFinishAtLessThanEqual_whenExistsMoreThanOneTopicWithStatusAndFinishedAtLessThanEqual_shouldFindAllOfThem() {
        // arrange
        LocalDateTime cutoffTime = LocalDateTime.now();
        Topic topic1 = createTopic(SessionStatusEnum.FINISHED, cutoffTime.minusHours(2));
        Topic topic2 = createTopic(SessionStatusEnum.FINISHED, cutoffTime.minusHours(1));
        topicRepository.saveAll(List.of(topic1, topic2));

        // act
        List<Topic> result = topicRepository.findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum.FINISHED, cutoffTime);

        // assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .extracting(Topic::getId)
                .containsExactlyInAnyOrder(topic1.getId(), topic2.getId());
    }

}
