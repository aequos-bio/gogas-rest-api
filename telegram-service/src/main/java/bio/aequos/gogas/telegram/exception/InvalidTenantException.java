package bio.aequos.gogas.telegram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class InvalidTenantException extends RuntimeException {

    public InvalidTenantException(String tenantId) {
        super("Tenant id not valid: " + tenantId);
    }
}
