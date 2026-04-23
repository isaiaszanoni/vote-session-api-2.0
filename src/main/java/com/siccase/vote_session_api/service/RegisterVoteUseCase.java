package com.siccase.vote_session_api.service;

import com.siccase.vote_session_api.dto.request.VoteRequestDTO;
import com.siccase.vote_session_api.dto.response.VoteResponseDTO;
import com.siccase.vote_session_api.exception.MemberAlreadyVoteException;
import com.siccase.vote_session_api.exception.TopicNotFoundException;
import com.siccase.vote_session_api.model.Topic;
import com.siccase.vote_session_api.model.Vote;
import com.siccase.vote_session_api.repository.TopicRepository;
import com.siccase.vote_session_api.repository.VoteRepository;
import com.siccase.vote_session_api.utils.CpfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterVoteUseCase {
    private final VoteRepository voteRepository;

    private final TopicRepository topicRepository;

    public Optional<Vote> getVoteByMemberCpfAndTopicId(String memberCpf, UUID topicId) {
        String cpfWithMask = CpfUtils.applyCpfMask(memberCpf);
        return voteRepository.findByMemberCpfAndTopicId(cpfWithMask,topicId);
    }

    public VoteResponseDTO registerVote(VoteRequestDTO voteRequest) {
        Topic topic = topicRepository.findById(voteRequest.getTopicId())
                .orElseThrow(() -> new TopicNotFoundException(voteRequest.getTopicId()));

        topic.ensureIsActive(LocalDateTime.now());

        if (getVoteByMemberCpfAndTopicId(voteRequest.getMemberCpf(), topic.getId()).isPresent()) {
            throw new MemberAlreadyVoteException();
        }

        voteRequest.setMemberCpf(CpfUtils.applyCpfMask(voteRequest.getMemberCpf()));
        Vote vote = Vote.builder()
                .topicId(topic.getId())
                .vote(voteRequest.getVote())
                .memberCpf(voteRequest.getMemberCpf())
                .build();

        Vote voteSaved = voteRepository.save(vote);
        log.info("Vote registered successfully for topicId: {}", voteSaved.getTopicId());
        return new VoteResponseDTO(voteSaved.getMemberCpf(), voteSaved.getVote(), voteSaved.getCreatedAt());
    }
}
