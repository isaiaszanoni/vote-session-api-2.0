package com.siccase.vote_session_api.model;

import com.siccase.vote_session_api.enums.DecisionOfTopicEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    private Topic topic;

    private DecisionOfTopicEnum decision;

    private long totalOfVotes;

    private double yesVotesPercent;

    private double noVotesPercent;
}
