package com.example.workoutcrew.crew;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.service.CrewManagementService;
import com.example.workoutcrew.crew.service.CrewMembershipService;
import com.example.workoutcrew.crew.dto.CrewUpdateRequest;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrewManagerConcurrencyIntegrationTest extends ApiIntegrationSupport {

    @Autowired CrewManagementService managementService;
    @Autowired CrewMembershipService membershipService;

    @Test
    void 동시_위임_후에도_MANAGER는_정확히_한_명이다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User first = saveUser("first@example.com", "첫회원");
        User second = saveUser("second@example.com", "둘회원");
        Crew crew = crewRepository.save(Crew.create("동시위임", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.save(CrewUser.member(first, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(second, crew));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> one = executor.submit(() -> transfer(start, crew.getId(), owner.getId(), first.getId()));
        Future<Boolean> two = executor.submit(() -> transfer(start, crew.getId(), owner.getId(), second.getId()));
        start.countDown();
        assertThat(List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
        executor.shutdownNow();
        assertThat(crewUserRepository.findByCrewId(crew.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(membership -> membership.getRole() == CrewRole.MANAGER).count()).isEqualTo(1);
    }

    @Test
    void 위임과_관리자_탈퇴가_경쟁해도_관리자_없는_크루가_남지_않는다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        Crew crew = crewRepository.save(Crew.create("위임탈퇴", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> transfer = executor.submit(() -> transfer(start, crew.getId(), owner.getId(), member.getId()));
        Future<Boolean> leave = executor.submit(() -> {
            try { start.await(); membershipService.leave(crew.getId(), owner.getId()); return true; }
            catch (Exception exception) { return false; }
        });
        start.countDown();
        transfer.get();
        leave.get();
        executor.shutdownNow();
        if (crewRepository.existsById(crew.getId())) {
            assertThat(crewUserRepository.findByCrewId(crew.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .stream().filter(membership -> membership.getRole() == CrewRole.MANAGER).count()).isEqualTo(1);
        }
    }

    @Test
    void 위임_대상_추방과_위임이_경쟁해도_MANAGER는_정확히_한_명이다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User target = saveUser("target@example.com", "대상원");
        Crew crew = crewRepository.save(Crew.create("위임추방", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(target, crew));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> transfer = executor.submit(() -> transfer(start, crew.getId(), owner.getId(), target.getId()));
        Future<Boolean> kick = executor.submit(() -> {
            try { start.await(); managementService.kick(crew.getId(), owner.getId(), target.getId()); return true; }
            catch (Exception exception) { return false; }
        });
        start.countDown();
        assertThat(List.of(transfer.get(), kick.get())).containsExactlyInAnyOrder(true, false);
        executor.shutdownNow();
        assertThat(crewUserRepository.findByCrewId(crew.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(membership -> membership.getRole() == CrewRole.MANAGER).count()).isEqualTo(1);
    }

    @Test
    void 정원_축소와_가입이_경쟁해도_현재인원이_정원을_넘지_않는다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User existing = saveUser("existing@example.com", "기존원");
        User joining = saveUser("joining@example.com", "가입원");
        Crew crew = crewRepository.save(Crew.create("정원경쟁", passwordEncoder.encode("crew12"), 3, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(existing, crew));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> update = executor.submit(() -> {
            try { start.await(); managementService.update(crew.getId(), owner.getId(),
                    new CrewUpdateRequest(null, null, 2, null)); return true; }
            catch (Exception exception) { return false; }
        });
        Future<Boolean> join = executor.submit(() -> {
            try { start.await(); membershipService.join(crew.getId(), joining.getId(), "crew12"); return true; }
            catch (Exception exception) { return false; }
        });
        start.countDown();
        assertThat(List.of(update.get(), join.get())).containsExactlyInAnyOrder(true, false);
        executor.shutdownNow();
        Crew reloaded = crewRepository.findById(crew.getId()).orElseThrow();
        assertThat(crewUserRepository.countByCrewId(crew.getId())).isLessThanOrEqualTo(reloaded.getMaxUsers());
    }

    private boolean transfer(CountDownLatch start, Long crewId, Long ownerId, Long targetId) {
        try {
            start.await();
            managementService.transferManager(crewId, ownerId, targetId);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
