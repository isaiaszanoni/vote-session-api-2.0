package com.siccase.vote_session_api.integration.service;

import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.dto.response.TopicResponseDTO;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.exception.TopicNotFoundException;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.service.TopicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class TopicServiceIntegrationTest {
    @Autowired
    private TopicService service;

    @Autowired
    private TopicRepository topicRepository;

    private void createAndPersistTopic(String title, SessionStatusEnum status) {
        Topic existingTopic = Topic.builder()
                .title(title)
                .sessionStatus(status)
                .build();
        topicRepository.save(existingTopic);
    }

    @Test
    @DisplayName("Deve retornar tópico persistido ao buscar por id existente")
    public void givenExistingTopic_whenGettingTopicById_thenShouldReturnPersistedTopic() {
        // arrange
        LocalDateTime now = LocalDateTime.now();
        createAndPersistTopic("Você é a favor da regra de negócio nova?", SessionStatusEnum.ACTIVE);
        Topic topic = Topic.builder()
                .title("Tópico de testes")
                .sessionStatus(SessionStatusEnum.PENDING)
                .startAt(now.plusDays(2))
                .finishAt(now.plusDays(12))
                .build();
        Topic savedTopic = topicRepository.save(topic);

        // act
        Topic response = service.getTopicById(savedTopic.getId());

        // assert
        assertEquals(savedTopic.getId(), response.getId());
        assertEquals(savedTopic.getTitle(), response.getTitle());
        assertEquals(savedTopic.getSessionStatus(), response.getSessionStatus());
        assertThat(savedTopic.getStartAt()).isEqualToIgnoringNanos(response.getStartAt());
        assertThat(savedTopic.getFinishAt()).isEqualToIgnoringNanos(response.getFinishAt());
    }

    @Test
    @DisplayName("Deve lançar exceção quando não existe tópico persistido com o id passado")
    public void givenInexistentTopic_whenGettingTopicById_thenShouldThrowTopicNotFoundException() {
        assertThrows(TopicNotFoundException.class, () -> service.getTopicById(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar um tópico com título já existente e status ativo")
    public void givenAValidTopicRequestDTO_whenCreateTopicWithExistentTitleAndActiveStatus_thenShouldThrowException() {
        // arrange
        String title = "Você é a favor da regra de negócio nova?";
        Topic existingTopic = Topic.builder()
                .title(title)
                .sessionStatus(SessionStatusEnum.ACTIVE)
                .build();
        createAndPersistTopic(title, SessionStatusEnum.ACTIVE);
        TopicRequestDTO request = new TopicRequestDTO(title);

        // act & assert
        assertThrows(IllegalStateException.class, () -> service.createTopic(request));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar um tópico com título já existente e status pendente")
    public void givenAValidTopicRequestDTO_whenCreateTopicWithExistentTitleAndPendingStatus_thenShouldThrowException() {
        String title = "Você é a favor da regra de negócio nova?";
        Topic existingTopic = Topic.builder()
                .title(title)
                .sessionStatus(SessionStatusEnum.PENDING)
                .build();
        topicRepository.save(existingTopic);
        TopicRequestDTO request = new TopicRequestDTO(title);

        // act & assert
        assertThrows(IllegalStateException.class, () -> service.createTopic(request));
    }

    @Test
    @DisplayName("Deve criar um novo tópico mesmo com título já existente, caso o tópico existente esteja com status finalizado")
    public void givenAValidTopicRequestDTO_whenCreateTopicWithExistentTitleButFinishedStatus_thenShouldCreateNewTopic() {
        // arrange
        String title = "Você é a favor da regra de negócio nova?";
        Topic finishedTopic = Topic.builder()
                .title(title)
                .sessionStatus(SessionStatusEnum.FINISHED)
                .build();
        topicRepository.save(finishedTopic);
        TopicRequestDTO request = new TopicRequestDTO(title);

        // act
        TopicResponseDTO response = service.createTopic(request);

        // assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getSessionStatus()).isEqualTo(SessionStatusEnum.PENDING);
    }

    @Test
    @DisplayName("Deve criar um novo tópico e retornar o tópico criado, caso o título seja válido e não exista outro tópico ativo ou pendente com o mesmo título")
    public void givenAValidTopicRequestDTO_whenCreateTopic_thenShouldSaveTopicAndReturnTopicResponseDTO() {
        // arrange
        TopicRequestDTO request = new TopicRequestDTO("Você é a favor da regra de negócio nova?");

        // act
        TopicResponseDTO response = service.createTopic(request);

        // assert
        assertThat(response)
                .isNotNull()
                .extracting(TopicResponseDTO::getTitle, TopicResponseDTO::getSessionStatus)
                .containsExactly(request.getTitle(), SessionStatusEnum.PENDING);
    }
}
