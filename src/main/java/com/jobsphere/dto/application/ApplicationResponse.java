package com.jobsphere.dto.application;

import com.jobsphere.entity.Application;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private Application.ApplicationStatus status;
    private String coverLetter;
    private String resumeUrl;
    private String recruiterNotes;

    // AI Skill Matching
    private Integer aiMatchScore;
    private String aiMatchDetails;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
