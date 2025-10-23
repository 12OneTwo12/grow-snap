package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.content.model.Category
import me.onetwo.growsnap.domain.content.model.ContentStatus
import me.onetwo.growsnap.domain.content.model.ContentType
import me.onetwo.growsnap.domain.interaction.model.Comment
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.repository.UserRepository
import me.onetwo.growsnap.jooq.generated.tables.ContentInteractions.Companion.CONTENT_INTERACTIONS
import me.onetwo.growsnap.jooq.generated.tables.ContentMetadata.Companion.CONTENT_METADATA
import me.onetwo.growsnap.jooq.generated.tables.Contents.Companion.CONTENTS
import org.jooq.DSLContext
import org.jooq.JSON
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("댓글 Repository 통합 테스트")
class CommentRepositoryTest {

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    private lateinit var testUser: User
    private lateinit var testContentId: UUID

    @BeforeEach
    fun setUp() {
        // Given: 테스트 데이터 준비
        testUser = userRepository.save(
            User(
                email = "user@test.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "user-123",
                role = UserRole.USER
            )
        )

        testContentId = UUID.randomUUID()
        insertContent(testContentId, testUser.id!!, "Test Video")
    }

    @Nested
    @DisplayName("save - 댓글 생성")
    inner class Save {

        @Test
        @DisplayName("댓글을 생성하면, 데이터베이스에 저장된다")
        fun save_CreatesComment() {
            // Given: 댓글 데이터
            val comment = Comment(
                contentId = testContentId,
                userId = testUser.id!!,
                content = "Test comment",
                timestampSeconds = null,
                parentCommentId = null
            )

            // When: 댓글 저장
            val saved = commentRepository.save(comment).block()

            // Then: 저장된 댓글 확인
            assertNotNull(saved)
            assertNotNull(saved!!.id)
            assertEquals(testContentId, saved.contentId)
            assertEquals(testUser.id!!, saved.userId)
            assertEquals("Test comment", saved.content)
        }

        @Test
        @DisplayName("타임스탬프 댓글을 생성하면, timestampSeconds가 저장된다")
        fun save_WithTimestamp_SavesTimestamp() {
            // Given: 타임스탬프 댓글
            val comment = Comment(
                contentId = testContentId,
                userId = testUser.id!!,
                content = "Comment at 30 seconds",
                timestampSeconds = 30,
                parentCommentId = null
            )

            // When: 저장
            val saved = commentRepository.save(comment).block()

            // Then: 타임스탬프 확인
            assertNotNull(saved)
            assertEquals(30, saved!!.timestampSeconds)
        }

        @Test
        @DisplayName("대댓글을 생성하면, parentCommentId가 저장된다")
        fun save_WithParent_SavesParentCommentId() {
            // Given: 부모 댓글 생성
            val parentComment = commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Parent comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            // 대댓글 생성
            val reply = Comment(
                contentId = testContentId,
                userId = testUser.id!!,
                content = "Reply comment",
                timestampSeconds = null,
                parentCommentId = parentComment!!.id
            )

            // When: 대댓글 저장
            val saved = commentRepository.save(reply).block()

            // Then: parentCommentId 확인
            assertNotNull(saved)
            assertEquals(parentComment.id, saved!!.parentCommentId)
        }
    }

