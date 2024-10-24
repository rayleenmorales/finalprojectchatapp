package com.guiyomi;


public class User {
    private String uid;
    private String firstName;
    private String lastName;

    // Constructor
    public User(String uid, String firstName, String lastName) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}

