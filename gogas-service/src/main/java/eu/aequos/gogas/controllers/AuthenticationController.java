package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.security.JwtTokenHandler;
import io.swagger.annotations.Api;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Api("Authentication")
@RestController
public class AuthenticationController {

    private AuthenticationManager authenticationManager;
    private JwtTokenHandler jwtTokenHandler;
    private AuthorizationService userDetailsService;
    private ConfigurationRepo configurationRepo;
    private TenantRegistry tenantRegistry;

    public AuthenticationController(ConfigurationRepo configurationRepo, AuthenticationManager authenticationManager,
                                    JwtTokenHandler jwtTokenHandler, AuthorizationService userDetailsService,
                                    TenantRegistry tenantRegistry) {

        this.configurationRepo = configurationRepo;
        this.authenticationManager = authenticationManager;
        this.jwtTokenHandler = jwtTokenHandler;
        this.userDetailsService = userDetailsService;
        this.tenantRegistry = tenantRegistry;
    }

    @PostMapping(value = "authenticate")
    public BasicResponseDTO createAuthenticationToken(HttpServletRequest req, HttpServletResponse resp,
                                                      @RequestBody CredentialsDTO authenticationRequest) throws AuthenticationException {
        
        String tenantId = tenantRegistry.extractFromHostName(req);

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        GoGasUserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername())
                .withTenant(tenantId);

        String token = jwtTokenHandler.generateToken(userDetails);
        resp.setHeader("Authentication", "bearer " + token);
        Cookie ck = new Cookie("jwt-token", token);
        resp.addCookie(ck);

        return new BasicResponseDTO(token);
    }

    @PostMapping(value = "authenticate/legacy")
    public BasicResponseDTO createAuthenticationTokenFromLegacy(HttpServletRequest req, HttpServletResponse resp,
                                                                @RequestBody String legacyJwtToken) throws AuthenticationException {

        if (legacyJwtToken == null) {
            throw new BadCredentialsException("Missing token");
        }

        String tenantId = tenantRegistry.extractFromHostName(req);
        String userId = jwtTokenHandler.extractUserIdFromLegacyJWT(extractHost(req), legacyJwtToken);
        GoGasUserDetails userDetails = userDetailsService.loadUserById(userId)
                .withTenant(tenantId);

        String token = jwtTokenHandler.generateToken(userDetails);
        resp.setHeader("Authentication", "bearer " + token);
        Cookie ck = new Cookie("jwt-token", token);
        resp.addCookie(ck);

        return new BasicResponseDTO(token);
    }

    private String extractHost(HttpServletRequest req) {
        String address = req.getHeader("origin");
        if (address == null) {
            address = req.getServerName();
        }
        return address;
    }

    @GetMapping(value = "info")
    public @ResponseBody Map<String,Object> getInfo() {
        return configurationRepo.findByVisibleOrderByKey(true).stream()
                .collect(Collectors.toMap(Configuration::getKey, Configuration::getValue));
    }

    @GetMapping(value = "info/gas")
    public @ResponseBody Map<String,Object> getGasInfo() {
        return configurationRepo.findByKeyLike("gas%").stream()
                .collect(Collectors.toMap(Configuration::getKey, Configuration::getValue));
    }
}
