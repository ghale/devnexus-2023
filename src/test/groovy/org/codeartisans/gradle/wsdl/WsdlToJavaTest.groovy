package org.codeartisans.gradle.wsdl

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class WsdlToJavaTest extends Specification {
    private static final String[] GRADLE_VERSIONS = ['7.6.1', '8.0.2']

    @TempDir
    File testProjectDir
    @TempDir
    File alternateProjectDir
    File buildFile

    def setup() {
        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name = 'test-project'
            buildCache {
                local {
                    directory = new File(rootDir, 'build-cache')
                }
            }
        """.stripIndent()

        buildFile = new File(testProjectDir, 'build.gradle') << """
            import de.undercouch.gradle.tasks.download.Download
            import org.codeartisans.gradle.wsdl.*
            
            plugins {
              id "java"
              id "de.undercouch.download" version "5.4.0"
              id "org.codeartisans.gradle.wsdl-tasks"
            }
      
            repositories {
              mavenCentral()
            }
            
            tasks.register("downloadWsdl", Download) {
              src "https://raw.githubusercontent.com/arktekk/sbt-cxf-example/master/src/main/wsdl/PingPong.wsdl"
              dest "\$buildDir/wsdls/PingPong.wsdl"
            }

            tasks.named('wsdlToJava') {
              dependsOn downloadWsdl
              wsdls {
                pingPong {
                  wsdl = file(downloadWsdl.dest)
                  packageName = 'ping.pong'
                }
              }
            }
        """.stripIndent()
    }

    def "functional: generates and compiles sources for a single ping-pong wsdl"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('build', '--stacktrace')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        and:
        new File(testProjectDir, "build/generated-sources/wsdlToJava/java/ping/pong/PingPongService.java").isFile()
        new File(testProjectDir, "build/classes/java/main/ping/pong/PingPong.class").isFile()

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "task avoidance: does not create wsdl tasks when none are requested"() {
        buildFile << """
            tasks.configureEach { task ->
                println "CREATED: \${task.name}"
            }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('help')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.output.contains("CREATED: help")
        !result.output.contains("CREATED: wsdlToJava")

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "task avoidance: only creates the wsdl tasks that are requested"() {
        buildFile << """
            tasks.register('anotherWsdlToJava', WsdlToJava) {
              dependsOn downloadWsdl
              wsdls {
                pingPong {
                  wsdl = file(downloadWsdl.dest)
                  packageName = 'ping.pong'
                }
              }
            }
            
            tasks.configureEach { task ->
                println "CREATED: \${task.name}"
            }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.output.contains("CREATED: wsdlToJava")
        !result.output.contains("CREATED: anotherWsdlToJava")

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "configuration cache: reuses configuration cache when build is not changed"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace', '--configuration-cache')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        when:
        result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace', '--configuration-cache')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.UP_TO_DATE

        and:
        result.output.contains("Reusing configuration cache.")

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "build cache: reuses outputs when inputs have not changed"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace', '--build-cache', '-Dorg.gradle.caching.debug=true')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        when:
        result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('clean', 'wsdlToJava', '--stacktrace', '--build-cache', '-Dorg.gradle.caching.debug=true')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.FROM_CACHE

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "build cache: relocated build reuses outputs when inputs have not changed"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace', '--build-cache', '-Dorg.gradle.caching.debug=true')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        when:
        FileUtils.copyDirectory(testProjectDir, alternateProjectDir)
        result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(alternateProjectDir)
                .withArguments('clean', 'wsdlToJava', '--stacktrace', '--build-cache', '-Dorg.gradle.caching.debug=true')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.FROM_CACHE

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    def "build cache: does not reuse outputs when inputs do change"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace', '--build-cache')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        buildFile << """
            tasks.named('wsdlToJava') {
              wsdls {
                pingPong2 {
                  wsdl = file(downloadWsdl.dest)
                  packageName = 'ping.pong2'
                }
              }
            }
        """

        when:
        result = GradleRunner.create()
                .withGradleVersion(gradleVersion as String)
                .withProjectDir(testProjectDir)
                .withArguments('clean', 'wsdlToJava', '--stacktrace', '--build-cache')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << GRADLE_VERSIONS
    }
}