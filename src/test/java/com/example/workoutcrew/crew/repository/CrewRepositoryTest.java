package com.example.workoutcrew.crew.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

class CrewRepositoryTest extends ApiIntegrationSupport {

    @Test
    @Transactional
    void 이름_고유성과_id_정렬_페이지와_쓰기잠금_조회가_동작한다() {
        Crew first = crewRepository.save(Crew.create("첫크루", passwordEncoder.encode("crew12"), 5, 2));
        Crew second = crewRepository.saveAndFlush(Crew.create("둘크루", passwordEncoder.encode("crew12"), 5, 2));
        assertThat(crewRepository.existsByName("첫크루")).isTrue();
        assertThat(crewRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .getContent()).extracting(Crew::getId).containsExactly(second.getId());
        assertThat(crewRepository.findByIdForUpdate(first.getId())).isPresent();
    }
}
