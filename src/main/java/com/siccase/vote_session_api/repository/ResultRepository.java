package com.siccase.vote_session_api.repository;

import com.siccase.vote_session_api.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {
    Optional<Result> findByTopicId(UUID topicId);

}
