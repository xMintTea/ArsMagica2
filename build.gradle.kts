
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val modVersion = providers.gradleProperty("modVersion").getOrElse("1.0.0")

afterEvaluate {
    tasks.named<Jar>("jar") {
        archiveFileName.set("AM2-1.7.10-$modVersion-minted.jar")
    }
}
