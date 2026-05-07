package com.qbook.auth;

import com.qbook.business.Business;
import com.qbook.business.BusinessRepository;
import com.qbook.business.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QBookUserDetailsService implements UserDetailsService {

    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Пробуем найти бизнес по email
        return businessRepository.findByEmail(username)
                .map(business -> User.builder()
                        .username(business.getEmail())
                        .password(business.getPasswordHash())
                        .authorities(List.of(
                            new SimpleGrantedAuthority("ROLE_" + business.getType().name().toUpperCase()),
                            new SimpleGrantedAuthority("ROLE_BUSINESS")
                        ))
                        .accountLocked(business.isBlocked())
                        .disabled(business.getDeletedAt() != null)
                        .build())
                .map(u -> (UserDetails) u)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Пользователь не найден: " + username));
    }
}
