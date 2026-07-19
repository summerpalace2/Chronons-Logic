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
    implementation(libs.coil.compose)
    implementation(projects.chronotaskPages.notes.api)
}