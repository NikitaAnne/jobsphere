package com.jobsphere.repository;

import com.jobsphere.entity.Job;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/* Spring Specifications for dynamic job filtering.*/

public class JobSpecification {

    private JobSpecification() {}

    public static Specification<Job> hasStatus(Job.JobStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> hasJobType(Job.JobType jobType) {
        return (root, query, cb) ->
                jobType == null ? null : cb.equal(root.get("jobType"), jobType);
    }

    public static Specification<Job> hasExperienceLevel(Job.ExperienceLevel level) {
        return (root, query, cb) ->
                level == null ? null : cb.equal(root.get("experienceLevel"), level);
    }

    public static Specification<Job> locationContains(String location) {
        return (root, query, cb) ->
                !StringUtils.hasText(location) ? null
                        : cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Job> titleContains(String keyword) {
        return (root, query, cb) ->
                !StringUtils.hasText(keyword) ? null
                        : cb.or(
                            cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                          );
    }

    public static Specification<Job> skillsContain(String skill) {
        return (root, query, cb) ->
                !StringUtils.hasText(skill) ? null
                        : cb.like(cb.lower(root.get("requiredSkills")), "%" + skill.toLowerCase() + "%");
    }

    public static Specification<Job> salaryAtLeast(BigDecimal minSalary) {
        return (root, query, cb) ->
                minSalary == null ? null : cb.greaterThanOrEqualTo(root.get("salaryMin"), minSalary);
    }

    public static Specification<Job> isRemote(Boolean remote) {
        return (root, query, cb) ->
                remote == null ? null : cb.equal(root.get("isRemote"), remote);
    }
}
