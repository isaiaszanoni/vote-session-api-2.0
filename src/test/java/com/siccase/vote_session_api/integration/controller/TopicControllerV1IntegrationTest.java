package com.siccase.vote_session_api.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siccase.vote_session_api.dto.request.StartSessionDTO;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.enums.TimeUnitEnum;
import com.siccase.vote_session_api.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class TopicControllerV1IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicRepository topicRepository;

    private ResultActions createTopic(TopicRequestDTO request) throws Exception {
        return mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private String createTopicAndGetId(TopicRequestDTO request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        if (result.getResponse().getStatus() != 201) {
            throw new RuntimeException("Failed to create topic: " + responseJson);
        }

        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode idNode = root.path("data").path("id");

        if (idNode.isMissingNode() || idNode.asText().isEmpty()) {
            throw new RuntimeException("Response JSON does not contain topic ID: " + responseJson);
        }

        return idNode.asText();
    }

    private ResultActions startTopic(StartSessionDTO request) throws Exception {
        return mockMvc.perform(post("/api/v1/topics/sessions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions finishTopic(String topicId) throws Exception {
        return mockMvc.perform(post("/api/v1/topics/{topicId}/sessions/stop", topicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    @Test
    void givenAValidRequest_whenCreatingTopic_shouldCreatePendingTopicWithoutErrors() throws Exception {
        // arrange
        String title = "Você recomendaria a SICREDI para um amigo?";
        TopicRequestDTO request = new TopicRequestDTO(title);

        // act & assert
        createTopic(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.PENDING.name()));

        assertEquals(1, topicRepository.count());
    }

    @Test
    void givenDuplicatedTitle_whenCreatingTopic_thenShouldReturnBadRequest() throws Exception {
        // arrange
        TopicRequestDTO request = new TopicRequestDTO("Você gostou do nosso novo APP?");

        createTopic(request)
                .andExpect(status().isCreated());
        assertEquals(1, topicRepository.count());

        // act & assert
        createTopic(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
        assertEquals(1, topicRepository.count());
    }

    @Test
    void givenATopicWithPendingStatus_whenStartingSession_thenShouldStartSessionWithoutErrors() throws Exception {
        // arrange
        String title = "Você aprova as novas regras de empréstimo para negativados?";
        TopicRequestDTO request = new TopicRequestDTO(title);

        String topicId = createTopicAndGetId(request);

        StartSessionDTO startSessionRequest = new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.MINUTE, 2L)
        );

        // act & assert
        startTopic(startSessionRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicId))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.ACTIVE.name()));
    }

    @Test
    void givenATopicActive_whenFinishTopic_thenShouldFinishGivenTopicWithoutErrors() throws Exception {
        // arrange
        String title = "Você aprova as novas regras de empréstimo para pensionistas?";
        TopicRequestDTO request = new TopicRequestDTO(title);
        String topicId = createTopicAndGetId(request);
        startTopic(new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.MINUTE, 2L)
        ));

        // act & assert
        finishTopic(topicId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicId))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.FINISHED.name()));
    }

    @Test
    void givenATopicWithPendingStatus_whenStartingSession_thenShouldStartSessionWithDefaultDuration() throws Exception {
        // arrange
        TopicRequestDTO request = new TopicRequestDTO("Você aprova as novas regras de empréstimo para negativados?");

        String topicId = createTopicAndGetId(request);

        StartSessionDTO startSessionRequest = new StartSessionDTO(
                UUID.fromString(topicId), null
        );

        // act & assert
        startTopic(startSessionRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicId))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.ACTIVE.name()));
    }

    @Test
    void givenAFinishedTopic_whenGettingSessionResult_whenShouldReturnResultOfGivenTopicSession() throws Exception {
        // arrange
        String topicId = createTopicAndGetId(new TopicRequestDTO("Você das novas negras para associados?"));

        StartSessionDTO startSessionDTO = new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.HOUR, 1L)
        );
        startTopic(startSessionDTO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicId))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.ACTIVE.name()));

        finishTopic(topicId);

        // act & assert
        mockMvc.perform(get("/api/v1/topics/{topicId}/sessions/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicId));
    }
}