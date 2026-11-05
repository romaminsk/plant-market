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
    testImplementation(libs.jakarta.servlet.api)
}
