#  AI-Powered Baggage Identification & Classification System

An intelligent full-stack web application that analyzes baggage images using Generative AI and determines:

- Baggage type (suitcase, backpack, duffle, etc.)
- Color
- Material
- Estimated size
- Cabin vs Checked compatibility
- Airline specification suitability

Built using Java (Spark Framework) and Azure OpenAI GPT-4o, this system combines RESTful backend engineering with multimodal AI image interpretation.

---

##  Problem Statement

Baggage allowance rules vary across airlines, routes, and cabin classes.  
Passengers frequently encounter:

- Excess baggage fees  
- Last-minute repacking  
- Boarding delays  
- Confusion about compliance  

This project addresses the problem by building an AI-powered baggage classification system that:

1. Accepts a baggage image  
2. Uses Generative AI to analyze attributes  
3. Extracts structured characteristics  
4. Determines compliance with airline specifications  
5. Returns a readable, structured JSON response  

---

## 🏗 System Architecture

            ┌────────────────────────────┐
            │         Frontend           │
            │  HTML | CSS | JavaScript   │
            │  - Image Upload            │
            │  - Prompt Input            │
            │  - Baggage Type Selection  │
            └──────────────┬─────────────┘
                           │
                           │ HTTP POST (/classify)
                           ▼
            ┌────────────────────────────┐
            │        Backend (Java)      │
            │     Spark Framework        │
            │  - JSON Parsing (Gson)     │
            │  - Input Validation        │
            │  - Prompt Construction     │
            │  - Azure API Integration   │
            └──────────────┬─────────────┘
                           │
                           │ Base64 Image + Prompt
                           ▼
            ┌────────────────────────────┐
            │       Azure OpenAI         │
            │         GPT-4o Model       │
            │  - Image Understanding     │
            │  - Attribute Extraction    │
            │  - Text Generation         │
            └──────────────┬─────────────┘
                           ▼
            ┌────────────────────────────┐
            │   Structured JSON Output   │
            │   Returned to Frontend     |
            └────────────────────────────┘


---

## 🖥 Frontend

**Technologies**
- HTML
- CSS
- JavaScript

**Responsibilities**
- Image upload handling
- Prompt collection
- Cabin / Checked selection
- Sending POST request to backend
- Rendering structured AI response in table format

**Core Files**
- baggageaifrontend/index.html  
- baggageaifrontend/style.css  
- baggageaifrontend/script.js  

---

##  Backend

**Technologies**
- Java
- Spark Framework (Lightweight HTTP Server)
- Gson (JSON Parsing)
- REST API Design

**Endpoint**

POST /classify


### Backend Workflow

1. Initialize Spark server on port 4567  
2. Enable CORS for cross-origin requests  
3. Receive POST request from frontend  
4. Parse JSON payload  
5. Validate required fields  
6. Encode image into Base64  
7. Construct structured AI prompt  
8. Send request to Azure OpenAI GPT-4o  
9. Parse AI response  
10. Return structured JSON response  

**Key Classes**
- Main.java  
- ImageUtils.java  
- PromptGenerator.java  

---

##  AI Integration

**Service:** Azure OpenAI  
**Model:** GPT-4o (Multimodal Vision Model)

The model processes:
- Base64 encoded image
- User textual prompt
- Baggage type and route context

It generates:
- Identified baggage category  
- Extracted attributes  
- Compliance evaluation  
- Structured descriptive output  

---

## 📂 Project Structure


baggage-identification-classifier/
│
├── baggageaifrontend/      # Frontend (HTML, CSS, JavaScript)
├── src/main/java/          # Backend (Java + Spark Framework)
├── pom.xml                 # Maven configuration & dependencies
├── .gitignore
└── README.md


---

## 🔍 Technical Highlights

- Multimodal AI (Image + Text) Processing  
- Base64 Image Transmission  
- Prompt Engineering Strategy  
- RESTful Backend Architecture  
- Structured JSON Parsing  
- CORS Configuration  
- Secure API Key Handling  
- Modular Java Design  

---

## 🧠 Engineering Challenges Solved

- Cross-origin browser restrictions  
- Structured parsing of generative AI responses  
- Reliable prompt formatting for consistent output  
- Cloud API authentication  
- Clean separation of frontend and backend layers  

---

## ▶️ How to Run Locally

1. Clone the repository  
2. Open project in IntelliJ IDEA  
3. Configure Azure OpenAI API key as environment variable  
4. Run backend (Spark server starts on port 4567)  
5. Open index.html in browser  
6. Upload baggage image and test classification  

---

## 📈 Future Improvements

- Cloud deployment (Azure / Render)  
- Airline policy database integration  
- Automatic dimension estimation  
- Enhanced UI/UX  
- Logging and monitoring integration  
- Authentication system  

---



##  Project Significance

This project demonstrates:

- Full-stack web development capability  
- Cloud-based Generative AI integration  
- REST API design knowledge  
- Prompt engineering understanding  
- Structured response parsing  
- Real-world aviation domain application  
