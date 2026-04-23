package com.siccase.vote_session_api.controller;

import com.siccase.vote_session_api.dto.request.StartSessionDTO;
import com.siccase.vote_session_api.dto.request.TopicRequestDTO;
import com.siccase.vote_session_api.dto.response.FinishedTopicResponseDTO;
import com.siccase.vote_session_api.dto.response.ResponseDTO;
import com.siccase.vote_session_api.dto.response.SessionResponseDTO;
import com.siccase.vote_session_api.dto.response.ResultResponseDTO;
import com.siccase.vote_session_api.dto.response.TopicResponseDTO;
import com.siccase.vote_session_api.service.FinishTopicUseCase;
import com.siccase.vote_session_api.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/v1/topics")
@AllArgsConstructor
public class TopicControllerV1 {
    @Autowired
    private final TopicService service;

    @Autowired
    private final FinishTopicUseCase finishTopicUseCase;

    @PostMapping()
    @Operation(summary = "Cria um novo tópico", description = "Recebe o título de um novo tópico e cria-o na base de dados")
    public ResponseEntity<ResponseDTO> createTopic(@RequestBody TopicRequestDTO topic) {
        TopicResponseDTO response = service.createTopic(topic);

        return ResponseEntity.status(201).body(new ResponseDTO(201, "Topic created successfully", LocalDateTime.now(), response));
    }

    @PostMapping("/sessions/start")
    @Operation(summary = "Inicia uma nova sessão de votação em cima de um tópico", description = "Recebe id do tópico e duração da sessão")
    public ResponseEntity<ResponseDTO> startSession(@RequestBody StartSessionDTO startSessionDTO) {
        SessionResponseDTO response = service.startSession(startSessionDTO);
        return ResponseEntity.ok(
                new ResponseDTO(200, "Session started successfully", LocalDateTime.now(), response));
    }

    @GetMapping("/{topicId}/sessions/result")
    @Operation(summary = "Retorna o resultado da votação")
    public ResponseEntity<ResponseDTO> getSessionResult(@PathVariable(value = "topicId") String topicId) {
        ResultResponseDTO result = service.getSessionResultByTopicId(topicId);
        return ResponseEntity.ok(
                new ResponseDTO(200, "Success", LocalDateTime.now(), result));
    }

    @PostMapping("/{topicId}/sessions/stop")
    public ResponseEntity<ResponseDTO> finishTopic(@PathVariable String topicId) {
        System.out.println("Received request to finish topic with ID: " + topicId);
        FinishedTopicResponseDTO response = finishTopicUseCase.closeTopicById(java.util.UUID.fromString(topicId));
        return ResponseEntity.ok(
                new ResponseDTO(200, "Topic finished successfully", LocalDateTime.now(), response));
    }
}
