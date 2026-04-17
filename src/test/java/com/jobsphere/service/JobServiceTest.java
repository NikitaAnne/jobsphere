package com.jobsphere.service;

import com.jobsphere.dto.job.JobRequest;
import com.jobsphere.entity.Job;
import com.jobsphere.entity.Role;
import com.jobsphere.entity.User;
import com.jobsphere.exception.ForbiddenException;
import com.jobsphere.exception.ResourceNotFoundException;
import com.jobsphere.repository.ApplicationRepository;
import com.jobsphere.repository.JobRepository;
import com.jobsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Unit Tests")
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private Authentication auth;

    @InjectMocks private JobService jobService;

    private User recruiter;
    private Job job;

    @BeforeEach
    void setUp() {
        recruiter = User.builder()
                .id(1L).email("recruiter@acme.com").fullName("Jane Smith")
                .roles(Set.of(Role.builder().name(Role.RoleName.ROLE_RECRUITER).build()))
                .build();

        job = Job.builder()
                .id(10L).title("Java Developer").company("Acme Corp")
                .location("Dubai").jobType(Job.JobType.FULL_TIME)
                .description("Java Spring Boot role")
                .postedBy(recruiter).status(Job.JobStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should create a job successfully")
    void createJob_shouldSucceed() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setDescription("Java Spring Boot role");
        request.setCompany("Acme Corp");
        request.setLocation("Dubai");
        request.setJobType(Job.JobType.FULL_TIME);

        when(auth.getName()).thenReturn(recruiter.getEmail());
        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j = Job.builder().id(10L).title(j.getTitle()).description(j.getDescription())
                    .company(j.getCompany()).location(j.getLocation())
                    .jobType(j.getJobType()).postedBy(recruiter).status(Job.JobStatus.ACTIVE).build();
            return j;
        });
        when(applicationRepository.countByJobId(any())).thenReturn(0L);

        var response = jobService.createJob(request, auth);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Java Developer");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when job does not exist")
    void getJobById_shouldThrow_whenNotFound() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-owner tries to update")
    void updateJob_shouldThrow_whenNotOwner() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(auth.getName()).thenReturn("other@someone.com"); // NOT the owner

        assertThatThrownBy(() -> jobService.updateJob(10L, new JobRequest(), auth))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not the owner");
    }

    @Test
    @DisplayName("Should close a job when owner requests it")
    void closeJob_shouldSetStatusClosed() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(auth.getName()).thenReturn(recruiter.getEmail());
        when(jobRepository.save(any())).thenReturn(job);
        when(applicationRepository.countByJobId(anyLong())).thenReturn(5L);

        var response = jobService.closeJob(10L, auth);

        assertThat(job.getStatus()).isEqualTo(Job.JobStatus.CLOSED);
    }
}
