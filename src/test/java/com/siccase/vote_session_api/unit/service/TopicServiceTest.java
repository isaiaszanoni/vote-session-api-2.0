package com.siccase.vote_session_api.unit.service;

import com.siccase.vote_session_api.dto.request.StartSessionDTO;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.enums.TimeUnitEnum;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.service.TopicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of TopicService")
public class TopicServiceTest {
    @InjectMocks
    private TopicService service;
    @Mock
    private TopicRepository repository;

    @Test
    void createTopicTest() {
        when(repository.findFirstByTitleAndSessionStatusNot(anyString(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new Topic());

        service.createTopic(new TopicRequestDTO("Teste"));

        verify(repository, times(1)).findFirstByTitleAndSessionStatusNot(anyString(), any());
        verify(repository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTopicAlreadyExistsWithPendingStatus() {
        String title = "Teste";
        TopicRequestDTO request = new TopicRequestDTO(title);
        Topic existingTopic = Topic.builder().title(title).sessionStatus(SessionStatusEnum.PENDING).build();

        when(repository.findFirstByTitleAndSessionStatusNot(anyString(), any()))
                .thenReturn(Optional.of(existingTopic));

        var exception = assertThrows(IllegalStateException.class,
                () -> service.createTopic(request));
        assertEquals("Already exists another topic active or pending with the same title",
                exception.getMessage());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTopicAlreadyExistsWithActiveStatus() {
        String title = "Teste";
        TopicRequestDTO request = new TopicRequestDTO(title);
        Topic existingTopic = Topic.builder()
                .title(title)
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .build();

        when(repository.findFirstByTitleAndSessionStatusNot(anyString(), any()))
                .thenReturn(Optional.of(existingTopic));

        var exception = assertThrows(IllegalStateException.class,
                () -> service.createTopic(request));

        assertEquals("Already exists another topic active or pending with the same title",
                exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void givenAnTopicNullShouldNotCanBeStartedByTopicCanBeStarted() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.topicCanBeStarted(null));
        assertEquals("Topic should not be null", exception.getMessage());
    }

    @Test
    void givenAnTopicFinishedShouldNotCanBeStartedAgain() {
        Topic topic = Topic.builder().title("teste").sessionStatus(SessionStatusEnum.FINISHED).build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.topicCanBeStarted(topic));
        assertEquals("Topic is already finished. Please open another topic and session", exception.getMessage());
    }

    @Test
    void givenAnTopicWithFinishAtShouldNotCanBeStarted() {
        Topic topic = Topic.builder()
                .title("teste")
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .finishAt(LocalDateTime.now().plusMinutes(30))
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.topicCanBeStarted(topic));
        assertEquals("Topic is already active. Consider to vote in this session", exception.getMessage());
    }

    @Test
    void givenAnValidTopicShouldNotThrowException() {
        Topic topic = Topic.builder()
                .title("teste")
                .sessionStatus(SessionStatusEnum.PENDING)
                .finishAt(null)
                .build();

        assertDoesNotThrow(() -> service.topicCanBeStarted(topic));
    }

    @Test
    void givenAnTopicWithPendingStatusAndFinishAtShouldNotCanBeStarted() {
        Topic topic = Topic.builder()
                .title("teste")
                .sessionStatus(SessionStatusEnum.PENDING)
                .finishAt(LocalDateTime.now().plusMinutes(30))
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.topicCanBeStarted(topic));
        assertEquals("Topic is already started", exception.getMessage());
    }

    @Test
    void givenAnTopicWithActiveStatusShouldNotCanBeStarted() {
        Topic topic = Topic.builder()
                .title("teste")
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .finishAt(null)
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.topicCanBeStarted(topic));
        assertEquals("Topic is already active. Consider to vote in this session", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTopicIsNull() {
        Topic topic = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.topicCanBeStarted(topic)
        );

        assertEquals("Topic should not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTopicIsFinished() {
        Topic topic = Topic.builder()
                .title("Test")
                .sessionStatus(SessionStatusEnum.FINISHED)
                .build();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.topicCanBeStarted(topic)
        );

        assertEquals(
                "Topic is already finished. Please open another topic and session",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenTopicIsActive() {
        Topic topic = Topic.builder()
                .title("Test Topic")
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .build();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.topicCanBeStarted(topic)
        );

        assertEquals(
                "Topic is already active. Consider to vote in this session",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenValueIsNegative() {
        StartSessionDTO.Duration duration = new StartSessionDTO.Duration();
        duration.setTimeUnit(TimeUnitEnum.DAY);
        duration.setValue(-10L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateFinishDate(duration)
        );

        assertTrue(exception.getMessage().contains("greater than zero"));
    }

    @Test
    void shouldThrowExceptionWhenValueIsZero() {
        StartSessionDTO.Duration duration = new StartSessionDTO.Duration();
        duration.setTimeUnit(TimeUnitEnum.MINUTE);
        duration.setValue(0L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateFinishDate(duration)
        );

        assertTrue(exception.getMessage().contains("greater than zero"));
    }

    @Test
    void shouldCalculateMinutesCorrectly() {
        StartSessionDTO.Duration duration = new StartSessionDTO.Duration();
        duration.setTimeUnit(TimeUnitEnum.MINUTE);
        duration.setValue(30L);

        LocalDateTime result = service.calculateFinishDate(duration);

        LocalDateTime expected = LocalDateTime.now().plusMinutes(30);
        assertEquals(expected.getMinute(), result.getMinute());
    }

    @Test
    void shouldCalculateDaysCorrectly() {
        StartSessionDTO.Duration duration = new StartSessionDTO.Duration();
        duration.setTimeUnit(TimeUnitEnum.DAY);
        duration.setValue(3L);

        LocalDateTime result = service.calculateFinishDate(duration);

        LocalDateTime expected = LocalDateTime.now().plusDays(3);
        assertEquals(expected.getDayOfMonth(), result.getDayOfMonth());
        assertEquals(expected.getMonth(), result.getMonth());
    }

    // TODO: implements nested test to validate default times


}
