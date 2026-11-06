plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":platform-common"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.flyway.core)
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.restclient)
    testImplementation(libs.testcontainers.postgresql)
}
