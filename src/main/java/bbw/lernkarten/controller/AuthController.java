package bbw.lernkarten.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import ch.qos.logback.core.model.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        int a = (int)(Math.random()*10);
        int b = (int)(Math.random()*10);
        session.setAttribute("captchaExpected", String.valueOf(a + b));
        model.addAttribute("captchaQuestion", a + " + " + b);
        return "login";
    }
}
