// Top-level build file — 하위 모듈별 설정은 각 모듈의 build.gradle.kts 참조
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
}
