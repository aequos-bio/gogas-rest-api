package eu.aequos.gogas.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class RestApiError {

    private HttpStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp;

    private String message;
    private String debugMessage;

    private RestApiError() {
        timestamp = LocalDateTime.now();
    }

    RestApiError(HttpStatus status) {
        this();
        this.status = status;
    }

    RestApiError(HttpStatus status, Throwable ex) {
        this(status, "Unexpected error", ex);
    }

    RestApiError(HttpStatus status, String message, Throwable ex) {
        this(status);
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }
}
