package com.jobsphere.dto.job;

import com.jobsphere.entity.Job;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String company;
    private String location;
    private Boolean isRemote;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private String requiredSkills;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private LocalDate applicationDeadline;
    private Job.JobStatus status;
    private Long postedById;
    private String postedByName;
    private Long applicationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
