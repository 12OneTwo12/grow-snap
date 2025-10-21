plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("kapt") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    id("jacoco")
    id ("io.gitlab.arturbosch.detekt") version "1.23.6"

    id("nu.studer.jooq") version "8.2"
}

group = "me.onetwo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")
val jooqVersion = "3.17.23"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Redis Reactive
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // BCrypt
    implementation("org.springframework.security:spring-security-crypto")

    // JDBC for JOOQ
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // JOOQ 기본 런타임 라이브러리
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("org.springframework.boot:spring-boot-starter-jooq") // jooq, Spring과 통합하는 경우 추가

    // JOOQ 코드 생성 관련 라이브러리
    jooqGenerator("org.jooq:jooq-meta:$jooqVersion")
    jooqGenerator("org.jooq:jooq-codegen:$jooqVersion")
    // DDLDatabase 지원 라이브러리 추가
    jooqGenerator("org.jooq:jooq-meta-extensions:$jooqVersion")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
    finalizedBy("jacocoTestReport")
}

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
}


tasks.register<Copy>("copyDocument") {
    dependsOn(tasks.asciidoctor)
    from(file("build/docs/asciidoc"))
    into(file("src/main/resources/static/docs"))
}

tasks.register<Copy>("buildDocument") {
    dependsOn("copyDocument")
    from(file("src/main/resources/static/docs"))
    into(file("build/resources/main/static/docs"))
}

tasks.named("bootJar") {
    dependsOn("buildDocument")
}

tasks.named("resolveMainClassName") {
    dependsOn("buildDocument")
}


// jacoco test 커버리지
extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

val excludedClasses = listOf(
    "**/GrowSnapApplication.class",
    "**/filter/**",
    "**/dto/**",
    "**/exception/**",
    "**/config/**",
    "**/generated/**",
    "**/docs/**",
    "**/test/**"
)

val jacocoClassDirectories = layout.buildDirectory.dir("classes").map { dir ->
    fileTree(dir) {
        include("**/*.class")
        exclude(excludedClasses)
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    classDirectories.setFrom(jacocoClassDirectories)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)

    classDirectories.setFrom(jacocoClassDirectories)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.1".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.1".toBigDecimal()
            }
        }
    }
}

val detektConfigFile = file("$rootDir/config/detekt/detekt.yml")

detekt {
    toolVersion = "1.23.6"
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file(detektConfigFile)) // Detekt에서 제공된 yml에서 Rule 설정 On/Off 가능
    ignoreFailures = true // detekt 빌드시 실패 ignore 처리
}

jooq {
    version.set(jooqVersion)

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase" // DDL 파일 사용 설정
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "src/main/resources/sql/create-table-sql.sql" // DDL 파일 경로
                            }
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "sort"
                                value = "semantic" // 테이블 참조 관계 정렬
                            }
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "unqualifiedSchema"
                                value = "true" // 스키마 이름 제거
                            }
                        )
                    }

                    generate.apply {
                        isPojos = true  // Kotlin 데이터 클래스로 생성
                        isDaos = true   // DAO 클래스 생성
                    }

                    target.apply {
                        packageName = "com.onetwo.growsnap.jooq.generated" // JOOQ 코드 저장 경로
                        directory = "src/main/generated"
                    }
                }
            }
        }
    }
}

sourceSets {
    main {
        kotlin {
            srcDirs("src/main/generated") // JOOQ 코드가 있는 폴더 소스 코드로 추가
        }
    }
}