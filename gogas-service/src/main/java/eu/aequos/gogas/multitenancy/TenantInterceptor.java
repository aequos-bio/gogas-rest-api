package eu.aequos.gogas.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private TenantRegistry tenantRegistry;

    public TenantInterceptor(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = tenantRegistry.extractFromHostName(request);

        if (!tenantRegistry.isValidTenant(tenantId)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not a valid tenant: " + tenantId);
            log.error("Not a valid tenant: {}", tenantId);
            return false;
        }

        TenantContext.setTenantId(tenantId);

        MDC.put("logFileName", tenantId);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clearTenantId();
        MDC.remove("logFileName");
    }
}
