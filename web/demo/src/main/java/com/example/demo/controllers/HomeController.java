package com.example.demo.controllers;

import com.example.demo.services.AuthService;
import com.example.demo.services.MathService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@Controller
public class HomeController {

    // Injecting services
    private final AuthService authService;
    private final MathService mathService;

    // Injecting the duration from application.properties
    @Value("${app.cookie.max-age}")
    private Duration cookieMaxAge;

    // Constructor injection (Recommended over @Autowired)
    public HomeController(AuthService authService, MathService mathService) {
        this.authService = authService;
        this.mathService = mathService;
    }

    @GetMapping("/")
    public String home(@CookieValue(value = "username", required = false) String username, Model model) {
        model.addAttribute("loggedIn", username != null);
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "home";
    }

    @GetMapping("/login")
    public String loginPage(@CookieValue(value = "username", required = false) String username) {
        if (username != null) return "redirect:/";
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String username,
            @RequestParam String password,
            @CookieValue(value = "username", required = false) String currentCookie,
            HttpServletResponse response,
            Model model) {

        if (currentCookie != null) return "redirect:/";

        // Using the AuthService instead of hardcoding logic
        if (authService.authenticate(username, password)) {
            Cookie cookie = new Cookie("username", username);

            // Convert the Duration (3h) to seconds for the cookie
            cookie.setMaxAge((int) cookieMaxAge.getSeconds());
            cookie.setPath("/");
            response.addCookie(cookie);

            return "redirect:/";
        } else {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("username", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }

    @GetMapping("/math")
    public String mathPage(@CookieValue(value = "username", required = false) String username) {
        if (username == null) return "redirect:/login";
        return "math";
    }

    @PostMapping("/math")
    public String mathSubmit(
            @RequestParam(defaultValue = "0") int n,
            @CookieValue(value = "username", required = false) String username,
            Model model) {

        if (username == null) return "redirect:/login";

        if (n < 0) {
            model.addAttribute("error", "Please enter a positive number!");
            return "math";
        }

        model.addAttribute("inputN", n);
        // Using the MathService to handle the calculation
        model.addAttribute("primes", mathService.getPrimesUpTo(n));

        return "math";
    }
}