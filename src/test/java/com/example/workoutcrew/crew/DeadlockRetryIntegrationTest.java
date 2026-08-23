package com.example.workoutcrew.crew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.global.config.RetryConfig.LockRetryExecutor;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;

class DeadlockRetryIntegrationTest extends ApiIntegrationSupport {

    @Autowired LockRetryExecutor retryExecutor;

    @Test
    void 잠금_실패는_최대_세_번만_재시도하고_성공값을_반환한다() {
        AtomicInteger recoverable = new AtomicInteger();
        String result = retryExecutor.execute(() -> {
            if (recoverable.incrementAndGet() < 3) throw new PessimisticLockingFailureException("deadlock");
            return "성공";
        });
        assertThat(result).isEqualTo("성공");
        assertThat(recoverable).hasValue(3);

        AtomicInteger exhausted = new AtomicInteger();
        assertThatThrownBy(() -> retryExecutor.execute(() -> {
            exhausted.incrementAndGet();
            throw new PessimisticLockingFailureException("deadlock");
        })).isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(exhausted).hasValue(3);
    }
}
