package dev.sleepy_evelyn.packwizml.exceptions;

public class FailedHashMatchException extends Exception {
    public FailedHashMatchException() {
        super("Failed to verify the Packwiz Bootstrap hashes match. Please manually download the " +
                "bootstrapper from: https://github.com/packwiz/packwiz-installer-bootstrap/releases " +
                "and place in the main directory for your server.");
    }
}
