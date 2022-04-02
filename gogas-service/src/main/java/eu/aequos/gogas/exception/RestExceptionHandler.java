package eu.aequos.gogas.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        String error = "Malformed JSON request";
        log.warn("Malformed JSON request - {}", request.getContextPath(), ex);
        return buildResponseEntity(new RestApiError(HttpStatus.BAD_REQUEST, error, ex));
    }

    @ExceptionHandler(MissingOrInvalidParameterException.class)
    protected ResponseEntity<Object> handleInvalidParameter(MissingOrInvalidParameterException ex) {
        log.warn("Missing or invalid parameter", ex);
        return buildResponseEntity(new RestApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex));
    }

    @ExceptionHandler(ItemNotFoundException.class)
    protected ResponseEntity<Object> handleItemNotFound(ItemNotFoundException ex) {
        log.warn("Item of type {} with id {} not found", ex.getItemType(), ex.getItemId());
        return buildResponseEntity(new RestApiError(HttpStatus.NOT_FOUND, ex.getMessage(), ex));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {} ", ex.getMessage());
        return buildResponseEntity(new RestApiError(HttpStatus.FORBIDDEN, "utente non autorizzato", ex));
    }

    @ExceptionHandler(JWTVerificationException.class)
    protected ResponseEntity<Object> handleJWTVerificationError(JWTVerificationException ex) {
        log.warn("JWT Token expired: {} ", ex.getMessage());
        return buildResponseEntity(new RestApiError(HttpStatus.UNAUTHORIZED, "Token non valido o scaduto", ex));
    }

    @ExceptionHandler(DuplicatedItemException.class)
    protected ResponseEntity<Object> handleDuplicatedItem(DuplicatedItemException ex) {
        return buildResponseEntity(new RestApiError(HttpStatus.CONFLICT, "L'elemento non può essere creato perché già esistente", ex));
    }

    @ExceptionHandler(OrderAlreadyExistsException.class)
    protected ResponseEntity<Object> handleDuplicatedItem(OrderAlreadyExistsException ex) {
        return buildResponseEntity(new RestApiError(HttpStatus.CONFLICT, "Esiste già un ordine nello stesso periodo", ex));
    }

    @ExceptionHandler(ItemNotDeletableException.class)
    protected ResponseEntity<Object> handleItemNotDeletable(ItemNotDeletableException ex) {
        return buildResponseEntity(new RestApiError(HttpStatus.CONFLICT, "L'elemento non può essere eliminato", ex));
    }

    @ExceptionHandler(GoGasException.class)
    protected ResponseEntity<Object> handleGoGasException(GoGasException ex) {
        log.warn("An exception occurred while processing the request", ex);
        return buildResponseEntity(new RestApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleItemNotDeletable(ConstraintViolationException ex) {
        return buildResponseEntity(new RestApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex));
    }

    private ResponseEntity<Object> buildResponseEntity(RestApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
