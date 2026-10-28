package com.bnp.kingmainandhiskingdom.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exercise B2: the king manages the roles of the users. Protected twice: URL rule /api/admin/** and @PreAuthorize. */
@RestController
@RequestMapping(path = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminRestController {

    private final AccountService accountService;

    public AdminRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping(path = "/users/{username}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('KING')")
    public AuthController.UserInfo changeRoles(@PathVariable String username, @Valid @RequestBody RolesRequest request,
                                               Authentication caller) {
        AppUser user = accountService.changeRoles(caller.getName(), username, request.roles());
        return new AuthController.UserInfo(user.getUsername(), user.getRoles().stream().sorted().toList());
    }

    public record RolesRequest(@NotEmpty List<String> roles) {
    }
}
