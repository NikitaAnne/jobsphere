package com.jobsphere.service;

import com.jobsphere.dto.job.JobRequest;
import com.jobsphere.dto.job.JobResponse;
import com.jobsphere.entity.Job;
import com.jobsphere.entity.User;
import com.jobsphere.exception.BadRequestException;
import com.jobsphere.exception.ForbiddenException;
import com.jobsphere.exception.ResourceNotFoundException;
import com.jobsphere.repository.ApplicationRepository;
import com.jobsphere.repository.JobRepository;
import com.jobsphere.repository.JobSpecification;
import com.jobsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    // ===== Create =====

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse createJob(JobRequest request, Authentication auth) {
        User recruiter = getUserByEmail(auth.getName());
        Job job = mapToEntity(request, recruiter);
        jobRepository.save(job);
        log.info("Job created [id={}] by recruiter [{}]", job.getId(), recruiter.getEmail());
        return mapToResponse(job);
    }

    // ===== Read =====

    @Cacheable(value = "jobs", key = "#keyword + #location + #skill + #jobType + #minSalary + #remote + #pageable.pageNumber")
    public Page<JobResponse> searchJobs(
            String keyword, String location, String skill,
            Job.JobType jobType, BigDecimal minSalary, Boolean remote, Pageable pageable) {

        Specification<Job> spec = Specification
                .where(JobSpecification.hasStatus(Job.JobStatus.ACTIVE))
                .and(JobSpecification.titleContains(keyword))
                .and(JobSpecification.locationContains(location))
                .and(JobSpecification.skillsContain(skill))
                .and(JobSpecification.hasJobType(jobType))
                .and(JobSpecification.salaryAtLeast(minSalary))
                .and(JobSpecification.isRemote(remote));

        return jobRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
        return mapToResponse(job);
    }

    // ===== Update =====

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse updateJob(Long id, JobRequest request, Authentication auth) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        assertOwnership(job, auth.getName());

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setIsRemote(request.getIsRemote());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryCurrency(request.getSalaryCurrency());
        job.setApplicationDeadline(request.getApplicationDeadline());

        return mapToResponse(jobRepository.save(job));
    }

    // ===== Delete =====

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public void deleteJob(Long id, Authentication auth) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            assertOwnership(job, auth.getName());
        }
        jobRepository.delete(job);
        log.info("Job deleted [id={}] by [{}]", id, auth.getName());
    }

    // ===== Close Job =====

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse closeJob(Long id, Authentication auth) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
        assertOwnership(job, auth.getName());
        job.setStatus(Job.JobStatus.CLOSED);
        return mapToResponse(jobRepository.save(job));
    }

    // ===== Helpers =====

    private void assertOwnership(Job job, String email) {
        if (!job.getPostedBy().getEmail().equals(email)) {
            throw new ForbiddenException("You are not the owner of this job posting");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Job mapToEntity(JobRequest request, User recruiter) {
        return Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .company(request.getCompany())
                .location(request.getLocation())
                .isRemote(request.getIsRemote())
                .jobType(request.getJobType())
                .experienceLevel(request.getExperienceLevel())
                .requiredSkills(request.getRequiredSkills())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .applicationDeadline(request.getApplicationDeadline())
                .postedBy(recruiter)
                .build();
    }

    public JobResponse mapToResponse(Job job) {
        Long appCount = applicationRepository.countByJobId(job.getId());
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .company(job.getCompany())
                .location(job.getLocation())
                .isRemote(job.getIsRemote())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .requiredSkills(job.getRequiredSkills())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .applicationDeadline(job.getApplicationDeadline())
                .status(job.getStatus())
                .postedById(job.getPostedBy().getId())
                .postedByName(job.getPostedBy().getFullName())
                .applicationCount(appCount)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
