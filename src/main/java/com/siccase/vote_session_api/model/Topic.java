package com.siccase.vote_session_api.model;

import com.siccase.vote_session_api.enums.SessionStatusEnum;
import com.siccase.vote_session_api.exception.SessionExpiredException;
import com.siccase.vote_session_api.exception.SessionNotActiveException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotEmpty
    private String title;

    @Column(name = "session_status")
    @Enumerated(EnumType.STRING)
    private SessionStatusEnum sessionStatus;

    private LocalDateTime startAt;

    private LocalDateTime finishAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void ensureIsActive(LocalDateTime currentTime) {
        if (this.sessionStatus != SessionStatusEnum.ACTIVE) {
            throw new SessionNotActiveException("Topic session is not active");
        }

        if (this.finishAt.isBefore(currentTime)) {
            throw new SessionExpiredException("Session has ended");
        }
    }

    public boolean isFinished() {
        return sessionStatus == SessionStatusEnum.FINISHED && finishAt != null;
    }
}


