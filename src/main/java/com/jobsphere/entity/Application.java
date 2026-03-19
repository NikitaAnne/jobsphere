package com.jobsphere.entity;


import jakarta.persistence.*;
import lombok.*;

/**
 * Application entity — represents a candidate's application to a job posting.
 * Includes AI skill match data populated asynchronously on apply.
 */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_application_candidate_job",
        columnNames = {"candidate_id", "job_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "recruiter_notes", columnDefinition = "TEXT")
    private String recruiterNotes;

    // ===== AI Skill Matching Fields =====

    // AI-generated match score: 0–100
    @Column(name = "ai_match_score")
    private Integer aiMatchScore;

    // JSON string: {"matchedSkills": ["Java", "Spring"], "missingSkills": ["Kubernetes"]}
    @Column(name = "ai_match_details", columnDefinition = "TEXT")
    private String aiMatchDetails;

    public enum ApplicationStatus {
        APPLIED,
        REVIEWED,
        SHORTLISTED,
        REJECTED
    }
}
