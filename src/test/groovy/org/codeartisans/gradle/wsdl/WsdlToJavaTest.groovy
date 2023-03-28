package org.codeartisans.gradle.wsdl

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class WsdlToJavaTest extends Specification {
    @TempDir
    File testProjectDir
    File buildFile

    def setup() {
        buildFile = new File(testProjectDir, 'build.gradle') << """
            import de.undercouch.gradle.tasks.download.Download
            import org.codeartisans.gradle.wsdl.*
            
            plugins {
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
        """
    }

    def "generates sources for a single ping-pong wsdl"() {
        given:
        buildFile << """
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

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":wsdlToJava").outcome == TaskOutcome.SUCCESS

        and:
        new File(testProjectDir, "build/generated-sources/wsdlToJava/java/ping/pong/PingPongService.java").isFile()
    }

    def "task avoidance: does not create wsdl tasks when none are requested"() {
        buildFile << """
            tasks.named('wsdlToJava') {
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
                .withProjectDir(testProjectDir)
                .withArguments('help')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.output.contains("CREATED: help")
        !result.output.contains("CREATED: wsdlToJava")
    }

    def "task avoidance: only creates the wsdl tasks that are requested"() {
        buildFile << """
            tasks.named('wsdlToJava') {
              dependsOn downloadWsdl
              wsdls {
                pingPong {
                  wsdl = file(downloadWsdl.dest)
                  packageName = 'ping.pong'
                }
              }
            }
            
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
                .withProjectDir(testProjectDir)
                .withArguments('wsdlToJava', '--stacktrace')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.output.contains("CREATED: wsdlToJava")
        !result.output.contains("CREATED: anotherWsdlToJava")
    }
}