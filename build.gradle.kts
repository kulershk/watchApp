plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

fun bumpVersion(bumpMajor: Boolean) {
    val file = rootProject.file("version.properties")
    val props = hashMapOf<String, String>()
    file.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
            val (key, value) = trimmed.split("=", limit = 2)
            props[key.trim()] = value.trim()
        }
    }
    val code = props["versionCode"]!!.toInt() + 1
    var major = props["versionMajor"]!!.toInt()
    var minor = props["versionMinor"]!!.toInt()
    if (bumpMajor) {
        major++
        minor = 0
    } else {
        minor++
    }
    file.writeText("versionCode=$code\nversionMajor=$major\nversionMinor=$minor\n")
    println("Version bumped to $major.$minor (code $code)")
}

tasks.register("bumpMinor") {
    group = "versioning"
    description = "Bump minor version (e.g. 1.1 -> 1.2)"
    doLast { bumpVersion(false) }
}

tasks.register("bumpMajor") {
    group = "versioning"
    description = "Bump major version (e.g. 1.1 -> 2.0)"
    doLast { bumpVersion(true) }
}
