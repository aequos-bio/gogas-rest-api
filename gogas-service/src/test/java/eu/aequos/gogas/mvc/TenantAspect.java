package eu.aequos.gogas.mvc;

import eu.aequos.gogas.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@Order(0)
@Slf4j
public class TenantAspect {

    @Around("within(@eu.aequos.gogas.mvc.WithTenant *) && execution(public * *(..))")
    public Object setTenant(ProceedingJoinPoint call) throws Throwable {
        MethodSignature signature = (MethodSignature) call.getSignature();
        WithTenant tenant = extractTenantAnnotation(signature);

        try {
            TenantContext.setTenantId(tenant.value());
            return call.proceed();
        } finally {
            TenantContext.clearTenantId();
        }
    }

    private WithTenant extractTenantAnnotation(MethodSignature signature) {
        return Optional.ofNullable(signature.getMethod().getAnnotation(WithTenant.class))
                .orElse((WithTenant) signature.getDeclaringType().getAnnotation(WithTenant.class));
    }
}
