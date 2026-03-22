package com.jobsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsphere.dto.application.ApplicationResponse;
import com.jobsphere.entity.Application;
import com.jobsphere.entity.Job;
import com.jobsphere.entity.User;
import com.jobsphere.event.ApplicationStatusChangedEvent;
import com.jobsphere.exception.BadRequestException;
import com.jobsphere.exception.ForbiddenException;
import com.jobsphere.exception.ResourceNotFoundException;
import com.jobsphere.repository.ApplicationRepository;
import com.jobsphere.repository.JobRepository;
import com.jobsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, ApplicationStatusChangedEvent> kafkaTemplate;
    private final AiSkillMatchingService aiSkillMatchingService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads/resumes/}")
    private String uploadDir;

    @Value("${app.kafka.topics.application-status}")
    private String statusChangedTopic;

    // ===== Apply to Job =====

    @Transactional
    public ApplicationResponse applyToJob(Long jobId, String coverLetter, Authentication auth) {
        User candidate = getUserByEmail(auth.getName());
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (job.getStatus() != Job.JobStatus.ACTIVE) {
            throw new BadRequestException("This job is no longer accepting applications.");
        }
        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), jobId)) {
            throw new BadRequestException("You have already applied for this job.");
        }

        Application application = Application.builder()
                .candidate(candidate)
                .job(job)
                .coverLetter(coverLetter)
                .build();

        applicationRepository.save(application);
        log.info("Candidate [{}] applied to job [{}]", candidate.getEmail(), jobId);

        // Trigger async AI skill matching
        aiSkillMatchingService.matchSkills(
                job.getDescription(), job.getRequiredSkills(), candidate.getSkills()
        ).thenAccept(result -> {
            if (result != null) {
                try {
                    application.setAiMatchScore(result.getMatchScore());
                    application.setAiMatchDetails(objectMapper.writeValueAsString(result));
                    applicationRepository.save(application);
                    log.info("AI match score [{}] saved for application [{}]",
                            result.getMatchScore(), application.getId());
                } catch (Exception e) {
                    log.warn("Failed to save AI match result: {}", e.getMessage());
                }
            }
        });

        return mapToResponse(application);
    }

    // ===== Get candidate's applications =====

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(Authentication auth) {
        User candidate = getUserByEmail(auth.getName());
        return applicationRepository.findByCandidateId(candidate.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ===== Get applications for a job (recruiter) =====

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForJob(Long jobId, Authentication auth) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
        // Ensure only the recruiter who posted this job can see applications
        if (!job.getPostedBy().getEmail().equals(auth.getName())) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) throw new ForbiddenException("Access denied");
        }
        return applicationRepository.findByJobId(jobId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ===== Update application status (recruiter) =====

    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, Application.ApplicationStatus newStatus,
                                            String notes, Authentication auth) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Job job = application.getJob();
        if (!job.getPostedBy().getEmail().equals(auth.getName())) {
            throw new ForbiddenException("You can only update applications for your own job postings.");
        }

        Application.ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            application.setRecruiterNotes(notes);
        }

        applicationRepository.save(application);

        // Publish Kafka event for email notification
        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.builder()
                .applicationId(application.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .company(job.getCompany())
                .candidateEmail(application.getCandidate().getEmail())
                .candidateName(application.getCandidate().getFullName())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();

        kafkaTemplate.send(statusChangedTopic, String.valueOf(applicationId), event);
        log.info("Status changed: application [{}] {} → {}", applicationId, oldStatus, newStatus);

        return mapToResponse(application);
    }

    // ===== Upload Resume =====

    @Transactional
    public ApplicationResponse uploadResume(Long applicationId, MultipartFile file, Authentication auth) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (!application.getCandidate().getEmail().equals(auth.getName())) {
            throw new ForbiddenException("You can only upload resume for your own application.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            application.setResumeUrl("/uploads/resumes/" + fileName);
            applicationRepository.save(application);
            log.info("Resume uploaded for application [{}]: {}", applicationId, fileName);
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload resume: " + e.getMessage());
        }

        return mapToResponse(application);
    }

    // ===== Helpers =====

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .company(app.getJob().getCompany())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFullName())
                .candidateEmail(app.getCandidate().getEmail())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .resumeUrl(app.getResumeUrl())
                .recruiterNotes(app.getRecruiterNotes())
                .aiMatchScore(app.getAiMatchScore())
                .aiMatchDetails(app.getAiMatchDetails())
                .appliedAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
