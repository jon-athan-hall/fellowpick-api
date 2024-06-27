package com.fellowpick.api.security;

import com.fellowpick.api.security.register.RegisterRequest;
import com.fellowpick.api.security.token.TokenRequest;
import com.fellowpick.api.security.token.TokenResponse;
import com.fellowpick.api.security.token.TokenService;
import com.fellowpick.api.user.User;
import com.fellowpick.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class SecurityService {
    private final AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;

    private UserRepository userRepository;

    public SecurityService(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public User register(RegisterRequest registerRequest) {
        User user = new User();

        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        return userRepository.save(user);
    }

    public User login(TokenRequest tokenRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        tokenRequest.getEmail(),
                        tokenRequest.getPassword()
                )
        );

        return userRepository.findByEmail(tokenRequest.getEmail()).orElseThrow();
    }
}
