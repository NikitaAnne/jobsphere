package com.jobsphere.controller;

import com.jobsphere.dto.application.ApplicationResponse;
import com.jobsphere.entity.Application;
import com.jobsphere.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Manage job applications")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    // Candidate: Apply to a job
    @PostMapping("/api/jobs/{jobId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to a job posting")
    public ResponseEntity<ApplicationResponse> applyToJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) String coverLetter,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.applyToJob(jobId, coverLetter, auth));
    }

    // Candidate: See own applications
    @GetMapping("/api/applications/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get my job applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        return ResponseEntity.ok(applicationService.getMyApplications(auth));
    }

    // Recruiter: See applications for a job
    @GetMapping("/api/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    @Operation(summary = "Get applications for a job (recruiter/admin)")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
            @PathVariable Long jobId, Authentication auth) {
        return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId, auth));
    }

    // Recruiter: Update application status
    @PatchMapping("/api/applications/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update application status (APPLIED→REVIEWED→SHORTLISTED/REJECTED)")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Application.ApplicationStatus status,
            @RequestParam(required = false) String notes,
            Authentication auth
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(id, status, notes, auth));
    }

    // Candidate: Upload resume
    @PostMapping(value = "/api/applications/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Upload resume PDF for an application")
    public ResponseEntity<ApplicationResponse> uploadResume(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        return ResponseEntity.ok(applicationService.uploadResume(id, file, auth));
    }
}
