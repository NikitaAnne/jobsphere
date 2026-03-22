package com.jobsphere.event;

import com.jobsphere.entity.Application;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 Kafka event published when an application status changes.
 Consumed by ApplicationEventConsumer to send email notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusChangedEvent {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String candidateEmail;
    private String candidateName;
    private Application.ApplicationStatus oldStatus;
    private Application.ApplicationStatus newStatus;
}
