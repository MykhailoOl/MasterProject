package com.example.masterproject.service;

public class InvalidProfileUpdateException extends RuntimeException {

    public InvalidProfileUpdateException(String message) {
        super(message);
    }
}
