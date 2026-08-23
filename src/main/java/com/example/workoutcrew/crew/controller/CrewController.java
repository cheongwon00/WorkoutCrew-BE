package com.example.workoutcrew.crew.controller;

import com.example.workoutcrew.crew.dto.CrewCreateRequest;
import com.example.workoutcrew.crew.dto.CrewSummaryResponse;
import com.example.workoutcrew.crew.dto.CrewUpdateRequest;
import com.example.workoutcrew.crew.service.CrewManagementService;
import com.example.workoutcrew.crew.service.CrewService;
import com.example.workoutcrew.global.response.ApiResponse;
import com.example.workoutcrew.global.response.PageData;
import com.example.workoutcrew.global.security.CustomPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crews")
public class CrewController {

    private final CrewService crewService;
    private final CrewManagementService managementService;
    private final Clock clock;

    public CrewController(CrewService crewService, CrewManagementService managementService, Clock clock) {
        this.crewService = crewService;
        this.managementService = managementService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@AuthenticationPrincipal CustomPrincipal principal,
                                                    @Valid @RequestBody CrewCreateRequest request) {
        Long crewId = crewService.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/crews/" + crewId))
                .body(ApiResponse.success(HttpStatus.CREATED, "크루가 생성되었습니다.", null, clock));
    }

    @GetMapping
    public ApiResponse<PageData<CrewSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        return ApiResponse.success(HttpStatus.OK, "크루 목록을 조회했습니다.",
                crewService.list(PageRequestFactory.create(page, size, sort)), clock);
    }

    @PatchMapping("/{crewId}")
    public ApiResponse<Void> update(@PathVariable Long crewId,
                                    @AuthenticationPrincipal CustomPrincipal principal,
                                    @Valid @RequestBody CrewUpdateRequest request) {
        managementService.update(crewId, principal.userId(), request);
        return ApiResponse.success(HttpStatus.OK, "크루 정보가 수정되었습니다.", null, clock);
    }

    @DeleteMapping("/{crewId}")
    public ApiResponse<Void> delete(@PathVariable Long crewId,
                                    @AuthenticationPrincipal CustomPrincipal principal) {
        managementService.delete(crewId, principal.userId());
        return ApiResponse.success(HttpStatus.OK, "크루가 삭제되었습니다.", null, clock);
    }
}
