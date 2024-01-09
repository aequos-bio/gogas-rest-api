package bio.aequos.gogas.telegram.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class TokenHandler {

    private static final int EXPIRATION_MILLISECONDS = 3600000;
    private static final Map<String, TokenInfo> TEMP_TOKEN_MAP = new HashMap<>();

    public String generateToken(String tenantId, String userId) {
        UUID token = UUID.randomUUID();
        TEMP_TOKEN_MAP.put(token.toString(), new TokenInfo(tenantId, userId));
        return token.toString();
    }

    public Optional<TokenInfo> getUserByToken(String token) throws Exception {
        TokenInfo tokenInfo = TEMP_TOKEN_MAP.remove(token);

        if (tokenInfo == null || new Date().getTime() > tokenInfo.creationTs + EXPIRATION_MILLISECONDS) {
            log.warn("Invalid or expired token");
            return Optional.empty();
        }

        return Optional.ofNullable(tokenInfo);
    }

    @Getter
    public static class TokenInfo {
        private final String tenantId;
        private final String userId;
        private final long creationTs;

        public TokenInfo(String tenantId, String userId) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.creationTs = new Date().getTime();
        }
    }
}
