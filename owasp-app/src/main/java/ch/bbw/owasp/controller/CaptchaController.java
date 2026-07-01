package ch.bbw.owasp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import java.security.SecureRandom;

@Controller
public class CaptchaController {
    private final SecureRandom rnd = new SecureRandom();

    @GetMapping("/captcha")
    public String captcha(HttpSession session, Model model) {
        int a = rnd.nextInt(10) + 1;
        int b = rnd.nextInt(10) + 1;
        String question = a + " + " + b + " = ?";
        session.setAttribute("CAPTCHA_ANSWER", Integer.toString(a + b));
        model.addAttribute("captchaQuestion", question);
        return "captcha"; // fragment used in login page
    }
}
