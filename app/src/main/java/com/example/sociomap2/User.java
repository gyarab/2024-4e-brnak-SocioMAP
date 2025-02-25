package com.example.sociomap2;

public class User {
    private String userId, name, surname, username;

    public User(String userId, String name, String surname, String username) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return name + " " + surname + " (@" + username + ")";
    }

    public String getUsername() {
        return username;
    }

    public boolean matchesSearch(String query) {
        query = query.toLowerCase();
        return (name != null && name.toLowerCase().contains(query)) ||
                (surname != null && surname.toLowerCase().contains(query)) ||
                (username != null && username.toLowerCase().contains(query));
    }
}