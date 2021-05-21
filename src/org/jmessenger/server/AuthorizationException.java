package org.jmessenger.server;

public class AuthorizationException extends Exception {
    public AuthorizationException(Exception e) {
        super(e);
    }
}
