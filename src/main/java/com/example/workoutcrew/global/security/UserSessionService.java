package com.example.workoutcrew.global.security;

import com.example.workoutcrew.user.service.UserWithdrawalCommittedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class UserSessionService {

    private final SessionRegistry sessionRegistry;

    public UserSessionService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWithdrawalCommitted(UserWithdrawalCommittedEvent event) {
        expireAll(event.userId());
    }

    public void expireAll(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(CustomPrincipal.class::isInstance)
                .map(CustomPrincipal.class::cast)
                .filter(principal -> principal.userId().equals(userId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
