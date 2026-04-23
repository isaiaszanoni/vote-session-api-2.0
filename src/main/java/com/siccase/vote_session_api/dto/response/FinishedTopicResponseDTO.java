package com.siccase.vote_session_api.dto.response;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FinishedTopicResponseDTO {
    String topicId;
    SessionStatusEnum sessionStatus;
}
