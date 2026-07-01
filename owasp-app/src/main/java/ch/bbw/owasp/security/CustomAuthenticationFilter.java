package ch.bbw.owasp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.ServletException;
import java.io.IOException;

public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager, LoginAttemptService service) {
        super(authenticationManager);
        this.loginAttemptService = service;
    }

    @Override
    public org.springframework.security.core.Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        String captcha = request.getParameter("captcha");
        String expected = (String) request.getSession().getAttribute("CAPTCHA_ANSWER");

        if (username == null) username = "";
        if (password == null) password = "";

        if (loginAttemptService.isBlocked(username)) {
            throw new BadCredentialsException("Account locked due to too many failed attempts");
        }

        if (expected == null || !expected.equals(captcha)) {
            loginAttemptService.loginFailed(username);
            throw new BadCredentialsException("Invalid captcha");
        }

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
