package com.example;

import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatMessageImageContentItem;
import com.azure.ai.openai.models.ChatMessageImageUrl;
import com.azure.ai.openai.models.ChatMessageTextContentItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromptGenerator {

    /**
     * Generates the list of ChatRequestMessage objects (system and user messages)
     * for the Azure OpenAI Vision API call, incorporating baggage rules.
     *
     * @param userTextPrompt The user's specific question about the bag.
     * @param baggageType    The type of baggage selected by the user ('cabin' or 'checked').
     * @param routeType      The specific route type for checked baggage ('americas_africa' or 'other_routes'), can be null for cabin baggage.
     * @param imageMimeType  The MIME type of the image (e.g., "image/jpeg").
     * @param base64Image    The Base64 encoded image data.
     * @return A List of ChatRequestMessage objects ready for the OpenAI client.
     */
    public static List<ChatRequestMessage> generatePromptMessages(
            String userTextPrompt, String baggageType, String routeType, String imageMimeType, String base64Image) {

        String rulesString;
        String compatibilityFieldName;
        String typePromptText = baggageType.toUpperCase(); // For displaying in prompt

        // Define Qatar Airways baggage rules dynamically based on baggageType and routeType
        if ("cabin".equalsIgnoreCase(baggageType)) {
            rulesString = "- Max dimensions: 50 cm (L) x 37 cm (W) x 25 cm (H).\n" +
                    "- Max weight: 7 kg."; // Cabin baggage weight rule
            compatibilityFieldName = "is_cabin_baggage_compatible"; // Specific field for cabin
        } else if ("checked".equalsIgnoreCase(baggageType)) {
            String routeDetail = "";
            // Use routeType to select the correct dimension rule for checked baggage
            if ("americas_africa".equalsIgnoreCase(routeType)) {
                rulesString = "- Max dimensions (sum of L+W+H): 158 cm.";
                routeDetail = " (for flights to/from Africa or the Americas)";
            } else if ("other_routes".equalsIgnoreCase(routeType)) {
                rulesString = "- Max dimensions (sum of L+W+H): 300 cm.";
                routeDetail = " (for all other routes)";
            } else {
                // Fallback for unexpected routeType (though validated in Main)
                rulesString = "No specific checked baggage route rules provided.";
                routeDetail = " (route unspecified or invalid)";
            }
            rulesString += "\n- Max weight: 23 kg."; // Common checked baggage weight rule (adjust if specific per route)
            compatibilityFieldName = "is_checked_baggage_compatible"; // Specific field for checked
            typePromptText += routeDetail; // Append route detail for clarity in prompt
        } else {
            // Fallback for unexpected baggageType (though validated in Main)
            rulesString = "No specific baggage rules provided for this request.";
            compatibilityFieldName = "is_baggage_compatible"; // Generic field
            typePromptText = "UNSPECIFIED";
        }

        // Construct the full Data URL for the image
        String imageUrl = "data:" + imageMimeType + ";base64," + base64Image;

        // System message: Guides the AI on its role and expected JSON format
        String systemPromptContent = "You are an AI assistant for Qatar Airways. Your task is to analyze images of baggage " +
                "and assess their compatibility with Qatar Airways rules based on the user's selected baggage type and route.\n" +
                "Provide the response in structured JSON format only. DO NOT include any markdown code blocks, backticks, or extra text.\n" +
                "The JSON must include the following properties EXACTLY:\n" +
                "- 'Is it a baggage': boolean (true if it's a bag, false otherwise)\n" +
                "- 'type': string (e.g., 'suitcase', 'backpack', 'duffel bag')\n" +
                "- 'color': string\n" +
                "- 'material': string\n" +
                "- 'dimensions': string (approximate, e.g., '55cm L x 35cm W x 20cm H')\n" +
                "- '" + compatibilityFieldName + "': boolean (true if compatible, false if not)\n" + // Dynamic compatibility field
                "- 'compatibility_reasoning': string (concise explanation based on rules, especially if not compatible).\n\n" +
                "All dimensions should be in centimeters (cm).";


        // User message: Contains the specific baggage type, rules, and user's question, along with the image
        String userPromptText = "Please analyze this bag. It is being classified as **" + typePromptText + " BAGGAGE.**\n\n" +
                "**Qatar Airways Baggage Rules (" + typePromptText + " Baggage):**\n" +
                rulesString + "\n\n" +
                "User's specific question: '" + userTextPrompt + "'.\n" +
                "Based on the image and the rules, determine its compatibility and provide all details in the specified JSON format.";

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage(systemPromptContent));
        chatMessages.add(new ChatRequestUserMessage(Arrays.asList(
                new ChatMessageTextContentItem(userPromptText),
                new ChatMessageImageContentItem(new ChatMessageImageUrl(imageUrl))
        )));

        return chatMessages;
    }
}
