plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("org.beryx.jlink") version "3.0.1"
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainModule.set("imageHelper")
    mainClass.set("imageHelper.Main")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls")
}

jlink {

    mergedModule {
        additive = true
    }

    launcher {
        name = "ImageHelper"
    }

    jpackage {
        installerType = "exe"
        imageName = "ImageHelper"
        installerName = "ImageHelper"
    }
}

