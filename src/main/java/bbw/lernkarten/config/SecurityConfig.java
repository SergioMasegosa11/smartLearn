package bbw.lernkarten.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import bbw.lernkarten.service.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @PostConstruct
    public void test() {
        log.info("SecurityConfig geladen");
    }

    
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService service,
            PasswordEncoder encoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(service);
        provider.setPasswordEncoder(encoder);

        return provider;
    }


    @Bean
    AuthenticationManager authenticationManager(
            DaoAuthenticationProvider provider) {

        return new ProviderManager(provider);
    }



/*
     * GEFIXT: HttpSessionCsrfTokenRepository speichert das CSRF-Token
     * NUR serverseitig in der Session, schreibt aber kein Cookie.
     * script.js (Frontend) liest das Token jedoch ueber
     * document.cookie.match(/XSRF-TOKEN/) aus einem Cookie aus.
     * -> Frontend fand nie ein Token, schickte es daher nicht mit,
     *    und jeder aendernde Request (POST/PUT/DELETE) wurde von
     *    Spring Security mit 403 (CSRF) abgelehnt.
     *
     * CookieCsrfTokenRepository.withHttpOnlyFalse() schreibt das Token
     * zusaetzlich in ein lesbares Cookie "XSRF-TOKEN", exakt wie es
     * script.js erwartet.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /*
     * GEFIXT: Ohne einen SecurityContextRepository-Bean (oder
     * requireExplicitSave(false) MIT tatsaechlich aktivem
     * Persistenz-Filter) wird die in UserService.login() gesetzte
     * Authentication nur fuer den aktuellen Request gehalten und ist
     * beim naechsten Request (z.B. POST /api/cards) bereits wieder weg
     * -> "current" und alle geschuetzten Endpunkte antworteten mit
     *    401/403, Karten konnten nie angelegt werden.
     *
     * HttpSessionSecurityContextRepository speichert/liest den
     * SecurityContext explizit in/aus der HttpSession.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository, SecurityContextRepository securityContextRepository)
        throws Exception {

    
    return http

    .cors(cors -> {})

    /*
     * GEFIXT (2): Spring Security 6 verwendet standardmaessig den
     * XorCsrfTokenRequestAttributeHandler (BREACH-Schutz). Dieser
     * erwartet, dass das im Header X-XSRF-TOKEN gesendete Token
     * XOR-maskiert ist. Das XSRF-TOKEN-Cookie (CookieCsrfTokenRepository)
     * enthaelt aber den ROHEN, unmaskierten Wert -- script.js liest genau
     * diesen rohen Cookie-Wert aus und schickt ihn 1:1 im Header mit.
     * Ergebnis: Server vergleicht maskiert vs. unmaskiert -> 403, obwohl
     * Login und Session laengst korrekt funktionieren.
     *
     * CsrfTokenRequestAttributeHandler (statt Xor-Variante) erwartet das
     * Token unmaskiert im Header -- passt damit zu dem, was script.js
     * tatsaechlich aus dem Cookie ausliest und sendet.
     */
    .csrf(csrf -> csrf
        .csrfTokenRepository(csrfTokenRepository)
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        .ignoringRequestMatchers(
            "/api/users/login",
            "/api/users/register"
        )
    )

    .formLogin(form -> form.disable())
    .logout(logout -> logout.disable())

    .securityContext(context -> context
        .securityContextRepository(securityContextRepository)
        .requireExplicitSave(false)
    )

    .authorizeHttpRequests(auth -> auth



            // Frontend
            .requestMatchers(
                "/",
                "/index.html",
                "/login.html",
                "/cards.html",
                "/test.html",
                "/style.css",
                "/script.js",
                "/favicon.ico"
            )
            .permitAll()


            // Auth
            .requestMatchers(
                "/api/users/login",
                "/api/users/register",
                "/api/users/captcha",
                "/api/users/csrf"
            )
            .permitAll()


            // Admin
            .requestMatchers("/api/admin/**")
            .hasRole("ADMIN")


            // Rest geschützt
            .anyRequest()
            .authenticated()

        )

        .build();
}



    @Bean
    CorsConfigurationSource corsConfigurationSource(){

        CorsConfiguration config =
                new CorsConfiguration();


        config.setAllowedOrigins(
            List.of("http://localhost:8080")
        );


        config.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE"
            )
        );


        config.setAllowedHeaders(
            List.of(
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-CSRF-TOKEN"
            )
        );


        config.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
            "/**",
            config
        );


        return source;
    }
}