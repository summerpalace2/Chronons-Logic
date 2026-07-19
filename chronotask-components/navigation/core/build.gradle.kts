import com.chronotask.buildlogic.convention.useCompose
import com.chronotask.buildlogic.convention.useNav

plugins {
    alias(libs.plugins.chronotask.library)
}
useCompose(includeBase = false)
useNav(includeBase = false)
