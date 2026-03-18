package com.jobsphere.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity — ADMIN, RECRUITER, CANDIDATE
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    public enum RoleName {
        ROLE_ADMIN,
        ROLE_RECRUITER,
        ROLE_CANDIDATE
    }
}
