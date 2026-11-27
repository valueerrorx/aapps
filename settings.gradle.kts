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
include(":core:store")
include(":apps:sleepr")
include(":apps:sleepr:shared")
include(":apps:hertzr:shared")
include(":apps:loudr:shared")
include(":apps:levlr")
include(":apps:levlr:shared")
include(":apps:nordr")
include(":apps:nordr:shared")
include(":apps:spinr")
include(":apps:spinr:shared")
include(":apps:scorr")
include(":apps:scorr:shared")
include(":apps:focusr")
include(":apps:focusr:shared")
include(":apps:roundr")
include(":apps:roundr:shared")
include(":apps:pulsr")
include(":apps:pulsr:shared")
include(":apps:loudr")
include(":apps:tunr")
include(":apps:magnr")
include(":apps:magnr:shared")
include(":apps:dozr")
include(":apps:hertzr")
include(":apps:hudr")
include(":apps:hudr:shared")
include(":apps:moovr")
include(":apps:glidr")
include(":apps:thinkr")
include(":apps:thinkr:shared")
include(":apps:dividr")
include(":apps:dividr:shared")
