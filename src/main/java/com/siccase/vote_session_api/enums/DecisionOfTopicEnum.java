package com.siccase.vote_session_api.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DecisionOfTopicEnum {
    YES("Sim"),
    NO("Não"),
    TIE("Empate");

    private final String value;

    public String value() {
        return value;
    }
}

