# Author: By Joel Mukherjee(20)
# Description: Upgraded Random Forest model with edge-case training data.

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
import joblib

print("1. Generating expanded synthetic transaction data...")
np.random.seed(42)

# 1. Normal Retail (lower amounts, daytime) - Safe
amounts_normal = np.random.uniform(10, 5000, 800)
hours_normal = np.random.randint(6, 23, 800) 
labels_normal = np.zeros(800) 

# 2. Obvious Fraud (high amounts, late night) - Fraud
amounts_fraud = np.random.uniform(10000, 100000, 200)
hours_fraud = np.random.randint(0, 5, 200) 
labels_fraud = np.ones(200) 

# 3. NEW: Corporate Business Wires (high amounts, daytime) - Safe
amounts_biz = np.random.uniform(20000, 120000, 150)
hours_biz = np.random.randint(9, 17, 150) # Standard banking hours
labels_biz = np.zeros(150) 

# Combine all datasets into one DataFrame
X = pd.DataFrame({
    'amount': np.concatenate([amounts_normal, amounts_fraud, amounts_biz]),
    'hour': np.concatenate([hours_normal, hours_fraud, hours_biz])
})
y = np.concatenate([labels_normal, labels_fraud, labels_biz])

print("2. Training Upgraded AI Model (Random Forest)...")
model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X, y)

print("3. Saving the smarter AI 'Brain'...")
joblib.dump(model, 'fraud_model.pkl')
print("Success! Smarter model saved as 'fraud_model.pkl'")