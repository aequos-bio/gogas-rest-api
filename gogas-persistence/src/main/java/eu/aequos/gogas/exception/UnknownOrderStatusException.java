package eu.aequos.gogas.exception;

public class UnknownOrderStatusException extends Exception {

    public UnknownOrderStatusException() {
    }

    public UnknownOrderStatusException(String message) {
        super(message);
    }
}
