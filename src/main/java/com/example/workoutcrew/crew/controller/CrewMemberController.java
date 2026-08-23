package com.example.workoutcrew.crew.controller;

import com.example.workoutcrew.crew.dto.CrewJoinRequest;
import com.example.workoutcrew.crew.dto.CrewMemberResponse;
import com.example.workoutcrew.crew.dto.ManagerTransferRequest;
import com.example.workoutcrew.crew.service.CrewManagementService;
import com.example.workoutcrew.crew.service.CrewMembershipService;
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
@RequestMapping("/api/v1/crews/{crewId}")
public class CrewMemberController {

    private final CrewMembershipService membershipService;
    private final CrewManagementService managementService;
    private final Clock clock;

    public CrewMemberController(CrewMembershipService membershipService,
                                CrewManagementService managementService, Clock clock) {
        this.membershipService = membershipService;
        this.managementService = managementService;
        this.clock = clock;
    }

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Void>> join(@PathVariable Long crewId,
                                                  @AuthenticationPrincipal CustomPrincipal principal,
                                                  @Valid @RequestBody CrewJoinRequest request) {
        membershipService.join(crewId, principal.userId(), request.password());
        return ResponseEntity.created(URI.create("/api/v1/crews/" + crewId + "/members/" + principal.userId()))
                .body(ApiResponse.success(HttpStatus.CREATED, "크루에 가입했습니다.", null, clock));
    }

    @GetMapping("/members")
    public ApiResponse<PageData<CrewMemberResponse>> list(
            @PathVariable Long crewId,
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        return ApiResponse.success(HttpStatus.OK, "크루원 목록을 조회했습니다.",
                membershipService.listMembers(crewId, principal.userId(),
                        PageRequestFactory.create(page, size, sort)), clock);
    }

    @DeleteMapping("/members/me")
    public ApiResponse<Void> leave(@PathVariable Long crewId,
                                   @AuthenticationPrincipal CustomPrincipal principal) {
        membershipService.leave(crewId, principal.userId());
        return ApiResponse.success(HttpStatus.OK, "크루에서 탈퇴했습니다.", null, clock);
    }

    @PatchMapping("/manager")
    public ApiResponse<Void> transfer(@PathVariable Long crewId,
                                      @AuthenticationPrincipal CustomPrincipal principal,
                                      @Valid @RequestBody ManagerTransferRequest request) {
        managementService.transferManager(crewId, principal.userId(), request.targetUserId());
        return ApiResponse.success(HttpStatus.OK, "관리자 권한이 위임되었습니다.", null, clock);
    }

    @DeleteMapping("/members/{userId}")
    public ApiResponse<Void> kick(@PathVariable Long crewId, @PathVariable Long userId,
                                  @AuthenticationPrincipal CustomPrincipal principal) {
        managementService.kick(crewId, principal.userId(), userId);
        return ApiResponse.success(HttpStatus.OK, "크루원이 추방되었습니다.", null, clock);
    }
}
