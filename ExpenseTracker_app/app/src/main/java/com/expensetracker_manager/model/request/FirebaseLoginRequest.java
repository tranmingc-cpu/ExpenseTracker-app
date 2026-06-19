package com.expensetracker_manager.model.request;

public class FirebaseLoginRequest {
    private String idToken;

    public FirebaseLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
