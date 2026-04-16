package com.siccase.vote_session_api.repository;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findFirstByTitleAndSessionStatusNot(String title, SessionStatusEnum status);

    List<Topic> findBySessionStatusAndFinishAtLessThanEqual(SessionStatusEnum status, LocalDateTime date);
}
