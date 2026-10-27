package com.company.meetingroom.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the first administrator from environment variables when it does not exist yet. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final UserRepository users;
    private final String email;
    private final String password;

    public AdminBootstrap(AuthService authService, UserRepository users,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.authService = authService;
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        authService.create(email, password, "Administrator", Role.ADMIN);
        log.info("Bootstrap admin account created for {}", email);
    }
}
