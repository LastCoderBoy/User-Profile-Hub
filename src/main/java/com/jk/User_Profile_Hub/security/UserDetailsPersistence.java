package com.jk.User_Profile_Hub.security;

import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.entity.UserPrincipal;
import com.jk.User_Profile_Hub.redis.RedisService;
import com.jk.User_Profile_Hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsPersistence implements UserDetailsService {

    private final UserRepository userRepository;
    private final RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserPrincipal cached = redisService.getCachedUserPrincipal(email);
        if (cached != null) {
            return cached; // no DB hit
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        UserPrincipal principal = UserPrincipal.create(user);

        // Store in cache
        redisService.cacheUserPrincipal(email, principal);

        return principal;
    }
}
