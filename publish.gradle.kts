// publish.gradle.kts — 共享 Maven Central 发布配置
// 用法：apply(from = "../publish.gradle.kts")

plugins {
    id("maven-publish")
    id("signing")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.morainet.widget"
                artifactId = project.name
                version = "0.1.0"

                pom {
                    name.set("Morainet Widget Kit - ${project.name}")
                    description.set("Android Widget development toolkit on top of Jetpack Glance — state, scheduling, animation, preview, debug & DSL.")
                    url.set("https://github.com/morainet/widget-kit")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("morainet")
                            name.set("Morainet Team")
                            email.set("dev@morainet.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/morainet/widget-kit.git")
                        developerConnection.set("scm:git:ssh://github.com/morainet/widget-kit.git")
                        url.set("https://github.com/morainet/widget-kit")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "local"
                url = uri("${rootProject.layout.buildDirectory.get().asFile}/maven-repo")
            }
            // Maven Central via Sonatype OSSRH
            // 发布时需要环境变量：OSSRH_USERNAME, OSSRH_PASSWORD
            maven {
                name = "sonatype"
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME") ?: ""
                    password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD") ?: ""
                }
            }
        }
    }

    signing {
        val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
        val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")
        if (!signingKey.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        setRequired { gradle.taskGraph.hasTask("publishSonatypePublicationToSonatypeRepository") }
    }
}
