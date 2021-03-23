package eu.aequos.gogas.mvc;

import eu.aequos.gogas.multitenancy.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class TenantAspect {

    @Around("@annotation(WithTenant)")
    public Object setTenant(ProceedingJoinPoint call) throws Throwable {
        MethodSignature signature = (MethodSignature) call.getSignature();
        WithTenant tenant = signature.getMethod().getAnnotation(WithTenant.class);

        try {
            TenantContext.setTenantId(tenant.value());
            return call.proceed();
        } finally {
            TenantContext.clearTenantId();
        }
    }
}
