package com.example.workoutcrew.crew.repository;

import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrewUserRepository extends JpaRepository<CrewUser, Long> {
    boolean existsByCrewIdAndUserId(Long crewId, Long userId);
    Optional<CrewUser> findByCrewIdAndUserId(Long crewId, Long userId);
    long countByCrewId(Long crewId);

    @EntityGraph(attributePaths = {"user"})
    Page<CrewUser> findByCrewId(Long crewId, Pageable pageable);

    List<CrewUser> findByUserId(Long userId);
    List<CrewUser> findByUserIdAndRoleOrderByCrewIdAsc(Long userId, CrewRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cu from CrewUser cu join fetch cu.user where cu.crew.id = :crewId and cu.user.id = :userId")
    Optional<CrewUser> findByCrewIdAndUserIdForUpdate(@Param("crewId") Long crewId,
                                                       @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cu from CrewUser cu join fetch cu.user where cu.crew.id = :crewId order by cu.id asc")
    List<CrewUser> findAllByCrewIdForUpdate(@Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CrewUser cu where cu.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CrewUser cu where cu.crew.id = :crewId")
    int deleteAllByCrewId(@Param("crewId") Long crewId);
}
