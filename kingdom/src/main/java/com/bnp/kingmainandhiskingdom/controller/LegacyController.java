package com.bnp.kingmainandhiskingdom.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Exercise B6: a tiny HTML page with a login form and a form that changes something (POST). Both forms carry
 * the CSRF token in a hidden field; without it the POST is refused with a 403. Everything is escaped with
 * HtmlUtils, a page that echoes input without escaping is an XSS hole.
 */
@RestController
@RequestMapping(path = "/legacy", produces = MediaType.TEXT_HTML_VALUE)
public class LegacyController {

    private static final String NOTES = "notes";

    @GetMapping("/login")
    public String login(CsrfToken csrf, @RequestParam(required = false) String logout) {
        return page("Legacy login", (logout != null ? "<p>You are logged out.</p>" : "")
                + "<form method='post' action='/legacy/login'>"
                + hidden(csrf)
                + "<p><label>Username <input name='username'></label></p>"
                + "<p><label>Password <input name='password' type='password'></label></p>"
                + "<button type='submit'>Log in</button></form>");
    }

    @GetMapping("/hello")
    public String hello(Authentication authentication, CsrfToken csrf, HttpSession session) {
        StringBuilder notes = new StringBuilder("<ul>");
        for (String note : notes(session)) {
            notes.append("<li>").append(HtmlUtils.htmlEscape(note)).append("</li>");
        }
        notes.append("</ul>");
        return page("Legacy hello", "<p>Hello " + HtmlUtils.htmlEscape(authentication.getName())
                + ". Your session id starts with <code>" + HtmlUtils.htmlEscape(session.getId().substring(0, Math.min(6, session.getId().length()))) + "</code>.</p>"
                + notes
                + "<form method='post' action='/legacy/note'>" + hidden(csrf)
                + "<input name='text'> <button type='submit'>Add note</button></form>"
                + "<form method='post' action='/legacy/logout'>" + hidden(csrf) + "<button type='submit'>Log out</button></form>");
    }

    /** Changes state with a POST: this is what CSRF protection is for. */
    @PostMapping("/note")
    @ResponseStatus(HttpStatus.SEE_OTHER)
    public void addNote(@RequestParam String text, HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        notes(session).add(text);
        response.setHeader("Location", "/legacy/hello");
    }

    @SuppressWarnings("unchecked")
    private static List<String> notes(HttpSession session) {
        List<String> notes = (List<String>) session.getAttribute(NOTES);
        if (notes == null) {
            notes = new ArrayList<>();
            session.setAttribute(NOTES, notes);
        }
        return notes;
    }

    private static String hidden(CsrfToken csrf) {
        return "<input type='hidden' name='" + HtmlUtils.htmlEscape(csrf.getParameterName())
                + "' value='" + HtmlUtils.htmlEscape(csrf.getToken()) + "'>";
    }

    private static String page(String title, String body) {
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + title + "</title></head><body><h1>"
                + title + "</h1>" + body + "</body></html>";
    }
}
