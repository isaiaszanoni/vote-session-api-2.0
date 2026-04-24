package com.siccase.vote_session_api.dto.request;

import com.siccase.vote_session_api.enums.ResponseOptionsEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class VoteRequestDTO {
    @Schema(name = "topicId", description = "id do tópico (UUID) ao qual se quer votar", example = "3d2c9bf5-a0fb-4d63-86ca-7fc436cd2df4")
    private UUID topicId;

    @Schema(name = "memberCpf", description = "CPF do membro que está votando", example = "88610617025")
    private String memberCpf;

    @Schema(name = "vote", description = "Voto do membro, deve ser 'SIM' ou 'NAO'", example = "SIM")
    private ResponseOptionsEnum vote;
}
