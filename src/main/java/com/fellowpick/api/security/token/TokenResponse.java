package com.fellowpick.api.security.token;

public class TokenResponse {
    private String token;

    private Long expiration;

    public String getToken() {
        return token;
    }

    public Long getExpiresIn() {
        return expiration;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiration(Long expiresIn) {
        this.expiration = expiration;
    }
}
