package com.example;

// Spark imports
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.HttpResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import static spark.Spark.awaitInitialization; // Used for parsing generic JSON request
import static spark.Spark.before;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Azure OpenAI Configuration
    private static final String API_KEY = System.getenv("AZURE_API_KEY");;
    private static final String AZURE_OPENAI_ENDPOINT = "https://gaaya-mbgu79dm-eastus2.openai.azure.com/";
    private static final String AZURE_OPENAI_DEPLOYMENT_NAME = "gpt-4o";

    // This client will now be passed to ImageUtils
    private static OpenAIClient openaiClient;

    // Simple class for consistent error responses
    private static class ErrorResponse {
        String error;
        String details;
        public ErrorResponse(String error) { this.error = error; }
        public ErrorResponse(String error, String details) { this.error = error; this.details = details; }
    }

    public static void main(String[] args) {
        System.out.println("Main method started.");
        logger.info("Main method started.");

        try {
            // Initialize OpenAIClient once here and pass it to ImageUtils
            openaiClient = new OpenAIClientBuilder()
                    .endpoint(AZURE_OPENAI_ENDPOINT)
                    .credential(new AzureKeyCredential(API_KEY))
                    .buildClient();
            ImageUtils.setOpenAIClient(openaiClient); // Pass client to ImageUtils
            ImageUtils.setDeploymentName(AZURE_OPENAI_DEPLOYMENT_NAME); // Pass deployment name

            System.out.println("Attempting to set Spark port to 4567...");
            port(4567); // http://localhost:4567
            System.out.println("Spark port set successfully.");
            logger.info("Spark port set to 4567.");

            Gson gson = new Gson();

            // --- CORS CONFIGURATION (Keep as is, ensures single origin header) ---
            options("/*", (request, response) -> {
                String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }
                String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                }
                return "OK";
            });

            before((request, response) -> {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Credentials", "true");
                response.type("application/json");
            });
            // --- END CORS CONFIGURATION ---

            System.out.println("Defining /classify POST endpoint...");

            post("/classify", (req, res) -> {
                res.type("application/json");
                logger.info("Received request on /classify endpoint.");

                try {
                    // Parse the request body into a JsonObject to easily extract fields
                    JsonObject inputJson = JsonParser.parseString(req.body()).getAsJsonObject();

                    String base64Image = inputJson.has("image") && !inputJson.get("image").isJsonNull() ? inputJson.get("image").getAsString() : null;
                    String imageMimeType = inputJson.has("image_mime_type") && !inputJson.get("image_mime_type").isJsonNull() ? inputJson.get("image_mime_type").getAsString() : null;
                    String userTextPrompt = inputJson.has("text_prompt") && !inputJson.get("text_prompt").isJsonNull() ? inputJson.get("text_prompt").getAsString() : null;
                    String baggageType = inputJson.has("baggage_type") && !inputJson.get("baggage_type").isJsonNull() ? inputJson.get("baggage_type").getAsString() : null; // NEW
                    String routeType = inputJson.has("route_type") && !inputJson.get("route_type").isJsonNull() ? inputJson.get("route_type").getAsString() : null;     // NEW

                    // Basic validation for required fields
                    if (base64Image == null || base64Image.isEmpty() ||
                            imageMimeType == null || imageMimeType.isEmpty() ||
                            userTextPrompt == null || userTextPrompt.isEmpty() ||
                            baggageType == null || baggageType.isEmpty()) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Missing required fields: image, image_mime_type, text_prompt, or baggage_type."));
                    }

                    // Specific validation for checked baggage route
                    if ("checked".equalsIgnoreCase(baggageType) && (routeType == null || routeType.isEmpty())) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("For checked baggage, 'route_type' (americas_africa or other_routes) is required."));
                    }

                    // Step 1: Generate the AI prompt based on inputs
                    List<ChatRequestMessage> chatMessages = PromptGenerator.generatePromptMessages(
                            userTextPrompt, baggageType, routeType, imageMimeType, base64Image
                    );

                    // Step 2: Call the AI using ImageUtils
                    String aiResponse = ImageUtils.callAzureOpenAI(chatMessages);

                    return aiResponse; // ImageUtils will return cleaned JSON string or error JSON

                } catch (JsonParseException e) {
                    System.err.println("Error parsing request body JSON: " + e.getMessage());
                    logger.error("Error parsing request body JSON: {}", e.getMessage(), e);
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Invalid JSON in request body.", e.getMessage()));
                } catch (HttpResponseException e) {
                    System.err.println("Azure OpenAI service error: " + e.getMessage());
                    logger.error("Azure OpenAI service error: {}", e.getMessage(), e);
                    res.status(e.getResponse().getStatusCode());
                    return gson.toJson(new ErrorResponse("Azure OpenAI service error: " + e.getMessage(), String.valueOf(e.getResponse().getStatusCode())));
                } catch (Exception e) {
                    System.err.println("An unexpected error occurred during AI classification: " + e.getMessage());
                    logger.error("An unexpected error occurred during AI classification: {}", e.getMessage(), e);
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Internal server error during classification.", e.getMessage()));
                }
            });

            System.out.println("Waiting for Spark initialization...");
            logger.info("Waiting for Spark initialization...");
            awaitInitialization();

            System.out.println("Spark server started and ready on port 4567.");
            logger.info("Spark server started and ready on port 4567.");

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Spark server failed to start: " + e.getMessage());
            logger.error("CRITICAL ERROR: Spark server failed to start.", e);
            System.exit(1);
        }
    }
}