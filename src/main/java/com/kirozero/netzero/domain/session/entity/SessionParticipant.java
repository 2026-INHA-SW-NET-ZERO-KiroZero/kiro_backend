package com.kirozero.netzero.domain.session.entity;

import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "session_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_participants_slot_user", columnNames = {"slot_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "can_purchase", nullable = false)
    private boolean canPurchase;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private SessionParticipant(Slot slot, User user, boolean canPurchase, LocalDateTime joinedAt) {
        this.slot = slot;
        this.user = user;
        this.canPurchase = canPurchase;
        this.joinedAt = joinedAt;
    }

    public static SessionParticipant create(Slot slot, User user, boolean canPurchase) {
        return new SessionParticipant(slot, user, canPurchase, LocalDateTime.now());
    }
}
