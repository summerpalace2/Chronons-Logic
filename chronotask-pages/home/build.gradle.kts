import com.chronotask.buildlogic.convention.useCompose
import com.chronotask.buildlogic.convention.useNav
import com.chronotask.buildlogic.convention.useDB

plugins {
    alias(libs.plugins.chronotask.library)
}
useCompose()
useNav()
useDB()
dependencies {
    implementation(projects.chronotaskPages.home.api)
    implementation(projects.chronotaskPages.create.api)
    implementation(projects.chronotaskPages.stats.api)
    implementation(projects.chronotaskPages.stats)
    implementation(projects.chronotaskPages.settings.api)
    implementation(projects.chronotaskPages.settings)
    implementation(projects.chronotaskPages.taskdetail.api)
    implementation(projects.chronotaskPages.notes)
    implementation(projects.chronotaskPages.notes.api)
    implementation(projects.chronotaskComponents.ui)
}
