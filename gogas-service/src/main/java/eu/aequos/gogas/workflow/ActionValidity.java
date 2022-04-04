package eu.aequos.gogas.workflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ActionValidity {
    private final boolean valid;
    private final String message;

    public static ActionValidity valid() {
        return new ActionValidity(true, null);
    }

    public static ActionValidity notValid(String message) {
        return new ActionValidity(false, message);
    }
}
