package com.github.dghng36.eauction.modules.identity.helper;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieHelper {
    @Value("${app.cookie.name}")
    protected String cookieName;

    @Value("${app.cookie.http-only:true}")
    protected boolean cookieHttpOnly;

    @Value("${app.cookie.secure:true}")
    protected boolean cookieSecure;

    @Value("${app.cookie.max-age:604800}")
    protected long cookieMaxAge;

    @Value("${app.cookie.same-site:Strict}")
    protected String cookieSameSite;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieName, refreshToken)
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .path("/api/v1/")
            .maxAge(cookieMaxAge)
            .sameSite(cookieSameSite)
            .build();
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(cookieName, "")
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .path("/api/v1/")
            .maxAge(0)
            .sameSite(cookieSameSite)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }
}