    @Nested
    @DisplayName("findById - ID로 댓글 조회")
    inner class FindById {

        @Test
        @DisplayName("존재하는 댓글 ID로 조회하면, 댓글이 반환된다")
        fun findById_ExistingId_ReturnsComment() {
            // Given: 댓글 생성
            val comment = commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Test comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            // When: ID로 조회
            val found = commentRepository.findById(comment!!.id!!).block()

            // Then: 댓글 반환 확인
            assertNotNull(found)
            assertEquals(comment.id, found!!.id)
            assertEquals(comment.content, found.content)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면, empty Mono가 반환된다")
        fun findById_NonExistingId_ReturnsEmpty() {
            // Given: 존재하지 않는 ID
            val nonExistingId = UUID.randomUUID()

            // When: 조회
            val found = commentRepository.findById(nonExistingId).block()

            // Then: null 반환
            assertEquals(null, found)
        }
    }

    @Nested
    @DisplayName("findByContentId - 콘텐츠의 댓글 목록 조회")
    inner class FindByContentId {

        @Test
        @DisplayName("콘텐츠의 모든 댓글을 조회하면, 생성일시 오름차순으로 반환된다")
        fun findByContentId_ReturnsCommentsOrderedByCreatedAt() {
            // Given: 3개의 댓글 생성
            commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "First comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            Thread.sleep(10) // 생성 시간 차이를 위해 대기

            commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Second comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            Thread.sleep(10)

            commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Third comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            // When: 댓글 목록 조회
            val comments = commentRepository.findByContentId(testContentId).collectList().block()

            // Then: 3개의 댓글이 생성 시간 순서대로 반환
            assertNotNull(comments)
            assertEquals(3, comments!!.size)
            assertEquals("First comment", comments[0].content)
            assertEquals("Second comment", comments[1].content)
            assertEquals("Third comment", comments[2].content)
        }

        @Test
        @DisplayName("삭제된 댓글은 조회되지 않는다")
        fun findByContentId_ExcludesDeletedComments() {
            // Given: 댓글 생성 후 삭제
            val comment = commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Comment to be deleted",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            commentRepository.delete(comment!!.id!!, testUser.id!!).block()

            // When: 댓글 목록 조회
            val comments = commentRepository.findByContentId(testContentId).collectList().block()

            // Then: 삭제된 댓글은 조회되지 않음
            assertNotNull(comments)
            assertEquals(0, comments!!.size)
        }
    }

    @Nested
    @DisplayName("findByParentCommentId - 부모 댓글의 대댓글 조회")
    inner class FindByParentCommentId {

        @Test
        @DisplayName("부모 댓글의 대댓글을 조회하면, 모든 대댓글이 반환된다")
        fun findByParentCommentId_ReturnsAllReplies() {
            // Given: 부모 댓글 생성
            val parentComment = commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Parent comment",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            // 대댓글 2개 생성
            commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Reply 1",
                    timestampSeconds = null,
                    parentCommentId = parentComment!!.id
                )
            ).block()

            commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Reply 2",
                    timestampSeconds = null,
                    parentCommentId = parentComment.id
                )
            ).block()

            // When: 대댓글 조회
            val replies = commentRepository.findByParentCommentId(parentComment.id!!).collectList().block()

            // Then: 2개의 대댓글 반환
            assertNotNull(replies)
            assertEquals(2, replies!!.size)
        }
    }

    @Nested
    @DisplayName("delete - 댓글 삭제")
    inner class Delete {

        @Test
        @DisplayName("댓글을 삭제하면, Soft Delete가 적용된다")
        fun delete_AppliesSoftDelete() {
            // Given: 댓글 생성
            val comment = commentRepository.save(
                Comment(
                    contentId = testContentId,
                    userId = testUser.id!!,
                    content = "Comment to delete",
                    timestampSeconds = null,
                    parentCommentId = null
                )
            ).block()

            // When: 댓글 삭제
            commentRepository.delete(comment!!.id!!, testUser.id!!).block()

            // Then: findById로 조회되지 않음 (deletedAt이 설정됨)
            val found = commentRepository.findById(comment.id!!).block()
            assertEquals(null, found)
        }
    }

    /**
     * 콘텐츠 삽입 헬퍼 메서드
     */
    private fun insertContent(
        contentId: UUID,
        creatorId: UUID,
        title: String
    ) {
        val now = LocalDateTime.now()

        dslContext.insertInto(CONTENTS)
            .set(CONTENTS.ID, contentId.toString())
            .set(CONTENTS.CREATOR_ID, creatorId.toString())
            .set(CONTENTS.CONTENT_TYPE, ContentType.VIDEO.name)
            .set(CONTENTS.URL, "https://example.com/$contentId.mp4")
            .set(CONTENTS.THUMBNAIL_URL, "https://example.com/$contentId-thumb.jpg")
            .set(CONTENTS.DURATION, 60)
            .set(CONTENTS.WIDTH, 1920)
            .set(CONTENTS.HEIGHT, 1080)
            .set(CONTENTS.STATUS, ContentStatus.PUBLISHED.name)
            .set(CONTENTS.CREATED_AT, now)
            .set(CONTENTS.CREATED_BY, creatorId.toString())
            .set(CONTENTS.UPDATED_AT, now)
            .set(CONTENTS.UPDATED_BY, creatorId.toString())
            .execute()

        dslContext.insertInto(CONTENT_METADATA)
            .set(CONTENT_METADATA.CONTENT_ID, contentId.toString())
            .set(CONTENT_METADATA.TITLE, title)
            .set(CONTENT_METADATA.DESCRIPTION, "Test Description")
            .set(CONTENT_METADATA.CATEGORY, Category.PROGRAMMING.name)
            .set(CONTENT_METADATA.TAGS, JSON.valueOf("[\"test\"]"))
            .set(CONTENT_METADATA.LANGUAGE, "ko")
            .set(CONTENT_METADATA.CREATED_AT, now)
            .set(CONTENT_METADATA.CREATED_BY, creatorId.toString())
            .set(CONTENT_METADATA.UPDATED_AT, now)
            .set(CONTENT_METADATA.UPDATED_BY, creatorId.toString())
            .execute()

        dslContext.insertInto(CONTENT_INTERACTIONS)
            .set(CONTENT_INTERACTIONS.CONTENT_ID, contentId.toString())
            .set(CONTENT_INTERACTIONS.VIEW_COUNT, 0)
            .set(CONTENT_INTERACTIONS.LIKE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.COMMENT_COUNT, 0)
            .set(CONTENT_INTERACTIONS.SAVE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.SHARE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.CREATED_AT, now)
            .set(CONTENT_INTERACTIONS.CREATED_BY, creatorId.toString())
            .set(CONTENT_INTERACTIONS.UPDATED_AT, now)
            .set(CONTENT_INTERACTIONS.UPDATED_BY, creatorId.toString())
            .execute()
    }
}
