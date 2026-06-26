package com.kirozero.netzero.domain.user.entity;

import com.kirozero.netzero.domain.user.enums.CookingSkill;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_skill", nullable = false, length = 20)
    private CookingSkill cookingSkill;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAllergy> allergies = new ArrayList<>();

    private User(String email, String passwordHash, String nickname, CookingSkill cookingSkill) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.cookingSkill = cookingSkill;
    }

    public static User create(String email, String passwordHash, String nickname, CookingSkill cookingSkill) {
        return new User(email, passwordHash, nickname, cookingSkill);
    }

    public void replaceAllergies(List<String> allergyTags) {
        allergies.clear();
        allergyTags.stream()
                .distinct()
                .map(tag -> UserAllergy.create(this, tag))
                .forEach(allergies::add);
    }
}
