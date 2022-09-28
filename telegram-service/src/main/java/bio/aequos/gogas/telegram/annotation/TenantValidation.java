package bio.aequos.gogas.telegram.annotation;

import bio.aequos.gogas.telegram.exception.InvalidTenantException;
import bio.aequos.gogas.telegram.persistence.repository.TenantRepo;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantValidation {

    private final TenantRepo tenantRepo;

    @Before("execution(* *(.., @ValidTenant (*), ..))")
    public void validate(JoinPoint pointcut) {
        MethodSignature signature = (MethodSignature) pointcut.getSignature();
        Method method = signature.getMethod();
        Parameter[] params = method.getParameters();
        Object[] args = pointcut.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (params[i].getAnnotation(ValidTenant.class) != null) {
                String tenantId = (String) args[i];
                if (tenantId == null || !tenantRepo.existsById(tenantId)) {
                    throw new InvalidTenantException(tenantId);
                }
            }
        }
    }
}
