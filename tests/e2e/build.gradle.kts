plugins {
    java
}

dependencies {
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.playwright)
    testImplementation(libs.allure.jupiter)
}

tasks.test {
    doFirst {
        delete(layout.buildDirectory.dir("allure-results"))
    }
    systemProperty(
        "allure.results.directory",
        layout.buildDirectory.dir("allure-results").get().asFile.absolutePath,
    )
    environment(
        "E2E_BASE_URL",
        providers.environmentVariable("E2E_BASE_URL").getOrElse(""),
    )
    environment(
        "E2E_ADMIN_URL",
        providers.environmentVariable("E2E_ADMIN_URL").getOrElse(""),
    )
    environment(
        "E2E_ADMIN_EMAIL",
        providers.environmentVariable("E2E_ADMIN_EMAIL").getOrElse("admin@plantmarket.local"),
    )
    environment(
        "E2E_ADMIN_PASSWORD",
        providers.environmentVariable("E2E_ADMIN_PASSWORD").getOrElse("AdminPassword123!"),
    )
}
