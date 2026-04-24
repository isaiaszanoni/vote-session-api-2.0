package com.siccase.vote_session_api.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siccase.vote_session_api.dto.request.StartSessionDTO;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.dto.request.VoteRequestDTO;
import com.siccase.vote_session_api.dto.response.ResponseDTO;
import com.siccase.vote_session_api.enums.ResponseOptionsEnum;
import com.siccase.vote_session_api.enums.TimeUnitEnum;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.repository.VoteRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class VoteControllerV1IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private TopicRepository topicRepository;

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

    private void startTopic(StartSessionDTO request) throws Exception {
        mockMvc.perform(post("/api/v1/topics/sessions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void finishTopic(String topicId) throws Exception {
        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions/stop", topicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    private String createStartAndFinishTopicReturningTopicId(TopicRequestDTO request) throws Exception {
        String topicId = createTopicAndGetId(request);
        StartSessionDTO startSessionRequest = new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.HOUR, 10L)
        );
        startTopic(startSessionRequest);
        finishTopic(topicId);
        return topicId;
    }

    private String createAndStartTopic(TopicRequestDTO request) throws Exception {
        String topicId = createTopicAndGetId(request);
        StartSessionDTO startSessionRequest = new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.HOUR, 10L)
        );
        startTopic(startSessionRequest);
        return topicId;
    }

    private ResultActions performVote(VoteRequestDTO requestDTO) throws Exception {
        return mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)));
    }

    private void performVoteAndAssert(VoteRequestDTO requestDTO) throws Exception {
        mockMvc.perform(post("/api/v1/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Vote computed successfully"));
    }

    private List<String> mockUsersCpf() {
        return List.of(
                "06527659040",
                "69413751080",
                "60013470000",
                "41934661082",
                "710.476.980-30",
                "216.945.090-44",
                "000.279.700-35",
                "635.633.360-08",
                "601.487.650-25",
                "295.759.310-68"
        );
    }

    private void createSessionExpiredButNotFinished(String topicId) {
        Topic topic = topicRepository.findById(UUID.fromString(topicId))
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        topic.setFinishAt(LocalDateTime.now().minusMinutes(1));

        topicRepository.save(topic);
    }

    @Test
    void givenAValidTopicSession_whenVoting_shouldComputeVoteSuccessfully() throws Exception {
        // arrange
        String title = "Você aprova as novas regras de empréstimo para negativados?";
        TopicRequestDTO request = new TopicRequestDTO(title);

        String topicId = createTopicAndGetId(request);
        StartSessionDTO startSessionRequest = new StartSessionDTO(
                UUID.fromString(topicId), new StartSessionDTO.Duration(TimeUnitEnum.MINUTE, 2L)
        );
        startTopic(startSessionRequest);

        // act & assert
        for (String cpf : mockUsersCpf()) {
            performVoteAndAssert(new VoteRequestDTO(UUID.fromString(topicId), cpf, ResponseOptionsEnum.SIM));
        }

        assertEquals(voteRepository.count(), mockUsersCpf().size());
    }

    @Test
    void givenATopicWithStatusFinished_whenVoting_shouldNotComputeVote() throws Exception {
        String topicId = createStartAndFinishTopicReturningTopicId(new TopicRequestDTO("Você gostou do nosso novo APP?"));

        performVote(new VoteRequestDTO(UUID.fromString(topicId), "12345678900", ResponseOptionsEnum.NAO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Topic session is not active"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenAMemberThatAlreadyVotedInTheTopicSession_whenVotingAgain_shouldNotComputeVote() throws Exception {
        String topicId = createAndStartTopic(new TopicRequestDTO("Você gostou do nosso novo APP?"));

        String memberCpf = "12345678900";
        performVoteAndAssert(new VoteRequestDTO(UUID.fromString(topicId), memberCpf, ResponseOptionsEnum.SIM));

        performVote(new VoteRequestDTO(UUID.fromString(topicId), memberCpf, ResponseOptionsEnum.NAO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Member already vote, this topic just accepts one vote by member"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenATopicWithStatusPending_whenVoting_shouldNotComputeVote() throws Exception {
        String topicId = createTopicAndGetId(new TopicRequestDTO("Você gostou do nosso novo APP?"));

        performVote(new VoteRequestDTO(UUID.fromString(topicId), "12345678900", ResponseOptionsEnum.SIM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Topic session is not active"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenAnExpiredSessionNotYetFinalized_whenVoting_shouldNotComputeVote() throws Exception {
        String topicId = createAndStartTopic(new TopicRequestDTO("Teste de sessão expirada, mas não finalizada"));

        createSessionExpiredButNotFinished(topicId);

        performVote(new VoteRequestDTO(UUID.fromString(topicId), "12345678900", ResponseOptionsEnum.SIM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Session has ended"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}