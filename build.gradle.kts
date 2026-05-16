

// File: build.gradle.kts (Project: DACS3)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false // Dòng này thay cho dòng id(...) cũ
}