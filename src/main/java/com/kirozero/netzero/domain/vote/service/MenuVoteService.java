package com.kirozero.netzero.domain.vote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.vote.dto.MenuVoteRequest;
import com.kirozero.netzero.domain.vote.dto.MenuVoteResponse;
import com.kirozero.netzero.domain.vote.dto.SelectedMenuSummaryResponse;
import com.kirozero.netzero.domain.vote.entity.MenuVote;
import com.kirozero.netzero.domain.vote.enums.VoteType;
import com.kirozero.netzero.domain.vote.repository.MenuVoteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MenuVoteService {

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final MenuVoteRepository menuVoteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public MenuVoteResponse vote(Long slotId, String authorizationHeader, MenuVoteRequest request) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));

        validateVoteState(slot, user);
        List<MenuCandidateResponse> candidates = readCandidates(slot.getCandidatesJson());
        VotePayload payload = validateVotePayload(request, candidates);

        MenuVote vote = MenuVote.create(slot, user, payload.candidateLabel(), request.voteType(), normalizedReason(request));
        menuVoteRepository.save(vote);

        List<MenuVote> votes = menuVoteRepository.findBySlotIdAndRecommendationCount(slotId, slot.getRecommendationCount());
        Map<String, Long> voteSummary = buildVoteSummary(votes);
        long participantCount = sessionParticipantRepository.countBySlotId(slot.getId());
        MenuCandidateResponse confirmedCandidate = MenuVoteDecisionPolicy.decide(
                        voteSummary,
                        votes.size(),
                        participantCount,
                        candidates
                )
                .orElse(null);

        SelectedMenuSummaryResponse selectedMenu = null;
        if (confirmedCandidate != null) {
            slot.selectMenu(writeSelectedMenuJson(confirmedCandidate));
            selectedMenu = new SelectedMenuSummaryResponse(
                    confirmedCandidate.candidateLabel(),
                    confirmedCandidate.menuName(),
                    confirmedCandidate.menuType()
            );
        } else if (votes.size() >= participantCount) {
            slot.reopenForRecommendation();
        }

        return new MenuVoteResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                voteSummary,
                selectedMenu != null,
                selectedMenu == null ? null : selectedMenu.candidateLabel(),
                selectedMenu,
                slot.getStatus()
        );
    }

    private void validateVoteState(Slot slot, User user) {
        if (slot.getStatus() != SlotStatus.MENU_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Votes can only be submitted after recommendations.");
        }
        if (!hasText(slot.getCandidatesJson())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu candidates are missing.");
        }
        if (hasText(slot.getSelectedMenuJson())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu is already confirmed.");
        }
        if (slot.getRecommendationCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recommendation count is missing.");
        }
        if (!sessionParticipantRepository.existsBySlotIdAndUserId(slot.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only session participants can vote.");
        }
        if (menuVoteRepository.existsBySlotIdAndVoterIdAndRecommendationCount(
                slot.getId(),
                user.getId(),
                slot.getRecommendationCount()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already voted for this recommendation.");
        }
    }

    private VotePayload validateVotePayload(MenuVoteRequest request, List<MenuCandidateResponse> candidates) {
        if (request.voteType() == VoteType.E) {
            if (!hasText(request.reasonText())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E vote requires reasonText.");
            }
            if (hasText(request.candidateLabel())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E vote must not include candidateLabel.");
            }
            return new VotePayload(null);
        }

        if (!hasText(request.candidateLabel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "candidateLabel is required.");
        }

        String candidateLabel = request.candidateLabel().trim().toUpperCase();
        if (!candidateLabel.equals(request.voteType().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "candidateLabel must match voteType.");
        }

        boolean candidateExists = candidates.stream()
                .anyMatch(candidate -> candidate.candidateLabel().equals(candidateLabel));
        if (!candidateExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate label is not in current recommendations.");
        }

        return new VotePayload(candidateLabel);
    }

    private List<MenuCandidateResponse> readCandidates(String candidatesJson) {
        try {
            return objectMapper.readValue(unwrapStoredJson(candidatesJson), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu candidates cannot be parsed.", e);
        }
    }

    private String unwrapStoredJson(String value) throws JsonProcessingException {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return objectMapper.readValue(trimmed, String.class);
        }
        return trimmed;
    }

    private Map<String, Long> buildVoteSummary(List<MenuVote> votes) {
        Map<String, Long> voteSummary = new LinkedHashMap<>();
        for (VoteType voteType : VoteType.values()) {
            voteSummary.put(voteType.name(), 0L);
        }
        for (MenuVote vote : votes) {
            voteSummary.computeIfPresent(vote.getVoteType().name(), (ignored, count) -> count + 1);
        }
        return voteSummary;
    }

    private String writeSelectedMenuJson(MenuCandidateResponse selectedMenu) {
        try {
            return objectMapper.writeValueAsString(selectedMenu);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Selected menu cannot be serialized.", e);
        }
    }

    private String normalizedReason(MenuVoteRequest request) {
        return hasText(request.reasonText()) ? request.reasonText().trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record VotePayload(String candidateLabel) {
    }
}
