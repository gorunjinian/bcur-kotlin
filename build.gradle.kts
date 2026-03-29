plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

group = "com.gorunjinian"
version = "1.0.1"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    jvm()

    // Apple platforms
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Pure Kotlin - no external dependencies
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting
    }
}

android {
    namespace = "com.gorunjinian.bcur"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates("com.gorunjinian", "bcur-kotlin", version.toString())

    pom {
        name.set("bcur-kotlin")
        description.set("Pure Kotlin Multiplatform implementation of BC-UR (Uniform Resources) with fountain codes for animated QR transmission")
        url.set("https://github.com/gorunjinian/bcur-kotlin")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("gorunjinian")
                name.set("gorunjinian")
                url.set("https://gorunjinian.com")
            }
        }

        scm {
            url.set("https://github.com/gorunjinian/bcur-kotlin")
            connection.set("scm:git:git://github.com/gorunjinian/bcur-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/gorunjinian/bcur-kotlin.git")
        }
    }
}
