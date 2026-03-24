package com.jobsphere.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsphere.dto.auth.LoginRequest;
import com.jobsphere.dto.auth.RegisterRequest;
import com.jobsphere.dto.job.JobRequest;
import com.jobsphere.entity.Job;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
  Integration Test — uses Testcontainers to spin up a real PostgreSQL instance.
  Tests the complete HTTP request → service → database → response cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("jobsphere_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable Kafka and Redis for integration tests
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                      "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                      "org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String recruiterToken;
    private static String candidateToken;
    private static Long jobId;

    @Test
    @Order(1)
    @DisplayName("Should register a recruiter")
    void registerRecruiter() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice Recruiter");
        request.setEmail("alice@acme.com");
        request.setPassword("Password1!");
        request.setRole("RECRUITER");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        recruiterToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should register a candidate")
    void registerCandidate() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Bob Candidate");
        request.setEmail("bob@gmail.com");
        request.setPassword("Password1!");
        request.setRole("CANDIDATE");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        candidateToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("Recruiter should create a job posting")
    void createJob() throws Exception {
        JobRequest request = new JobRequest();
        request.setTitle("Senior Java Developer");
        request.setDescription("We need a senior Spring Boot developer with 5+ years.");
        request.setCompany("Acme Corp");
        request.setLocation("Dubai, UAE");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setRequiredSkills("Java,Spring Boot,PostgreSQL,Kafka");

        MvcResult result = mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("Senior Java Developer")))
                .andReturn();

        jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test
    @Order(4)
    @DisplayName("Anyone should be able to search jobs")
    void searchJobs() throws Exception {
        mockMvc.perform(get("/api/jobs")
                .param("keyword", "Java")
                .param("location", "Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(5)
    @DisplayName("Candidate should apply to a job")
    void applyToJob() throws Exception {
        mockMvc.perform(post("/api/jobs/{jobId}/apply", jobId)
                .header("Authorization", "Bearer " + candidateToken)
                .param("coverLetter", "I am very excited about this opportunity!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("APPLIED")));
    }

    @Test
    @Order(6)
    @DisplayName("Recruiter should update application status to SHORTLISTED")
    void updateApplicationStatus() throws Exception {
        // First get the application ID
        MvcResult apps = mockMvc.perform(get("/api/jobs/{jobId}/applications", jobId)
                .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andReturn();

        Long appId = objectMapper.readTree(apps.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        // Update status
        mockMvc.perform(patch("/api/applications/{id}/status", appId)
                .header("Authorization", "Bearer " + recruiterToken)
                .param("status", "SHORTLISTED")
                .param("notes", "Strong Java background, proceed to interview."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHORTLISTED")));
    }

    @Test
    @Order(7)
    @DisplayName("Recruiter should not be able to apply to a job")
    void recruiterCannotApply() throws Exception {
        mockMvc.perform(post("/api/jobs/{jobId}/apply", jobId)
                .header("Authorization", "Bearer " + recruiterToken)
                .param("coverLetter", "I want to apply"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Login with wrong password should return 401")
    void loginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@acme.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
