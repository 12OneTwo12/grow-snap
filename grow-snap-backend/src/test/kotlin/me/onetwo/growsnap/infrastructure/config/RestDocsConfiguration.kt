package me.onetwo.growsnap.infrastructure.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors

/**
 * REST Docs 테스트 설정
 *
 * Spring REST Docs 문서 생성을 위한 설정입니다.
 */
@TestConfiguration
class RestDocsConfiguration {

    @Bean
    fun restDocsMockMvcConfigurationCustomizer() =
        org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer { configurer ->
            configurer.operationPreprocessors()
                .withRequestDefaults(Preprocessors.prettyPrint())
                .withResponseDefaults(Preprocessors.prettyPrint())
        }
}
