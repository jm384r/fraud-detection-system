# Description: FastAPI Microservice with integrated Scikit-Learn AI & Explainability
from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
import random
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv
from math import radians, sin, cos, sqrt, atan2

# Load environment variables
load_dotenv()

# Initialize Gemini Client for Explainable AI
from google import genai
client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

# Geographic coordinate mapping for velocity/distance calculations
CITY_COORDS = {
    "Raipur": (21.2514, 81.6296),
    "Boria Kal": (21.1904, 81.6534),
    "Bhilai": (21.1938, 81.3509),
    "Bilaspur": (22.0797, 82.1409),
    "Mumbai": (19.0760, 72.8777),
    "Delhi": (28.7041, 77.1025)
}

def calculate_distance(city1: str, city2: str) -> float:
    """Calculates the Haversine distance in kilometers between two cities."""
    if city1 not in CITY_COORDS or city2 not in CITY_COORDS:
        return 0.0  # Return 0 if location is unrecognized
    
    lat1, lon1 = CITY_COORDS[city1]
    lat2, lon2 = CITY_COORDS[city2]
    R = 6371.0  # Earth radius in KM

    dlon = radians(lon2 - lon1)
    dlat = radians(lat2 - lat1)

    a = sin(dlat / 2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c

app = FastAPI()

# CORS configuration for frontend dashboards
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load the trained AI model
model = joblib.load("fraud_model.pkl")

class Transaction(BaseModel):
    account_id: str
    amount: float
    location: str
    time_of_day: str
    home_location: str = "Raipur"
    historical_avg: float = 0.0
    velocity_count: int = 1 
    channel: str = "UPI_QR_OFFLINE"
    merchant_location: str = "Same as User"

def extract_hour(time_str: str) -> int:
    try:
        time_parts = time_str.split(" ")
        hour = int(time_parts[0].split(":")[0])
        if len(time_parts) > 1:
            if time_parts[1].upper() == "PM" and hour != 12:
                hour += 12
            elif time_parts[1].upper() == "AM" and hour == 12:
                hour = 0
        return hour
    except Exception:
        return 12  # Default to noon if parsing fails

@app.post("/api/v1/evaluate-fraud")
def evaluate_transaction(txn: Transaction):
    # 1. Feature Extraction for Scikit-Learn Model
    hour = extract_hour(txn.time_of_day)
    features = pd.DataFrame([{'amount': txn.amount, 'hour': hour}])
    
    # 2. Base AI Prediction Probability
    base_fraud_score = float(model.predict_proba(features)[0][1])

    # 3. Spatial Heuristics (Haversine Distance)
    distance_km = calculate_distance(txn.home_location, txn.location)

    # 4. Dynamic Risk Adjustment
    anomaly_penalty = 0.0

    # Rule A: Large Amount Threshold
    if txn.amount > 100000:
        anomaly_penalty += 0.80

    # Rule B: Impossible Travel (Contextual UPI Heuristics)
    is_remote_channel = txn.channel.upper() in ["P2P_ONLINE", "ECOMMERCE"]

    if txn.channel.upper() == "UPI_OFFLINE":
        if distance_km > 500:
            anomaly_penalty += 0.50
        elif distance_km > 100:
            anomaly_penalty += 0.30

    # Rule C: Time-series Spending Spike
    if txn.historical_avg > 0 and txn.amount > (txn.historical_avg * 3):
        anomaly_penalty += 0.40

    # Rule D: Velocity Spike (Multiple swipes within 60s)
    if txn.velocity_count > 3:
        anomaly_penalty += 0.45

    final_risk_score = min(base_fraud_score + anomaly_penalty, 1.0)

    # 5. Decision & Explainability Reason Generation
    action = "BLOCK" if final_risk_score >= 0.50 else "ALLOW"

    reasons = []
    if txn.amount > 100000:
        reasons.append("Massive amount threshold exceeded")
    if txn.channel.upper() == "UPI_OFFLINE":
        if distance_km > 500:
            reasons.append(f"Impossible travel distance ({round(distance_km)} km from home via Physical QR)")
        elif distance_km > 100:
            reasons.append(f"Notable location shift ({round(distance_km)} km away)")
    else:
        reasons.append(f"Remote transfer ({txn.channel}): Distance check relaxed")
    if txn.historical_avg > 0 and txn.amount > (txn.historical_avg * 3):
        reasons.append("Significant spending spike over historical baseline")
    if txn.velocity_count > 3:
        reasons.append(f"High transaction frequency ({txn.velocity_count} swipes in 60s)")

    if not reasons:
        reason_str = "Flagged by ML model on behavioral risk patterns." if action == "BLOCK" else "Transaction is within normal behavioral bounds."
    else:
        reason_str = "; ".join(reasons) if action == "BLOCK" else "Approved with minor flags: " + "; ".join(reasons)

    return {
        "transaction_id": f"TX_ML_{random.randint(1000, 9999)}",
        "fraud_score": round(float(final_risk_score), 2),
        "distance_flag_km": round(distance_km, 1),
        "is_fraud": bool(final_risk_score >= 0.75),
        "action": action,
        "reason": reason_str,
        "channel": txn.channel
    }

# LLM Chat Endpoint for System Explainability
class ChatRequest(BaseModel):
    message: str

@app.post("/api/v1/chat")
def assistant(request: ChatRequest):
    system_context = """
    You are an AI assistant embedded in a secure Fraud Detection System.
    The system architecture uses a Java Spring Boot backend, a MySQL database, and a Python FastAPI microservice.
    The fraud model uses Scikit-Learn (Random Forest) and checks for Impossible Travel (Haversine formula), Velocity Spikes, and Time-Series spending spikes.
    Answer the user's questions about this system professionally and concisely.
    """
    full_prompt = f"{system_context}\n\nUser Question: {request.message}"

    try:
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=full_prompt,
        )
        return {"reply": response.text}
    except Exception as e:
        return {"reply": f"Connection to the LLM neural network is currently offline. Error: {str(e)}"}