package ch.bbw.owasp.controller;

import ch.bbw.owasp.model.Card;
import ch.bbw.owasp.repository.CardRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class CardController {
    private final CardRepository repo;
    private final Logger logger = LoggerFactory.getLogger(CardController.class);

    public CardController(CardRepository repo) { this.repo = repo; }

    @GetMapping({"/","/cards"})
    public String list(Model model) {
        model.addAttribute("cards", repo.findAll());
        return "list";
    }

    @GetMapping("/cards/new")
    public String createForm(Model model) {
        model.addAttribute("card", new Card());
        return "form";
    }

    @PostMapping("/cards")
    public String create(@Valid Card card, BindingResult br) {
        if (br.hasErrors()) return "form";
        repo.save(card);
        logger.info("Card created: {}", card.getTitle());
        return "redirect:/cards";
    }

    @GetMapping("/cards/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("card", repo.findById(id).orElse(new Card()));
        return "form";
    }

    @PostMapping("/cards/delete/{id}")
    public String delete(@PathVariable Long id) {
        repo.deleteById(id);
        logger.warn("Card deleted: {}", id);
        return "redirect:/cards";
    }
}
