package com.kirozero.netzero.domain.allergy.enums;

import java.util.Arrays;
import java.util.Optional;

public enum AllergyTag {
    CRUSTACEAN_SHELLFISH("crustacean_shellfish", "갑각류", "새우, 게, 랍스터 등"),
    MOLLUSK_SHELLFISH("mollusk_shellfish", "패류/연체류", "조개, 굴, 오징어 등"),
    FISH("fish", "생선", "참치, 어묵, 생선류 등"),
    EGG("egg", "계란", "계란, 마요네즈 등"),
    MILK("milk", "우유", "우유, 치즈, 요거트 등"),
    SOY("soy", "대두", "두부, 콩, 간장, 된장 등"),
    WHEAT("wheat", "밀", "식빵, 면, 밀가루 등"),
    PEANUT("peanut", "땅콩", "땅콩, 땅콩버터 등"),
    TREE_NUT("tree_nut", "견과류", "아몬드, 호두 등 견과류"),
    SESAME("sesame", "참깨", "참기름, 깨 등");

    private final String tag;
    private final String labelKo;
    private final String description;

    AllergyTag(String tag, String labelKo, String description) {
        this.tag = tag;
        this.labelKo = labelKo;
        this.description = description;
    }

    public String tag() {
        return tag;
    }

    public String labelKo() {
        return labelKo;
    }

    public String description() {
        return description;
    }

    public static Optional<AllergyTag> findByTag(String tag) {
        return Arrays.stream(values())
                .filter(allergyTag -> allergyTag.tag.equals(tag))
                .findFirst();
    }
}
