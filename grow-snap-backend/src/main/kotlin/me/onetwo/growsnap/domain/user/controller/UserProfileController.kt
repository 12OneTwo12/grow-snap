package me.onetwo.growsnap.domain.user.controller

import jakarta.validation.Valid
import me.onetwo.growsnap.domain.user.dto.CreateProfileRequest
import me.onetwo.growsnap.domain.user.dto.ImageUploadResponse
import me.onetwo.growsnap.domain.user.dto.NicknameCheckResponse
import me.onetwo.growsnap.domain.user.dto.UpdateProfileRequest
import me.onetwo.growsnap.domain.user.dto.UserProfileResponse
import me.onetwo.growsnap.domain.user.service.UserProfileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
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
     * @param userId 인증된 사용자 ID (Spring Security에서 자동 주입)
     * @param request 프로필 생성 요청
     * @return 생성된 프로필 정보
     */
    @PostMapping
    fun createProfile(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateProfileRequest
    ): Mono<ResponseEntity<UserProfileResponse>> {
        return Mono.fromCallable {
            val profile = userProfileService.createProfile(
                userId = userId,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
                bio = request.bio
            )
            ResponseEntity.status(HttpStatus.CREATED).body(UserProfileResponse.from(profile))
        }
    }

    /**
     * 내 프로필 조회
     *
     * @param userId 인증된 사용자 ID (Spring Security에서 자동 주입)
     * @return 프로필 정보
     */
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal userId: UUID
    ): Mono<ResponseEntity<UserProfileResponse>> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByUserId(userId)
            ResponseEntity.ok(UserProfileResponse.from(profile))
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
    ): Mono<ResponseEntity<UserProfileResponse>> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByUserId(targetUserId)
            ResponseEntity.ok(UserProfileResponse.from(profile))
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
    ): Mono<ResponseEntity<UserProfileResponse>> {
        return Mono.fromCallable {
            val profile = userProfileService.getProfileByNickname(nickname)
            ResponseEntity.ok(UserProfileResponse.from(profile))
        }
    }

    /**
     * 프로필 수정
     *
     * @param userId 인증된 사용자 ID (Spring Security에서 자동 주입)
     * @param request 프로필 수정 요청
     * @return 수정된 프로필 정보
     */
    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: UpdateProfileRequest
    ): Mono<ResponseEntity<UserProfileResponse>> {
        return Mono.fromCallable {
            val profile = userProfileService.updateProfile(
                userId = userId,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
                bio = request.bio
            )
            ResponseEntity.ok(UserProfileResponse.from(profile))
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
    ): Mono<ResponseEntity<NicknameCheckResponse>> {
        return Mono.fromCallable {
            val isDuplicated = userProfileService.isNicknameDuplicated(nickname)
            ResponseEntity.ok(NicknameCheckResponse(nickname, isDuplicated))
        }
    }

    /**
     * 프로필 이미지 업로드
     *
     * 이미지는 리사이징된 후 S3에 저장되며, 업로드된 이미지 URL을 반환합니다.
     *
     * @param userId 인증된 사용자 ID (Spring Security에서 자동 주입)
     * @param filePart 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    @PostMapping("/image")
    fun uploadProfileImage(
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("file") filePart: Mono<FilePart>
    ): Mono<ResponseEntity<ImageUploadResponse>> {
        return filePart
            .flatMap { file ->
                userProfileService.uploadProfileImage(userId, file)
            }
            .map { imageUrl ->
                ResponseEntity.status(HttpStatus.CREATED).body(ImageUploadResponse(imageUrl))
            }
    }
}
