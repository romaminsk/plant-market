import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter)
    api(libs.spring.web)
    api(libs.spring.boot.starter.jackson)
    api(libs.jakarta.validation.api)
    compileOnly(libs.jakarta.servlet.api)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.validation)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.jakarta.servlet.api)
}


val pitest = configurations.create("pitest")

dependencies {
    pitest("org.pitest:pitest-command-line:1.30.2")
}

tasks.register<JavaExec>("mutationTest") {
    group = "verification"
    description = "Runs PIT mutation testing for platform-common"
    dependsOn(tasks.named("testClasses"))
    classpath = pitest + sourceSets["test"].runtimeClasspath
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    args(
        "--reportDir",
        layout.buildDirectory.dir("reports/pitest").get().asFile.absolutePath,
        "--targetClasses",
        "com.plantmarket.common.id.UuidV7,com.plantmarket.common.api.PageResponse,com.plantmarket.common.error.ApiException,com.plantmarket.common.error.FieldViolation",
        "--targetTests",
        "com.plantmarket.common.id.UuidV7MutationTest,com.plantmarket.common.api.PageResponseMutationTest,com.plantmarket.common.error.ApiExceptionMutationTest",
        "--sourceDirs",
        file("src/main/java").absolutePath,
        "--outputFormats",
        "HTML,XML",
        "--timestampedReports=false",
        "--mutators",
        "DEFAULTS",
        "--verbose",
    )
}
