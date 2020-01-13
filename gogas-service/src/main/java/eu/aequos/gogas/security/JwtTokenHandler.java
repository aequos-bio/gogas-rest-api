package eu.aequos.gogas.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import eu.aequos.gogas.persistence.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Component
public class JwtTokenHandler implements Serializable {

    private static final long serialVersionUID = -3301605591108950415L;

    private static final String ISSUER = "gogas";
    private static final String CLAIM_KEY_TENANT = "tenant";
    private static final String CLAIM_KEY_USERID = "id";
    private static final String CLAIM_KEY_USERFIRSTNAME = "firstname";
    private static final String CLAIM_KEY_USERLASTNAME = "lastname";
    private static final String CLAIM_KEY_ROLE = "role";
    private static final String CLAIM_KEY_ENABLED = "enabled";
    private static final String CLAIM_KEY_MANAGER = "manager";

    private JWTVerifier verifier;
    private Algorithm algorithm = Algorithm.HMAC256("4eQu05%&/G0g!45sS£=)2020Nw£éd+f*W°5@SWd^^||£LKJ%$ddknnSMNadf+,-:");

    @Value("${jwt.duration.minutes:60}")
    private int tokenValidityInMinutes;

    public JwtTokenHandler() {
        verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    public String generateToken(GoGasUserDetails userDetails) {
        Date now = new Date();

        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(new Date())
                .withExpiresAt(generateExpiringDate(now))
                .withSubject(userDetails.getUsername())
                .withClaim(CLAIM_KEY_TENANT, userDetails.getTenant())
                .withClaim(CLAIM_KEY_USERID, userDetails.getId())
                .withClaim(CLAIM_KEY_USERFIRSTNAME, userDetails.getFirstname())
                .withClaim(CLAIM_KEY_USERLASTNAME, userDetails.getLastname())
                .withClaim(CLAIM_KEY_ROLE, getRoleFromUserDetails(userDetails))
                .withClaim(CLAIM_KEY_ENABLED, userDetails.isEnabled())
                .withClaim(CLAIM_KEY_MANAGER, userDetails.isManager())
                .sign(algorithm);
    }

    private Date generateExpiringDate(Date now) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, tokenValidityInMinutes);
        return calendar.getTime();
    }

    public GoGasUserDetails getUserDetails(String token) {

        if (token == null)
            return null;

        DecodedJWT decodedToken = verifier.verify(token);

        return new GoGasUserDetails(decodedToken.getSubject())
                .withTenant(decodedToken.getClaim(CLAIM_KEY_TENANT).asString())
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