package com.ngo.util;

/** A problem whose message is safe and meaningful to show to the user (validation / business rule). */
public class AppException extends Exception {
    public AppException(String message) { super(message); }
}
