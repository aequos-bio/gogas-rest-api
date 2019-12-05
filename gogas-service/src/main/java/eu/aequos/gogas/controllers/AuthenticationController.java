package eu.aequos.gogas.controllers;

import eu.aequos.gogas.datasource.CustomRoutingDataSource;
import eu.aequos.gogas.persistence.entity.Configuration;
import eu.aequos.gogas.persistence.repository.ConfigurationRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.security.JwtAuthenticationRequest;
import eu.aequos.gogas.security.JwtTokenUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthenticationController {

    private AuthenticationManager authenticationManager;
    private JwtTokenUtil jwtTokenUtil;
    private AuthorizationService userDetailsService;
    private ConfigurationRepo configurationRepo;

    public AuthenticationController(ConfigurationRepo configurationRepo, AuthenticationManager authenticationManager, 
            JwtTokenUtil jwtTokenUtil, AuthorizationService userDetailsService) {
                    this.configurationRepo = configurationRepo;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping(value = "api/authenticate")
    public String createAuthenticationToken(HttpServletRequest req, HttpServletResponse resp, @RequestBody JwtAuthenticationRequest authenticationRequest) throws AuthenticationException, IOException {
        
        String tenantId = CustomRoutingDataSource.extractTenantIdFromHostName(req);

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        GoGasUserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername())
                .withTenant(tenantId);

        String token = jwtTokenUtil.generateToken(userDetails);
        resp.setHeader("Authentication", "bearer " + token);
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
