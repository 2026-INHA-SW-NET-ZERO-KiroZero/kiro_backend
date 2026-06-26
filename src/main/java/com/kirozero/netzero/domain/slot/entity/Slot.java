package com.kirozero.netzero.domain.slot.entity;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Slot extends BaseTimeEntity {

    @Id
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "place_name", nullable = false, length = 120)
    private String placeName;

    @Column(name = "station_code", nullable = false, length = 10)
    private String stationCode;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SlotStatus status;

    @Column(name = "candidates_json", columnDefinition = "json")
    private String candidatesJson;

    @Column(name = "selected_menu_json", columnDefinition = "json")
    private String selectedMenuJson;

    @Column(name = "cooking_plan_json", columnDefinition = "json")
    private String cookingPlanJson;

    @Column(name = "recommendation_count", nullable = false)
    private int recommendationCount;

    public void proposeMenuCandidates(String candidatesJson) {
        this.candidatesJson = candidatesJson;
        this.status = SlotStatus.MENU_PROPOSED;
        this.recommendationCount += 1;
    }
}
