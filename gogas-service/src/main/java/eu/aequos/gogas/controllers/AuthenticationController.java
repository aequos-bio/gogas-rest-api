package eu.aequos.gogas.controllers;

import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import lombok.extern.slf4j.Slf4j;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.security.JwtTokenHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    public String createAuthenticationToken(HttpServletRequest req, HttpServletResponse resp, @RequestBody CredentialsDTO authenticationRequest) throws AuthenticationException, IOException {

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
        return token;
    }

    @GetMapping(value = "info")
    public @ResponseBody Map<String,Object> getInfo() {
        List<Configuration> configs = configurationRepo.findAll();
        Map<String,Object> map = new HashMap<>();
        for(Configuration cfg : configs) {
            if (cfg.isVisible()) {
                map.put(cfg.getKey(), cfg);
            }
        }
        return map;                  
    }      
}
