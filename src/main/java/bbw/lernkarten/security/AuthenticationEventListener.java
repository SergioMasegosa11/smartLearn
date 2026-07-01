package bbw.lernkarten.security;

import org.springframework.context.event.EventListener;

@Component
@Slf4j
public class AuthenticationEventListener {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String user = event.getAuthentication().getName();
        loginAttemptService.loginSucceeded(user);
        log.info("Login success user={}", user);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String user = (String) event.getAuthentication().getPrincipal();
        loginAttemptService.loginFailed(user);
        log.warn("Login failed user={}", user);
    }
}
