package bbw.lernkarten.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CaptchaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("/login".equals(request.getRequestURI()) && request.getMethod().equals("POST")) {
            String answer = request.getParameter("captchaAnswer");
            String expected = (String) request.getSession().getAttribute("captchaExpected");

            if (expected == null || !expected.equals(answer)) {
                response.sendRedirect("/login?captchaError");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
