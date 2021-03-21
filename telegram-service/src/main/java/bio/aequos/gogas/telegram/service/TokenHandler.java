package bio.aequos.gogas.telegram.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TokenHandler {

    private static final int EXPIRATION_MILLISECONDS = 3600000;
    private static final Map<String, TokenInfo> TEMP_TOKEN_MAP = new HashMap<>();

    public String generateToken(String tenantId, String userId) {
        UUID token = UUID.randomUUID();
        TEMP_TOKEN_MAP.put(token.toString(), new TokenInfo(tenantId, userId));
        return token.toString();
    }

    public TokenInfo getUserByToken(String token) throws Exception {
        TokenInfo tokenInfo = TEMP_TOKEN_MAP.remove(token);

        if (tokenInfo == null || new Date().getTime() > tokenInfo.creationTs + EXPIRATION_MILLISECONDS) {
            throw new Exception("Invalid or expired token");
        }

        return tokenInfo;
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
