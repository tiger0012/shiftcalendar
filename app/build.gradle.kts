import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 签名配置从本机 keystore.properties 读取（该文件已加入 .gitignore，不会提交到仓库）。
// 若本机没有该文件，则回退到仓库内默认值，保证 CI/协作者构建不被破坏；
// 正式发布建议删除回退值，改用 CI Secrets 注入。
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { keystoreProps.load(it) }
}
fun signingProp(name: String, default: String): String =
    keystoreProps.getProperty(name)?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 31
        versionName = "2026.11.06"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(signingProp("storeFile", "sc.jks"))
            storePassword = signingProp("storePassword", "Rsddjgq@sc1")
            keyAlias = signingProp("keyAlias", "sc-key-alias")
            keyPassword = signingProp("keyPassword", "Rsddjgq@sc1")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // 使用 getByName 更规范
        }
        // 可以保留 debug 配置（如果需要）
        debug {
            // ...
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    // 更换为Maven Central托管的稳定农历库（如LunarCalendar）
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("cn.6tail:lunar:1.7.7")
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
