# AI Fraud Detection System

A real-time, event-driven microservice architecture designed to detect and explain fraudulent transactions. This system was developed as a comprehensive Vocational Training project, bridging a robust enterprise backend with machine learning analytics.

## 🚀 Overview

This project implements a full-stack, dual-language pipeline. It utilizes a Python-based AI microservice to evaluate transaction risk using Scikit-Learn (Random Forest) and custom heuristic checks (like Impossible Travel via the Haversine formula). The results are handled by a Java Spring Boot backend, which persists the data and broadcasts live AI confidence metrics and explainability reasoning to a dynamic dashboard via STOMP WebSockets.

## ✨ Key Features

*   **Real-Time Analytics Dashboard:** WebSocket integration allows the frontend to update instantaneously upon transaction processing without client-side polling.
*   **AI Explainability:** The machine learning model doesn't just block transactions; it generates human-readable reasoning for its decisions.
*   **Microservice Architecture:** Complete separation of concerns between the Java transactional backend and the Python machine learning service via REST APIs.
*   **Secure Environment:** Implements environment variables (`.dotenv`) for secure credential and API key management.

## 🛠️ Technology Stack

**Backend (Core System)**
*   Java Spring Boot
*   Spring Data JPA / Hibernate
*   H2 In-Memory SQL Database
*   Maven

**AI & Analytics Microservice**
*   Python 3
*   FastAPI
*   Scikit-Learn (Random Forest Classifier)
*   Google GenAI Integration

**Frontend & Messaging**
*   HTML5 / CSS3 / JavaScript
*   STOMP WebSockets (Real-time message broadcasting)

## ⚙️ System Architecture

1.  **Transaction Ingestion:** The Spring Boot backend receives transaction requests.
2.  **Delegation:** The backend securely passes the transaction payload via REST to the FastAPI Python service.
3.  **Evaluation:** The Python service scores the transaction using the trained Scikit-Learn model and formulates a response based on behavioral risk patterns.
4.  **Persistence:** The Java backend records the verdict into the H2 database, ensuring no duplicate logs.
5.  **Broadcasting:** The verdict is pushed to a live WebSocket channel (`/topic/transactions`), updating the frontend metrics dashboard instantly.

## 🏃‍♂️ Getting Started

### Prerequisites
*   Java 17 or higher
*   Python 3.10+
*   Maven

### Running the Python AI Service
1. Navigate to the `python-ai-service` directory.
2. Create a virtual environment and install dependencies:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows use `venv\Scripts\activate`
   pip install -r requirements.txt
3. Create a .env file in this directory and add your API credentials.
4. Start the FastAPI server:
    ```bash
    uvicorn app:app --reload
##Running the Java Spring Boot Backend 
1. Navigate to the java-backend directory.
2. Run the application using Maven:
   ```Bash
   mvn spring-boot:run
3. The server will start on port 8080.


~Developed by Joel Mukherjee 
