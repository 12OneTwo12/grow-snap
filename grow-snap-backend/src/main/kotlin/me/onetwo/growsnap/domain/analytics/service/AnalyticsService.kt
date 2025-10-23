package me.onetwo.growsnap.domain.analytics.service

import me.onetwo.growsnap.domain.analytics.dto.InteractionEventRequest
import me.onetwo.growsnap.domain.analytics.dto.ViewEventRequest
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Analytics 서비스 인터페이스
 *
 * 사용자 행동 추적 및 개인화 추천을 위한 분석 기능을 제공합니다.
 */
interface AnalyticsService {

    /**
     * 시청 이벤트 추적
     *
     * 사용자의 콘텐츠 시청 기록을 저장하고, view_count를 증가시킵니다.
     * 스킵 이벤트(skipped=true)인 경우 view_count는 증가시키지 않습니다.
     *
     * @param userId 사용자 ID
     * @param request 시청 이벤트 요청
     * @return 처리 완료 신호
     */
    fun trackViewEvent(userId: UUID, request: ViewEventRequest): Mono<Void>

    /**
     * 인터랙션 이벤트 추적
     *
     * 사용자의 콘텐츠 인터랙션 (좋아요, 저장, 공유, 댓글)을 추적합니다.
     * 해당 인터랙션 타입의 카운터를 증가시킵니다.
     *
     * @param userId 사용자 ID
     * @param request 인터랙션 이벤트 요청
     * @return 처리 완료 신호
     */
    fun trackInteractionEvent(userId: UUID, request: InteractionEventRequest): Mono<Void>
}
