package com.siccase.vote_session_api.dto.request;

import com.siccase.vote_session_api.enums.TimeUnitEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartSessionDTO {
    @Schema(name = "topicId", description = "id do tópico a iniciar", example = "3d2c9bf5-a0fb-4d63-86ca-7fc436cd2df4")
    UUID topicId;

    @Schema(name = "duration", description = "objeto de duracao do periodo de votacao")
    Duration duration;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Duration {
        @Schema(name = "timeUnit", description = "unidade de tempo (MINUTE, HOUR, DAY)", example = "HOUR")
        TimeUnitEnum timeUnit;
        @Schema(name = "value",
                description = "valor que representa a unidade de tempo desejada, formando, por exemplo, 2 horas",
                example = "2")
        Long value;
    }
}

