// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    id("maven-publish")
}

subprojects {
    plugins.apply("maven-publish")
    // Set the group ID if not set in submodules
    group = "eu.anifantakis.lib"
}