package eu.aequos.gogas.multitenancy;

import java.util.Optional;

public class TenantContext {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        CONTEXT.set(tenantId);
    }

    public static Optional<String> getTenantId() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clearTenantId() {
        CONTEXT.remove();
    }
}
