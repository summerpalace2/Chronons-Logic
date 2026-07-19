pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://jitpack.io")
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        mavenLocal()
    }
}

rootProject.name = "chronotask"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":chronotask-applications:mobile")

include(":chronotask-components:common")
include(":chronotask-components:database")
include(":chronotask-components:ui")
include(":chronotask-components:navigation:core")
include(":chronotask-components:navigation:processor")

include(":chronotask-pages:home")
include(":chronotask-pages:home:api")
include(":chronotask-pages:create")
include(":chronotask-pages:create:api")
include(":chronotask-pages:stats")
include(":chronotask-pages:stats:api")
include(":chronotask-pages:settings")
include(":chronotask-pages:settings:api")
include(":chronotask-pages:taskdetail")
include(":chronotask-pages:taskdetail:api")
include(":chronotask-pages:notes")
include(":chronotask-pages:notes:api")
