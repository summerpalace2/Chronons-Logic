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
    implementation(projects.chronotaskPages.taskdetail.api)
    implementation(projects.chronotaskPages.home.api)
}