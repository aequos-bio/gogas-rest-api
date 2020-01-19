package eu.aequos.gogas.workflow;

import lombok.Getter;

@Getter
public class ActionValidity {
    private boolean valid;
    private String message;

    private ActionValidity(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ActionValidity valid() {
        return new ActionValidity(true, null);
    }

    public static ActionValidity notValid(String message) {
        return new ActionValidity(false, message);
    }
}
