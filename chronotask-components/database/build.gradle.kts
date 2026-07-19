import com.chronotask.buildlogic.convention.useCompose
import com.chronotask.buildlogic.convention.useNav
import com.chronotask.buildlogic.convention.useDB

plugins {
    alias(libs.plugins.chronotask.library)
}
useDB()
useNav()
useCompose()
