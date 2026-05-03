package io.clamped.cloud.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Implements all getters and setters
@Data
@Builder
@AllArgsConstructor // doesnt care about final or not

//  Java object that represents the currently authenticated user
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private Role role;
    private Collection<? extends GrantedAuthority> authorities;

    // create a UserPrincipal Object from a user object (could be imported using lombok, but we have multiple uses)
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // we could use lombok here but the main advantage is when we are building from other classes (no need for 'manual' creation)
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                authorities
        );
    }

    // todo add checks for these while authenticating and registering
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

}
