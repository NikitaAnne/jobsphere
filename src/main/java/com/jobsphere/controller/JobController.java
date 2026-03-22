package com.jobsphere.controller;

import com.jobsphere.dto.job.JobRequest;
import com.jobsphere.dto.job.JobResponse;
import com.jobsphere.entity.Job;
import com.jobsphere.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Browse, create, and manage job postings")
public class JobController {

    private final JobService jobService;

    // ===== Public: Search Jobs (cached) =====

    @GetMapping
    @Operation(summary = "Search and filter job postings (paginated)")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Job.JobType jobType,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) Boolean remote,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
                jobService.searchJobs(keyword, location, skill, jobType, minSalary, remote, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job details by ID")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // ===== Recruiter: Manage Jobs =====

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Create a new job posting", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request, auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update a job posting (owner only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(jobService.updateJob(id, request, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    @Operation(summary = "Delete a job posting", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, Authentication auth) {
        jobService.deleteJob(id, auth);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Close a job posting", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<JobResponse> closeJob(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(jobService.closeJob(id, auth));
    }
}
