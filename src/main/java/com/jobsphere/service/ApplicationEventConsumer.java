package com.jobsphere.service;

import com.jobsphere.event.ApplicationStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/*
 Kafka Consumer — listens for application status change events
 and sends email notifications to candidates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventConsumer {

    private final JavaMailSender mailSender;

    @KafkaListener(
        topics = "${app.kafka.topics.application-status}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("Kafka event received: application [{}] status → {}", 
                event.getApplicationId(), event.getNewStatus());
        try {
            sendEmail(event);
        } catch (Exception e) {
            log.error("Failed to send email notification for application [{}]: {}",
                    event.getApplicationId(), e.getMessage());
        }
    }

    private void sendEmail(ApplicationStatusChangedEvent event) {
        String subject = buildSubject(event);
        String body = buildBody(event);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getCandidateEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Email sent to {} for application status change", event.getCandidateEmail());
    }

    private String buildSubject(ApplicationStatusChangedEvent event) {
        return String.format("[JobSphere] Your application for '%s' at %s has been %s",
                event.getJobTitle(), event.getCompany(), event.getNewStatus());
    }

    private String buildBody(ApplicationStatusChangedEvent event) {
        return String.format("""
            Hi %s,
            
            We have an update on your application for the position of '%s' at %s.
            
            Status Update: %s → %s
            
            %s
            
            Log in to JobSphere to view more details.
            
            Best of luck!
            The JobSphere Team
            """,
            event.getCandidateName(),
            event.getJobTitle(),
            event.getCompany(),
            event.getOldStatus(),
            event.getNewStatus(),
            getStatusMessage(event.getNewStatus())
        );
    }

    private String getStatusMessage(com.jobsphere.entity.Application.ApplicationStatus status) {
        return switch (status) {
            case REVIEWED    -> "Your application is being reviewed by the recruiter.";
            case SHORTLISTED -> "Congratulations! You have been shortlisted. Expect a call soon.";
            case REJECTED    -> "We're sorry, your application was not selected at this time. Keep applying!";
            default          -> "";
        };
    }
}
