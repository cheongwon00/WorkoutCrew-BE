package com.example.workoutcrew.global.config;

import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public LockRetryExecutor lockRetryExecutor(PlatformTransactionManager transactionManager) {
        return new LockRetryExecutor(new TransactionTemplate(transactionManager), 3, Duration.ofMillis(25));
    }

    public static final class LockRetryExecutor {
        private final TransactionTemplate transactionTemplate;
        private final int maxAttempts;
        private final Duration backoff;

        LockRetryExecutor(TransactionTemplate transactionTemplate, int maxAttempts, Duration backoff) {
            this.transactionTemplate = transactionTemplate;
            this.maxAttempts = maxAttempts;
            this.backoff = backoff;
        }

        public void execute(Runnable work) {
            execute(() -> {
                work.run();
                return null;
            });
        }

        public <T> T execute(Supplier<T> work) {
            int attempt = 0;
            while (true) {
                attempt++;
                try {
                    return transactionTemplate.execute(status -> work.get());
                } catch (PessimisticLockingFailureException exception) {
                    if (attempt >= maxAttempts) throw exception;
                    pause();
                }
            }
        }

        private void pause() {
            try {
                Thread.sleep(backoff.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("잠금 재시도 대기가 중단되었습니다.", exception);
            }
        }
    }
}
