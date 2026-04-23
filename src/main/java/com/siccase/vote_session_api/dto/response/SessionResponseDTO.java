package com.siccase.vote_session_api.dto.response;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SessionResponseDTO {
    private UUID topicId;
    private String title;
    private SessionStatusEnum sessionStatus;
    private LocalDateTime startAt;
    private LocalDateTime finishAt;
}
