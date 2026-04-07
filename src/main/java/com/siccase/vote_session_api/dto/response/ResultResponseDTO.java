package com.siccase.vote_session_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResultResponseDTO {
    private UUID topicId;
    private String decision;
    private String sessionStatus;
    private long totalOfVotes;
    private double yesVotesPercent;
    private double noVotesPercent;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
