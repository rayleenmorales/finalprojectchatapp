package com.guiyomi;

import java.net.URL;
import java.net.URI;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import org.json.JSONObject;

public class FirebaseService {

    private static final String API_KEY = "AIzaSyBNtPixNZI2wecaRO37a3l0hkJsjBSYMsQ";

    public void signUp(String email, String password) throws Exception {
        // Use URI and URL.of() to avoid using the deprecated constructor
        URI uri = URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY);
        URL url = uri.toURL();  // Converts the URI to a URL
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("email", email);
        jsonBody.put("password", password);
        jsonBody.put("returnSecureToken", true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Sign up successful.");
        } else {
            System.out.println("Sign up failed. Response Code: " + responseCode);
        }

        connection.disconnect();
 
    }

    // Method to handle user sign-in (login)
    public int signIn(String email, String password) throws Exception {
        URI uri = URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY);
        URL url = uri.toURL();  // Converts the URI to a URL
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("email", email);
        jsonBody.put("password", password);
        jsonBody.put("returnSecureToken", true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Login successful.");
        } else {
            System.out.println("Login failed. Response Code: " + responseCode);
        }

        connection.disconnect();

        return responseCode;
    }

    // Method to 
}
