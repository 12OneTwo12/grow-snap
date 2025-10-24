# GrowSnap Backend 개발 원칙 (Claude용)

> **⚠️ DEPRECATED**: 이 파일은 더 이상 사용되지 않습니다.

## 🚀 새로운 Skill 기반 시스템으로 이동

**토큰 효율성 개선**: 기존 40,000 토큰 → 평균 2,000-10,000 토큰 (75-95% 절감)

### 📖 시작하기

**`.claude/skills/README.md` 파일을 읽어주세요.**

모든 CLAUDE가 참고할 개발 규칙과 가이드는 `.claude/skills/` 디렉토리의 Skill 파일로 이동했습니다.

### 📁 Skill 목록

| Skill 파일 | 내용 | 사용 시기 |
|-----------|------|----------|
| **README.md** | Skill 사용 가이드 | 처음 시작 시 |
| **quick-reference.md** | 빠른 참조 체크리스트 | 모든 작업 시작 전 |
| **core-principles.md** | TDD, SOLID, Audit Trail | 새로운 기능 개발 시 |
| **mvc-layers.md** | Controller/Service/Repository | API 개발 시 |
| **testing-guide.md** | 테스트 템플릿 | 테스트 작성 시 |
| **api-design.md** | REST API, HTTP 상태 코드 | API 설계 시 |
| **database-query.md** | JOOQ, Soft Delete | DB 쿼리 작성 시 |
| **code-style.md** | 로깅, 네이밍, WebFlux | 코드 작성 시 |
| **spring-event.md** | Spring Event 비동기 처리 | 이벤트 구현 시 |

### 💡 사용 방법

#### 1. 빠른 참조 (권장)
```
모든 작업 시작 전: /skill quick-reference
```

#### 2. 작업별 Skill 조합
```
새로운 API 개발: /skill mvc-layers + /skill testing-guide
데이터베이스 쿼리: /skill database-query
코드 리뷰: /skill quick-reference
```

#### 3. 자연스러운 요청
```
"좋아요 기능 추가 구현해"
→ Claude가 자동으로 필요한 Skill을 선택
```

### 📊 토큰 절감 효과

| 작업 유형 | 기존 CLAUDE.md | Skill 사용 | 절감 |
|---------|---------------|-----------|------|
| API 개발 | 40,000 토큰 | ~8,000 | 80% ↓ |
| 테스트 작성 | 40,000 토큰 | ~4,000 | 90% ↓ |
| 코드 리뷰 | 40,000 토큰 | ~2,000 | 95% ↓ |

---

## 📖 참고 문서

- **Claude용**: `.claude/skills/README.md` - Skill 사용 가이드
- **개발자용**: `/docs/BACKEND_DEVELOPMENT_GUIDE.md` - 백엔드 개발 컨벤션 가이드
