package com.guiyomi;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationService {
    // This is a mock method; you should implement actual logic to get user data
    public static User getUserByFullName(String fullName) {
        // Logic to retrieve user by their full name from your authentication provider
        // For example, you might have a list of users or an API call to get user info
        for (User user : getAllUsers()) {
            if ((user.getFirstName() + " " + user.getLastName()).equals(fullName)) {
                return user; // Return the user object if found
            }
        }
        return null; // Return null if no matching user is found
    }
    
private static List<User> getAllUsers() {

    List<User> users = new ArrayList<>();
    return users; 
}

}

