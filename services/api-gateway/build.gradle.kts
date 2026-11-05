plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":platform-common"))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway.server.webflux)
    implementation(libs.spring.boot.starter.actuator)

    testImplementation(libs.spring.boot.starter.test)
}
