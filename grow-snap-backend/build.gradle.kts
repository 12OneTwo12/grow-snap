plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("kapt") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    id("jacoco")
    id ("io.gitlab.arturbosch.detekt") version "1.23.6"
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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.bootJar {
    dependsOn("buildDocument")
}


// jacoco test 커버리지
extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

tasks.test {
    finalizedBy("jacocoTestReport")
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

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    val allClassesDirs = layout.buildDirectory.dir("classes")

    classDirectories.setFrom(
        allClassesDirs.map { dir ->
            fileTree(dir) {
                include("**/*.class")
                exclude(
                    excludedClasses
                )
            }
        }
    )
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)

    val allClassesDirs = layout.buildDirectory.dir("classes")

    classDirectories.setFrom(
        allClassesDirs.map { dir ->
            fileTree(dir) {
                include("**/*.class")
                exclude(
                    excludedClasses
                )
            }
        }
    )

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