import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
 kotlin("jvm") version "2.0.21"
 id("org.jetbrains.compose") version "1.7.3"
 id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}
kotlin { jvmToolchain(21) }
dependencies {
 implementation(compose.desktop.currentOs)
 implementation(compose.material3)
 implementation(compose.materialIconsExtended)
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
 implementation("org.json:json:20240303")
 implementation("me.friwi:jcefmaven:152.0.6")
 implementation("com.google.code.gson:gson:2.11.0")
 implementation("uk.co.caprica:vlcj:4.8.3")
 implementation("net.jthink:jaudiotagger:3.0.1")
}
compose.desktop {
 application {
  mainClass = "com.lormusic.desktop.MainKt"
  jvmArgs += listOf("-Dfile.encoding=UTF-8")
  nativeDistributions {
   targetFormats(TargetFormat.Deb)
   packageName = "lormusic"
   packageVersion = "1.8.3"
   description = "LorMusic — Material 3 Expressive music player"
   vendor = "LorMusic"
   modules("java.sql", "java.net.http", "jdk.unsupported", "java.naming", "java.management", "jdk.charsets", "jdk.zipfs", "java.prefs", "java.security.jgss", "java.rmi")
   linux { debMaintainer = "LorMusic <lormusic@localhost>"; menuGroup = "AudioVideo"; appCategory = "AudioVideo;Audio;Player;"; iconFile.set(project.file("packaging/lormusic.png")) }
   windows { menu = true; shortcut = true; dirChooser = true }
  }
 }
}
tasks.register("exportRuntime") {
 dependsOn("jar")
 doLast {
  val out=layout.buildDirectory.dir("desktop-runtime").get().asFile
  out.deleteRecursively(); out.mkdirs()
  copy { from(configurations.runtimeClasspath); into(out) }
  copy { from(tasks.named<Jar>("jar").get().archiveFile); into(out) }
 }
}
