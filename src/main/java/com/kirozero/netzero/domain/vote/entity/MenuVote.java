package com.kirozero.netzero.domain.vote.entity;

import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.vote.enums.VoteType;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "menu_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_menu_votes_slot_voter_recommendation",
                columnNames = {"slot_id", "voter_id", "recommendation_count"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @Column(name = "candidate_label", length = 1)
    private String candidateLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 1)
    private VoteType voteType;

    @Column(name = "reason_text", columnDefinition = "text")
    private String reasonText;

    @Column(name = "recommendation_count", nullable = false)
    private int recommendationCount;

    private MenuVote(
            Slot slot,
            User voter,
            String candidateLabel,
            VoteType voteType,
            String reasonText,
            int recommendationCount
    ) {
        this.slot = slot;
        this.voter = voter;
        this.candidateLabel = candidateLabel;
        this.voteType = voteType;
        this.reasonText = reasonText;
        this.recommendationCount = recommendationCount;
    }

    public static MenuVote create(
            Slot slot,
            User voter,
            String candidateLabel,
            VoteType voteType,
            String reasonText
    ) {
        return new MenuVote(slot, voter, candidateLabel, voteType, reasonText, slot.getRecommendationCount());
    }
}
