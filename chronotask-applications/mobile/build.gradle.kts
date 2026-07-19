plugins {
    alias(libs.plugins.chronotask.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.chronotask.applications.mobile"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.chronotaskComponents.common)
    implementation(libs.androidx.lifecycle.process)
    implementation(projects.chronotaskComponents.database)
    implementation(projects.chronotaskComponents.ui)
    implementation(projects.chronotaskPages.home)
    implementation(projects.chronotaskPages.home.api)
    implementation(projects.chronotaskPages.create)
    implementation(projects.chronotaskPages.create.api)
    implementation(projects.chronotaskPages.stats)
    implementation(projects.chronotaskPages.stats.api)
    implementation(projects.chronotaskPages.settings)
    implementation(projects.chronotaskPages.settings.api)
    implementation(projects.chronotaskPages.taskdetail)
    implementation(projects.chronotaskPages.taskdetail.api)
}
