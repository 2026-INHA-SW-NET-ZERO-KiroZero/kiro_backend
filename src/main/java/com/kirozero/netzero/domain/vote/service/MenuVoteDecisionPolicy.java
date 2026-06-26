package com.kirozero.netzero.domain.vote.service;

import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class MenuVoteDecisionPolicy {

    private static final List<String> TIE_BREAK_PRIORITY = List.of("D", "C", "B", "A");

    private MenuVoteDecisionPolicy() {
    }

    static Optional<MenuCandidateResponse> decide(
            Map<String, Long> voteSummary,
            long submittedVoteCount,
            long participantCount,
            List<MenuCandidateResponse> candidates
    ) {
        if (participantCount <= 0 || submittedVoteCount < participantCount) {
            return Optional.empty();
        }

        long maxCandidateVotes = candidates.stream()
                .mapToLong(candidate -> voteSummary.getOrDefault(candidate.candidateLabel(), 0L))
                .max()
                .orElse(0L);
        long regenerationVotes = voteSummary.getOrDefault("E", 0L);
        if (regenerationVotes >= 2 || regenerationVotes > maxCandidateVotes) {
            return Optional.empty();
        }
        if (maxCandidateVotes <= 0) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(candidate -> voteSummary.getOrDefault(candidate.candidateLabel(), 0L) == maxCandidateVotes)
                .max(Comparator.comparingInt(candidate -> tieBreakScore(candidate.candidateLabel())));
    }

    private static int tieBreakScore(String candidateLabel) {
        int index = TIE_BREAK_PRIORITY.indexOf(candidateLabel);
        return index < 0 ? -1 : TIE_BREAK_PRIORITY.size() - index;
    }
}
