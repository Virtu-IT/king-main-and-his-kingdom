package com.bnp.kingmainandhiskingdom.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exercise A4: ownership checks with SpEL. */
@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** You may look up yourself; the king may look up everybody. #username is the path variable. */
    @GetMapping("/{username}")
    @PreAuthorize("#username.equalsIgnoreCase(authentication.name) or hasRole('KING')")
    public AuthController.UserInfo user(@PathVariable String username) {
        AppUser user = accountService.find(username);
        return new AuthController.UserInfo(user.getUsername(), user.getRoles().stream().sorted().toList());
    }

    /** Only you can change your own password, not even the king. */
    @PutMapping(path = "/{username}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("#username.equalsIgnoreCase(authentication.name)")
    public ResponseEntity<Void> changePassword(@PathVariable String username, @Valid @RequestBody PasswordChange change) {
        accountService.changePassword(username, change.oldPassword(), change.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record PasswordChange(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
}
