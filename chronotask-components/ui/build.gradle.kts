import com.chronotask.buildlogic.convention.useCompose

plugins {
    alias(libs.plugins.chronotask.library)
}
useCompose(includeBase = false)
