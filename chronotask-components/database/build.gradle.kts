import com.chronotask.buildlogic.convention.useCompose
import com.chronotask.buildlogic.convention.useNav
import com.chronotask.buildlogic.convention.useDB
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.chronotask.library)
}
useDB()
useNav()
useCompose()

extensions.configure<KspExtension> {
    arg("room.schemaLocation", file("schemas").path)
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("schemas")
    }
}

dependencies {
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
