package com.siccase.vote_session_api.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    @Test
    void givenAValidRequest_whenCreatingTopic_shouldCreatePendingTopicWithoutErrors() throws Exception {
        String title = "Você recomendaria a SICREDI para um amigo?";
        TopicRequestDTO request = new TopicRequestDTO(title);

        createTopic(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.sessionStatus").value(SessionStatusEnum.PENDING.name()));

        assertEquals(1, topicRepository.count());
    }

    @Test
    void givenDuplicatedTitle_whenCreatingTopic_thenShouldReturnBadRequest() throws Exception {
        TopicRequestDTO request = new TopicRequestDTO("Você gostou do nosso novo APP?");

        createTopic(request)
                .andExpect(status().isCreated());
        assertEquals(1, topicRepository.count());

        createTopic(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andDo(print());
        assertEquals(1, topicRepository.count());

    }
}