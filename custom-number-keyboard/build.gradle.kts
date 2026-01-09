import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.emsyne.customkeyboard"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
}

/* ---------------------------------------------------
   MAVEN PUBLISH (SAFE & ANDROID-PROOF)
--------------------------------------------------- */
configure<PublishingExtension> {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.emsyne"
            artifactId = "customkeyboard"
            version = "1.0.0"

            // 👇 DIRECTLY PUBLISH THE AAR
            artifact("$buildDir/outputs/aar/custom-number-keyboard-release.aar")
        }
    }

    repositories {
        maven {
            name = "localRepo"
            url = uri("${rootProject.projectDir}/local-repo")
        }
    }
}
