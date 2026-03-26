package com.example;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions; // For error response
import com.azure.ai.openai.models.ChatCompletionsOptions; // For error response
import com.azure.ai.openai.models.ChatRequestMessage; // For error response
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    private static final Gson gson = new Gson(); // For internal JSON handling

    // Static client and deployment name to be set by Main class
    private static OpenAIClient openaiClient;
    private static String deploymentName;

    // Setter for OpenAIClient
    public static void setOpenAIClient(OpenAIClient client) {
        ImageUtils.openaiClient = client;
    }

    // Setter for Deployment Name
    public static void setDeploymentName(String name) {
        ImageUtils.deploymentName = name;
    }

    /**
     * Calls the Azure OpenAI API with the generated chat messages.
     * Handles the API call, response parsing, and markdown stripping.
     *
     * @param chatMessages A list of ChatRequestMessage objects (system, user messages including image).
     * @return The cleaned JSON string response from the AI.
     * @throws Exception if there's an error in the API call or response processing.
     */
    public static String callAzureOpenAI(List<ChatRequestMessage> chatMessages) throws Exception {
        if (openaiClient == null || deploymentName == null || deploymentName.isEmpty()) {
            throw new IllegalStateException("OpenAIClient or deployment name not set in ImageUtils. Ensure setOpenAIClient() and setDeploymentName() are called from Main.");
        }

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages)
                .setTemperature(0.2); // Set temperature as desired

        System.out.println("Making call to Azure OpenAI...");
        logger.info("Making call to Azure OpenAI deployment: {}", deploymentName);

        ChatCompletions chatCompletions = openaiClient.getChatCompletions(deploymentName, chatCompletionsOptions);

        if (chatCompletions != null && !chatCompletions.getChoices().isEmpty()) {
            String aiContent = chatCompletions.getChoices().get(0).getMessage().getContent();
            System.out.println("Azure OpenAI SDK Raw Response Content: " + aiContent);
            logger.info("Azure OpenAI SDK Raw Response Content: {}", aiContent);

            // --- Strip Markdown code block wrappers ---
            if (aiContent.startsWith("```json")) {
                aiContent = aiContent.substring(aiContent.indexOf("```json") + 7); // Remove "```json\n"
            }
            if (aiContent.endsWith("```")) {
                aiContent = aiContent.substring(0, aiContent.lastIndexOf("```")); // Remove "\n```"
            }
            aiContent = aiContent.trim(); // Trim any remaining whitespace

            System.out.println("Cleaned AI Response Content: " + aiContent);
            logger.info("Cleaned AI Response Content: {}", aiContent);

            // Validate if the cleaned content is indeed valid JSON before returning
            try {
                JsonParser.parseString(aiContent).getAsJsonObject(); // Attempt to parse to validate (to object)
                return aiContent; // Return the cleaned, valid JSON string
            } catch (JsonParseException jsonEx) {
                System.err.println("Cleaned AI content is still not valid JSON: " + jsonEx.getMessage());
                logger.error("Cleaned AI content is still not valid JSON: {}", jsonEx.getMessage(), jsonEx);
                // Return a JSON error message for the frontend
                JsonObject errorJson = new JsonObject();
                errorJson.addProperty("error", "AI response was not valid JSON even after cleaning.");
                errorJson.addProperty("ai_raw_content", chatCompletions.getChoices().get(0).getMessage().getContent());
                errorJson.addProperty("ai_cleaned_content", aiContent);
                return gson.toJson(errorJson);
            }

        } else {
            System.err.println("Azure OpenAI returned an empty or invalid response.");
            logger.error("Azure OpenAI returned an empty or invalid response.");
            // Return a JSON error message for the frontend
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", "Azure OpenAI returned an empty or invalid response.");
            return gson.toJson(errorJson);
        }
    }

    // Your existing encodeImageToBase64 method can remain if used elsewhere,
    // but it's not used by the current frontend -> backend flow as frontend sends base64 directly.
    /*
    public static String encodeImageToBase64(String imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    */
}