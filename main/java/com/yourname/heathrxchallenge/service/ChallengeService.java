package com.kashish.heathrx_challenge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.kashish.heathrx_challenge.dto.SolutionRequest;
import com.kashish.heathrx_challenge.dto.WebhookRequest;
import com.kashish.heathrx_challenge.dto.WebhookResponse;


// @Component tells Spring: "This is a class that you should manage. Create an instance of it."
@Component
public class ChallengeService implements CommandLineRunner {

    // A logger is a standard way to print messages to the console for debugging.
    private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);

    // The @Value annotation injects a value from application.properties into this variable.
    @Value("${user.name}")
    private String name;
    @Value("${user.regNo}")
    private String regNo;
    @Value("${user.email}")
    private String email;
    @Value("${api.url.generate}")
    private String generateUrl;

    // This is where Dependency Injection happens.
    // Spring sees that this constructor needs a RestTemplate, and since we defined a @Bean for it, Spring provides it automatically.
    private final RestTemplate restTemplate;
    public ChallengeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * This is the main method that Spring Boot will run automatically on startup
     * because this class implements CommandLineRunner.
     */
    @Override
    public void run(String... args) {
        try {
            // STEP 1: Call the first API to get the webhook URL and token.
            logger.info("🚀 Step 1: Generating webhook by sending our details...");
            WebhookResponse webhookResponse = generateWebhook();
            logger.info("✅ Webhook received: {}", webhookResponse.getWebhook());
            logger.info("✅ Access Token received: (hidden for security)");

            // STEP 2: Figure out which SQL question to solve based on regNo.
            logger.info("🤔 Step 2: Solving the SQL problem based on RegNo: {}", regNo);
            String finalQuery = solveSqlProblem();
            logger.info("💡 SQL Query solved and ready to be submitted.");

            // STEP 3: Call the second API (the webhook URL) to submit our answer.
            logger.info("📤 Step 3: Submitting the final SQL query...");
            submitSolution(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), finalQuery);
            logger.info("🎉🎉🎉 Challenge completed successfully! 🎉🎉🎉");

        } catch (Exception e) {
            logger.error("❌ A critical error occurred: {}", e.getMessage(), e);
        }
    }

    /**
     * This private helper method handles the logic for the first API call.
     */
    private WebhookResponse generateWebhook() {
        // Create the Java object that will be converted to the JSON request body.
        WebhookRequest requestBody = new WebhookRequest(name, regNo, email);
        
        // Use RestTemplate to send the POST request.
        // It takes the URL, the request body object, and the class of the expected response.
        // Spring automatically converts our requestBody to JSON and the JSON response to a WebhookResponse object.
        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(generateUrl, requestBody, WebhookResponse.class);
        
        // It's crucial to check if the request was successful. A "2xx" status code means success.
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to generate webhook. Server responded with status: " + response.getStatusCode());
        }
        
        // Return the response body (our WebhookResponse object).
        return response.getBody();
    }

    /**
     * This private helper method contains the logic for solving the SQL puzzle.
     */
    private String solveSqlProblem() {
        // Use a regular expression to remove all non-digit characters from the string.
        // e.g., "REG12347" becomes "12347".
        String numericPart = regNo.replaceAll("\\D+", "");
        
        // Make sure we have at least two digits to work with.
        if (numericPart.length() < 2) {
            throw new IllegalArgumentException("Registration number must have at least two digits.");
        }
        
        // Get the substring of the last two characters and parse it into an integer.
        int lastTwoDigits = Integer.parseInt(numericPart.substring(numericPart.length() - 2));
        
        // The Modulo operator (%) gives the remainder of a division.
        // If a number divided by 2 has a remainder that is not 0, it's ODD.
        if (lastTwoDigits % 2 != 0) {
            logger.info("Last two digits ({}) are ODD. Using SQL Query 1.", lastTwoDigits);
            return "SELECT p.* FROM Patients p WHERE p.admission_date = (SELECT p2.admission_date FROM Patients p2 WHERE p2.name = 'John Smith');";
        } else {
            logger.info("Last two digits ({}) are EVEN. Using SQL Query 2.", lastTwoDigits);
            return "SELECT d.* FROM Doctors d LEFT JOIN Department_Assignments da ON d.doctor_id = da.doctor_id WHERE da.department_id IS NULL;";
        }
    }
    
    /**
     * This private helper method handles the logic for the second API call.
     */
    private void submitSolution(String webhookUrl, String accessToken, String finalQuery) {
        // For the second request, we need to add custom headers.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Tell the server we are sending JSON data.
        headers.setBearerAuth(accessToken); // This adds the "Authorization: Bearer <token>" header, required for JWT authentication.

        // Create the request body object.
        SolutionRequest solutionRequest = new SolutionRequest(finalQuery);

        // An HttpEntity is a special wrapper that combines the request body and the headers.
        HttpEntity<SolutionRequest> entity = new HttpEntity<>(solutionRequest, headers);

        // Send the POST request with our entity. We expect a simple String as a response.
        ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
        
        // Again, check for success.
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Failed to submit solution. Status: " + response.getStatusCode() + ", Body: " + response.getBody());
        }
        
        // Log the final success message from the server.
        logger.info("Server submission response: {}", response.getBody());
    }
}