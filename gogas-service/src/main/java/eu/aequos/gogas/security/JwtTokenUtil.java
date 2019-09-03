package eu.aequos.gogas.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import eu.aequos.gogas.persistence.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -3301605591108950415L;

    private static final String ISSUER = "gogas";
    private static final String CLAIM_KEY_USERID = "id";
    private static final String CLAIM_KEY_ROLE = "role";
    private static final String CLAIM_KEY_ENABLED = "enabled";
    private static final String CLAIM_KEY_MANAGER = "manager";

    private JWTVerifier verifier;
    private Algorithm algorithm = Algorithm.HMAC256("cippirimerlo");

    public JwtTokenUtil() {
        verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    public String generateToken(GoGasUserDetails userDetails) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(new Date())
                .withSubject(userDetails.getUsername())
                .withClaim(CLAIM_KEY_USERID, userDetails.getId())
                .withClaim(CLAIM_KEY_ROLE, getRoleFromUserDetails(userDetails))
                .withClaim(CLAIM_KEY_ENABLED, userDetails.isEnabled())
                .withClaim(CLAIM_KEY_MANAGER, userDetails.isManager())
                .sign(algorithm);
    }

    public GoGasUserDetails getUserDetails(String token) {

        if (token == null)
            return null;

        DecodedJWT decodedToken = verifier.verify(token);

        return new GoGasUserDetails(decodedToken.getSubject())
                .withId(decodedToken.getClaim(CLAIM_KEY_USERID).asString())
                .withRole(decodedToken.getClaim(CLAIM_KEY_ROLE).asString())
                .withEnabled(decodedToken.getClaim(CLAIM_KEY_ENABLED).asBoolean())
                .withManager(decodedToken.getClaim(CLAIM_KEY_MANAGER).asBoolean());
    }

    private String getRoleFromUserDetails(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(User.Role.U.name());
    }
}