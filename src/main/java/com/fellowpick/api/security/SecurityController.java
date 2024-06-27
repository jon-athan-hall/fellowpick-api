package com.fellowpick.api.security;

import com.fellowpick.api.security.register.RegisterRequest;
import com.fellowpick.api.security.token.TokenRequest;
import com.fellowpick.api.security.token.TokenResponse;
import com.fellowpick.api.security.token.TokenService;
import com.fellowpick.api.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
@Controller
public class SecurityController {
    private final SecurityService securityService;

    private final TokenService tokenService;

    public SecurityController(
            SecurityService securityService,
            TokenService tokenService) {
        this.securityService = securityService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest registerRequest) {
        User user = securityService.register(registerRequest);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> createToken(@RequestBody TokenRequest tokenRequest) {
        User user = securityService.login(tokenRequest);
        String token = tokenService.generateToken(user);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setToken(token);
        tokenResponse.setExpiration(tokenService.getExpiration());

        return ResponseEntity.ok(tokenResponse);
    }
}
