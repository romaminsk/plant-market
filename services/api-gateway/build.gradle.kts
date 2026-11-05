plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":platform-common"))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway.server.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jackson)
    implementation(libs.spring.boot.starter.security.oauth2.resource.server)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux.test)
    testImplementation(libs.spring.boot.starter.restclient)
}
