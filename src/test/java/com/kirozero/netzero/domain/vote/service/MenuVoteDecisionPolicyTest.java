package com.kirozero.netzero.domain.vote.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MenuVoteDecisionPolicyTest {

    @Test
    void doesNotConfirmBeforeAllParticipantsVoteEvenWhenCandidateHasTwoVotes() {
        var decision = MenuVoteDecisionPolicy.decide(
                voteSummary(2, 0, 0, 0, 0),
                2,
                4,
                candidates()
        );

        assertTrue(decision.isEmpty());
    }

    @Test
    void confirmsHighestVotedCandidateAfterAllParticipantsVote() {
        var decision = MenuVoteDecisionPolicy.decide(
                voteSummary(2, 1, 1, 0, 0),
                4,
                4,
                candidates()
        );

        assertTrue(decision.isPresent());
        assertEquals("A", decision.get().candidateLabel());
    }

    @Test
    void prefersCandidateCloserToDWhenThereIsATwoToTwoTie() {
        var decision = MenuVoteDecisionPolicy.decide(
                voteSummary(2, 0, 0, 2, 0),
                4,
                4,
                candidates()
        );

        assertTrue(decision.isPresent());
        assertEquals("D", decision.get().candidateLabel());
    }

    @Test
    void prefersLowCarbonSideWhenEveryCandidateGetsOneVote() {
        var decision = MenuVoteDecisionPolicy.decide(
                voteSummary(1, 1, 1, 1, 0),
                4,
                4,
                candidates()
        );

        assertTrue(decision.isPresent());
        assertEquals("D", decision.get().candidateLabel());
    }

    @Test
    void doesNotConfirmWhenRegenerationVoteWins() {
        var decision = MenuVoteDecisionPolicy.decide(
                voteSummary(1, 0, 1, 0, 2),
                4,
                4,
                candidates()
        );

        assertTrue(decision.isEmpty());
    }

    private Map<String, Long> voteSummary(long a, long b, long c, long d, long e) {
        return Map.of(
                "A", a,
                "B", b,
                "C", c,
                "D", d,
                "E", e
        );
    }

    private List<MenuCandidateResponse> candidates() {
        return List.of(
                candidate("A", "GENERAL"),
                candidate("B", "GENERAL"),
                candidate("C", "LOW_CARBON"),
                candidate("D", "LOW_CARBON")
        );
    }

    private MenuCandidateResponse candidate(String label, String type) {
        return new MenuCandidateResponse(
                label,
                label + " 메뉴",
                type,
                List.of(),
                List.of(),
                List.of(),
                30,
                "LOW",
                "테스트",
                List.of(),
                List.of()
        );
    }
}
