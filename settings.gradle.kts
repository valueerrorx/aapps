pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "aapps"

include(":core:ui")
include(":core:billing")
include(":apps:sleepr")
include(":apps:levlr")
include(":apps:nordr")
include(":apps:spinr")
include(":apps:scorr")
include(":apps:focusr")
include(":apps:roundr")
include(":apps:pulsr")
include(":apps:loudr")
include(":apps:tunr")
include(":apps:magnr")
include(":apps:dozr")
include(":apps:hertzr")
include(":apps:hudr")
include(":apps:moovr")
include(":apps:glidr")
include(":apps:thinkr")
