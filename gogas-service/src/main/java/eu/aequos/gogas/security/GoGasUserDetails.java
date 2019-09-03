package eu.aequos.gogas.security;

import eu.aequos.gogas.persistence.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;

public class GoGasUserDetails implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private String id;
    private String username;
    private String password;
    private String role;
    private String tenant;
    private boolean enabled;
    private boolean isManager;

    public GoGasUserDetails(String username) {
        this.username = username;
    }

    public GoGasUserDetails(User user, boolean isManager) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.isManager = isManager;
    }

    public GoGasUserDetails withId(String id) {
        this.id = id;
        return this;
    }

    public GoGasUserDetails withRole(String role) {
        this.role = role;
        return this;
    }

    public GoGasUserDetails withTenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    public GoGasUserDetails withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public GoGasUserDetails withManager(boolean manager) {
        isManager = manager;
        return this;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority(role));
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getTenant() {
        return tenant;
    }

    public boolean isManager() {
        return isManager;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
