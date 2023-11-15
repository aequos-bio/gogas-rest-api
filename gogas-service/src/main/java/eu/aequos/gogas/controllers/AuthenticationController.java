package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.ConfigurationItemDTO;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.security.JwtTokenHandler;
import eu.aequos.gogas.service.ConfigurationService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Api("Authentication")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenHandler jwtTokenHandler;
    private final AuthorizationService userDetailsService;
    private final ConfigurationService configurationService;
    private final TenantRegistry tenantRegistry;
    private final BuildProperties buildProperties;

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
        return configurationService.getVisibleConfigurationItems().stream()
                .collect(Collectors.toMap(ConfigurationItemDTO::getKey, ConfigurationItemDTO::getValue));
    }

    @GetMapping(value = "info/gas")
    public @ResponseBody Map<String,Object> getGasInfo() {
        return configurationService.getGasProperties().stream()
                .collect(Collectors.toMap(ConfigurationItemDTO::getKey, ConfigurationItemDTO::getValue));
    }

    @GetMapping(value = "info/logo")
    public void getGasLogo(HttpServletResponse response) throws IOException, GoGasException {
        AttachmentDTO invoiceAttachment = configurationService.readLogo();
        invoiceAttachment.writeToHttpResponse(response);
    }

    @GetMapping(value = "info/build")
    public String getBuildTimestamp() {
        return buildProperties.getTime()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
