package com.example.workoutcrew.crew;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.service.CrewMembershipService;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrewJoinConcurrencyIntegrationTest extends ApiIntegrationSupport {

    @Autowired CrewMembershipService membershipService;

    @Test
    void 마지막_한_자리에_동시_가입해도_정원을_넘지_않는다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User first = saveUser("first@example.com", "가입일");
        User second = saveUser("second@example.com", "가입이");
        Crew crew = crewRepository.save(Crew.create("동시가입", passwordEncoder.encode("crew12"), 2, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));
        List<Boolean> results = runTogether(
                () -> attemptJoin(crew.getId(), first.getId()),
                () -> attemptJoin(crew.getId(), second.getId()));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(crewUserRepository.countByCrewId(crew.getId())).isEqualTo(2);
    }

    @Test
    void 같은_사용자의_동시_가입도_소속을_하나만_만든다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "가입자");
        Crew crew = crewRepository.save(Crew.create("중복가입", passwordEncoder.encode("crew12"), 3, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));
        List<Boolean> results = runTogether(
                () -> attemptJoin(crew.getId(), member.getId()),
                () -> attemptJoin(crew.getId(), member.getId()));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(crewUserRepository.countByCrewId(crew.getId())).isEqualTo(2);
    }

    private boolean attemptJoin(Long crewId, Long userId) {
        try {
            membershipService.join(crewId, userId, "crew12");
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private List<Boolean> runTogether(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> wrapFirst = () -> { ready.countDown(); start.await(); return first.call(); };
        Callable<Boolean> wrapSecond = () -> { ready.countDown(); start.await(); return second.call(); };
        Future<Boolean> one = executor.submit(wrapFirst);
        Future<Boolean> two = executor.submit(wrapSecond);
        ready.await();
        start.countDown();
        List<Boolean> result = List.of(one.get(), two.get());
        executor.shutdownNow();
        return result;
    }
}
