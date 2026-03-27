document.addEventListener('DOMContentLoaded', () => {
    // Get references to DOM elements
    const askButton = document.getElementById('askButton');
    const imageInput = document.getElementById('imageInput');
    const imagePreview = document.getElementById('imagePreview'); // Element to display image preview
    const textPromptInput = document.getElementById('textPrompt');
    const outputDiv = document.getElementById('output');
    const baggageTypeRadios = document.querySelectorAll('input[name="baggageType"]');
    const routeTypeContainer = document.getElementById('routeTypeContainer');
    const routeTypeDropdown = document.getElementById('routeType');

    // --- Event Listener for the "Ask AI" Button ---
    if (askButton) {
        askButton.addEventListener('click', askAI);
    } else {
        console.error("Error: 'Ask AI' button not found! Check index.html for id='askButton'.");
    }

    // --- Event Listener for Image Input to Show Preview ---
    if (imageInput && imagePreview) {
        imageInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    imagePreview.src = e.target.result;
                    imagePreview.style.display = 'block'; // Show the image preview
                };
                reader.readAsDataURL(file);
            } else {
                imagePreview.src = '#'; // Clear image
                imagePreview.style.display = 'none'; // Hide if no file selected
            }
        });
    } else {
        console.error("Error: Image input or preview element not found! Check index.html for id='imageInput' and id='imagePreview'.");
    }

    // --- Event Listeners for Baggage Type Radios to Control Route Dropdown Visibility ---
    if (baggageTypeRadios.length > 0 && routeTypeContainer && routeTypeDropdown) {
        function toggleRouteTypeVisibility() {
            let selectedBaggageType = null;
            for (const radio of baggageTypeRadios) {
                if (radio.checked) {
                    selectedBaggageType = radio.value;
                    break;
                }
            }

            if (selectedBaggageType === 'checked') {
                routeTypeContainer.style.display = 'block';
                routeTypeDropdown.setAttribute('required', 'required'); // Make dropdown selection required
            } else {
                routeTypeContainer.style.display = 'none';
                routeTypeDropdown.removeAttribute('required'); // Remove required attribute
                routeTypeDropdown.value = ''; // Reset dropdown value when hidden
            }
        }

        baggageTypeRadios.forEach(radio => {
            radio.addEventListener('change', toggleRouteTypeVisibility);
        });

        // Set initial visibility on page load (important for default 'cabin' selection)
        toggleRouteTypeVisibility();
    } else {
        console.error("Error: Baggage type radios, route type container, or dropdown not found! Check index.html for correct elements.");
    }

    /**
     * Handles the AI query process: collects data, sends to backend, displays response.
     */
    async function askAI() {
        // Disable button and show loading message immediately
        askButton.disabled = true;
        outputDiv.innerHTML = '<p class="loading-message">Analyzing image and rules...</p>';
        outputDiv.classList.remove('error-message', 'success-message');

        const imageFile = imageInput.files[0];
        const textPrompt = textPromptInput.value.trim();

        let selectedBaggageType = null;
        for (const radio of baggageTypeRadios) {
            if (radio.checked) {
                selectedBaggageType = radio.value;
                break;
            }
        }

        // --- Client-side validation for required inputs ---
        if (!imageFile || !textPrompt) {
            outputDiv.innerHTML = '<p class="error-message">Please select an image and enter a text prompt.</p>';
            askButton.disabled = false; // Re-enable button on validation failure
            return;
        }
        if (!selectedBaggageType) {
            outputDiv.innerHTML = '<p class="error-message">Please select a baggage type (Cabin or Checked).</p>';
            askButton.disabled = false; // Re-enable button on validation failure
            return;
        }

        let selectedRouteType = null;
        if (selectedBaggageType === 'checked') {
            selectedRouteType = routeTypeDropdown.value;
            if (!selectedRouteType || selectedRouteType === '') {
                outputDiv.innerHTML = '<p class="error-message">For checked baggage, please select a route type.</p>';
                askButton.disabled = false; // Re-enable button on validation failure
                return;
            }
        }

        // --- Image conversion to Base64 ---
        const reader = new FileReader();
        reader.onload = async (e) => {
            const base64Image = e.target.result.split(',')[1]; // Get only the Base64 part
            const imageMimeType = imageFile.type;

            // Construct the request body for the backend
            const requestBody = {
                image: base64Image,
                image_mime_type: imageMimeType,
                text_prompt: textPrompt,
                baggage_type: selectedBaggageType
            };

            // Conditionally add route_type only if it's checked baggage and a route is selected
            if (selectedBaggageType === 'checked' && selectedRouteType) {
                requestBody.route_type = selectedRouteType;
            }

            try {
                // Send the request to your Java backend
                const response = await fetch('http://localhost:4567/classify', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(requestBody),
                });

                // Handle HTTP errors (e.g., 400 Bad Request, 500 Internal Server Error)
                if (!response.ok) {
                    const errorData = await response.json();
                    console.error('HTTP Error! Status:', response.status, '-', errorData);
                    outputDiv.innerHTML = `<p class="error-message">Error: HTTP status ${response.status} - ${errorData.error || 'Unknown error'}</p>`;
                    // Display more details if available from the backend error response
                    if (errorData.details) {
                        outputDiv.innerHTML += `<p class="error-message">Details: ${errorData.details}</p>`;
                    }
                    return; // Stop execution after handling error
                }

                // Parse and display the AI's JSON response
                const data = await response.json();
                console.log('AI Raw Response:', data); // Log the raw AI response for debugging
                displayAIResponse(data);
                outputDiv.classList.add('success-message'); // Add a class for success styling

            } catch (error) {
                // Catch network errors or other unexpected exceptions during fetch
                console.error('Error fetching AI response:', error);
                outputDiv.innerHTML = `<p class="error-message">Network Error: ${error.message}. Please ensure your Java backend is running.</p>`;
            } finally {
                // IMPORTANT: Re-enable the button regardless of success or failure
                askButton.disabled = false;
            }
        };

        // If an image file exists, start reading it. The rest of the logic is in reader.onload.
        if (imageFile) {
            reader.readAsDataURL(imageFile);
        } else {
            // This case should ideally be caught by the initial validation, but as a safeguard.
            askButton.disabled = false;
        }
    }

    /**
     * Displays the AI's JSON response in a readable format within the outputDiv.
     * @param {Object} data - The JSON object received from the AI.
     */
    function displayAIResponse(data) {
        outputDiv.innerHTML = ''; // Clear any previous content

        // Check if the AI returned an error message
        if (data.error) {
            outputDiv.innerHTML = `<p class="error-message">AI Response Error: ${data.error}</p>`;
            if (data.details) {
                outputDiv.innerHTML += `<p class="error-message">Details: ${data.details}</p>`;
            }
            if (data.ai_cleaned_content) { // Display if AI sent malformed JSON
                outputDiv.innerHTML += `<p class="error-message">AI tried to respond with (cleaned): <pre>${data.ai_cleaned_content}</pre></p>`;
            }
            if (data.ai_raw_content) {
                outputDiv.innerHTML += `<p class="error-message">AI raw content: <pre>${data.ai_raw_content}</pre></p>`;
            }
            return;
        }

        // Helper function to create formatted detail lines
        const createDetail = (label, value) => {
            // Convert boolean values to "Yes" or "No" for better readability
            if (typeof value === 'boolean') {
                value = value === true ? 'Yes' : 'No';
            }
            // Handle cases where values might be missing or empty
            if (value === undefined || value === null || value === '') {
                return `<strong>${label}:</strong> Not provided<br>`;
            }
            return `<strong>${label}:</strong> ${value}<br>`;
        };

        outputDiv.innerHTML += '<h3>AI Analysis Result:</h3>';
        outputDiv.innerHTML += createDetail('Is it a baggage', data['Is it a baggage']);
        outputDiv.innerHTML += createDetail('Type', data.type);
        outputDiv.innerHTML += createDetail('Color', data.color);
        outputDiv.innerHTML += createDetail('Material', data.material);
        outputDiv.innerHTML += createDetail('Dimensions', data.dimensions);

        // Dynamically check for the correct compatibility field based on what the AI provided
        if (data.hasOwnProperty('is_cabin_baggage_compatible')) {
            outputDiv.innerHTML += createDetail('Cabin Baggage Compatible', data.is_cabin_baggage_compatible);
            outputDiv.innerHTML += createDetail('Compatibility Reasoning', data.compatibility_reasoning);
        } else if (data.hasOwnProperty('is_checked_baggage_compatible')) {
            outputDiv.innerHTML += createDetail('Checked Baggage Compatible', data.is_checked_baggage_compatible);
            outputDiv.innerHTML += createDetail('Compatibility Reasoning', data.compatibility_reasoning);
        } else if (data.hasOwnProperty('is_baggage_compatible')) { // Fallback for a generic compatibility field
            outputDiv.innerHTML += createDetail('Baggage Compatible', data.is_baggage_compatible);
            outputDiv.innerHTML += createDetail('Compatibility Reasoning', data.compatibility_reasoning);
        }
    }
});