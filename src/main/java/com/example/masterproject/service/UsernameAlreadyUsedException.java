package com.example.masterproject.service;

public class UsernameAlreadyUsedException extends RuntimeException {

    public UsernameAlreadyUsedException(String username) {
        super("Username already registered: " + username);
    }
}
