package me.onetwo.growsnap.domain.user.controller

import jakarta.validation.Valid
import me.onetwo.growsnap.domain.user.dto.*
import me.onetwo.growsnap.domain.user.service.UserProfileService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 사용자 프로필 관리 Controller
 *
 * 프로필 생성, 조회, 수정, 닉네임 중복 확인 등의 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/profiles")
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    /**
     * 프로필 생성
     *
     * @param userId 사용자 ID (인증된 사용자)
     * @param request 프로필 생성 요청
     * @return 생성된 프로필 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProfile(
        @RequestAttribute userId: UUID,
        @Valid @RequestBody request: CreateProfileRequest
    ): Mono<UserProfileResponse> {
        return Mono.fromCallable {
            val profile = userProfileService.createProfile(
                userId = userId,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
                bio = request.bio
            )
            UserProfileResponse.from(profile)
        }
    }

    /**
     * 내 프로필 조회
     *
     * @param userId 사용자 ID (인증된 사용자)
     * @return 프로필 정보
     */
    @GetMapping("/me")
    fun getMyProfile(
        @RequestAttribute userId: UUID
    ): Mono<UserProfileResponse> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByUserId(userId)
            UserProfileResponse.from(profile)
        }
    }

    /**
     * 사용자 ID로 프로필 조회
     *
     * @param targetUserId 조회할 사용자 ID
     * @return 프로필 정보
     */
    @GetMapping("/{targetUserId}")
    fun getProfileByUserId(
        @PathVariable targetUserId: UUID
    ): Mono<UserProfileResponse> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByUserId(targetUserId)
            UserProfileResponse.from(profile)
        }
    }

    /**
     * 닉네임으로 프로필 조회
     *
     * @param nickname 조회할 닉네임
     * @return 프로필 정보
     */
    @GetMapping("/nickname/{nickname}")
    fun getProfileByNickname(
        @PathVariable nickname: String
    ): Mono<UserProfileResponse> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByNickname(nickname)
            UserProfileResponse.from(profile)
        }
    }

    /**
     * 프로필 수정
     *
     * @param userId 사용자 ID (인증된 사용자)
     * @param request 프로필 수정 요청
     * @return 수정된 프로필 정보
     */
    @PatchMapping
    fun updateProfile(
        @RequestAttribute userId: UUID,
        @Valid @RequestBody request: UpdateProfileRequest
    ): Mono<UserProfileResponse> {
        return Mono.fromCallable {
            val profile = userProfileService.updateProfile(
                userId = userId,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
                bio = request.bio
            )
            UserProfileResponse.from(profile)
        }
    }

    /**
     * 닉네임 중복 확인
     *
     * @param nickname 확인할 닉네임
     * @return 중복 여부
     */
    @GetMapping("/check/nickname/{nickname}")
    fun checkNickname(
        @PathVariable nickname: String
    ): Mono<NicknameCheckResponse> {
        return Mono.fromCallable {
            val isDuplicated = userProfileService.isNicknameDuplicated(nickname)
            NicknameCheckResponse(nickname, isDuplicated)
        }
    }
}
