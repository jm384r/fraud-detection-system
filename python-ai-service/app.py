# Author: By Joel Mukherjee(20)
# Description: FastAPI Microservice with integrated Scikit-Learn AI.

from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
import random
from fastapi.middleware.cors import CORSMiddleware
# Author: By Joel Mukherjee(20)
from math import radians, sin, cos, sqrt, atan2

# Geographic coordinate mapping for velocity calculations
CITY_COORDS = {
    "Raipur": (21.2514, 81.6296),
    "Boria Kal": (21.1904, 81.6534),
    "Bhilai": (21.1938, 81.3509),
    "Bilaspur": (22.0797, 82.1409),
    "Mumbai": (19.0760, 72.8777),
    "Delhi": (28.7041, 77.1025)
}

def calculate_distance(city1, city2):
    """Calculates the Haversine distance in kilometers between two cities."""
    if city1 not in CITY_COORDS or city2 not in CITY_COORDS:
        return 0.0 # Return 0 if location is unrecognized
        
    lat1, lon1 = CITY_COORDS[city1]
    lat2, lon2 = CITY_COORDS[city2]
    
    R = 6371.0 # Radius of the Earth in kilometers
    
    dlon = radians(lon2 - lon1)
    dlat = radians(lat2 - lat1)
    
    a = sin(dlat / 2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return R * c

app = FastAPI()

# Add this CORS block to allow the frontend to talk to Python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Allows any origin (like your port 8080 dashboard)
    allow_credentials=True,
    allow_methods=["*"], # Allows POST, GET, OPTIONS, etc.
    allow_headers=["*"],
)

# Load the trained AI model into memory when the server starts
model = joblib.load('fraud_model.pkl')

class Transaction(BaseModel):
    account_id: str
    amount: float
    location: str
    time_of_day: str
    home_location: str = "Raipur" 
    historical_avg: float = 0.0 # NEW: Expect the moving average from Java

# Helper function to convert "03:00 AM" into an integer (3) for the AI
def extract_hour(time_str: str) -> int:
    try:
        time_parts = time_str.split(' ')
        hour = int(time_parts[0].split(':')[0])
        if time_parts[1].upper() == 'PM' and hour != 12:
            hour += 12
        elif time_parts[1].upper() == 'AM' and hour == 12:
            hour = 0
        return hour
    except:
        return 12 # Default to noon if parsing fails

@app.post("/api/v1/evaluate-fraud")
def evaluate_transaction(txn: Transaction):
    # 1. Format the data exactly how the AI expects it
    hour = extract_hour(txn.time_of_day)
    features = pd.DataFrame([{'amount': txn.amount, 'hour': hour}])
    
    # 2. Ask the AI for a prediction and force it into a standard Python float!
    base_fraud_score = float(model.predict_proba(features)[0][1])
    
    # NEW: 3. Impossible Travel Calculation
    distance_km = calculate_distance(txn.home_location, txn.location)
    
    # NEW: 4. Dynamic Risk Adjustment
    anomaly_penalty = 0.0

    # Catch the New Account Loophole: Absolute limit check
    if txn.amount > 100000:
        anomaly_penalty += 0.80  # Instant block for massive amounts

    # Impossible Travel
    if distance_km > 500:
        anomaly_penalty += 0.80  # High enough to instantly trigger the 0.75 threshold
    elif distance_km > 100:
        anomaly_penalty += 0.30

    # Time-Series Profiling (Spending Spike)
    if txn.historical_avg > 0 and txn.amount > (txn.historical_avg * 3):
        anomaly_penalty += 0.40
        
    final_risk_score = min(base_fraud_score + anomaly_penalty, 1.0)
    
    # 5. Determine action based on the AI's adjusted confidence
    action = "BLOCK" if final_risk_score >= 0.75 else "ALLOW"
    
        # Build dynamic explainability reason
    reasons = []
    if txn.amount > 100000:
        reasons.append("Massive amount threshold exceeded")
    if distance_km > 500:
        reasons.append(f"Impossible travel distance ({round(distance_km)}km from home)")
    elif distance_km > 100:
        reasons.append(f"Notable location shift ({round(distance_km)}km away)")
    if txn.historical_avg > 0 and txn.amount > (txn.historical_avg * 3):
        reasons.append("Significant spending spike over historical baseline")
        
    if not reasons:
            if action == "BLOCK":
                reason_str = "Flagged by ML model based on behavioral risk patterns."
            else:
                reason_str = "Transaction is within normal behavioral bounds."
    else:
            reason_str = " | ".join(reasons) if action == "BLOCK" else "Approved with minor flags: " + " | ".join(reasons)

    return {
            "transaction_id": f"TX_ML_{random.randint(1000, 9999)}",
            "fraud_score": round(float(final_risk_score), 2),
            "distance_flag_km": round(distance_km, 1),
            "is_fraud": bool(final_risk_score > 0.75),
            "action": action,
            "reason": reason_str  # <--- NEW: Passes the explanation to Java!
    }
from google import genai

from google import genai
import os
from dotenv import load_dotenv

# Load the variables from the hidden .env file
load_dotenv()

# Initialize the client using the hidden key
client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

# Match the frontend JSON ('prompt' instead of 'message')
class ChatRequest(BaseModel):
    message: str

@app.post("/api/v1/chat")
def ai_assistant(request: ChatRequest):
    # This secret context makes the AI aware of your specific project
    system_context = """
    You are an AI assistant embedded in a secure Fraud Detection System built by Joel Mukherjee(20).
    The system architecture uses a Java Spring Boot backend, an H2 SQL database, and a Python FastAPI microservice.
    The fraud model uses Scikit-Learn (Random Forest) and checks for Impossible Travel (Haversine formula) and Time-Series spending spikes.
    Answer the user's questions about this system professionally and concisely.
    """
    
    try:
        # Combine the context with the user's actual question
        full_prompt = f"{system_context}\n\nUser Question: {request.message}"
        
        # Generate the response using the NEW SDK syntax
        response = client.models.generate_content(
            model='gemini-3.5-flash',
            contents=full_prompt,
        )
        return {"reply": response.text}
        
    except Exception as e:
        return {"reply": f"Connection to the LLM neural network is currently offline. Error: {str(e)}"}