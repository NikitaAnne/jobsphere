package com.jobsphere.dto.job;

import com.jobsphere.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating or updating a job posting.
 */
@Data
public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotBlank(message = "Company name is required")
    private String company;

    @NotBlank(message = "Location is required")
    private String location;

    private Boolean isRemote = false;

    @NotNull(message = "Job type is required")
    private Job.JobType jobType;

    private Job.ExperienceLevel experienceLevel;

    // Comma-separated: "Java,Spring Boot,PostgreSQL"
    private String requiredSkills;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency = "USD";
    private LocalDate applicationDeadline;
}
