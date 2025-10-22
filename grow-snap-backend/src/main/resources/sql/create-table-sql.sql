-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL DEFAULT 'GOOGLE',
    provider_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_id)
);

CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_provider_id ON users(provider_id);

-- User Profiles Table
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL UNIQUE,
    nickname VARCHAR(20) NOT NULL UNIQUE,
    profile_image_url VARCHAR(500),
    bio VARCHAR(500),
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_nickname ON user_profiles(nickname);
CREATE INDEX idx_user_id ON user_profiles(user_id);

-- Follows Table (팔로우 관계)
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id CHAR(36) NOT NULL,
    following_id CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_follow UNIQUE (follower_id, following_id)
);

CREATE INDEX idx_follower ON follows(follower_id);
CREATE INDEX idx_following ON follows(following_id);

-- Creator Applications Table (크리에이터 전환 신청) - 제거됨: 모든 유저가 크리에이터이자 소비자
-- CREATE TABLE IF NOT EXISTS creator_applications (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     user_id CHAR(36) NOT NULL,
--     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
--     reason TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
-- );
--
-- CREATE INDEX idx_creator_app_user ON creator_applications(user_id);
-- CREATE INDEX idx_creator_app_status ON creator_applications(status);