package cz.malohlava

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class VisTegPluginFuncTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()

    File settingsFile
    File buildFile
    File vistegReportFile

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
        vistegReportFile = new File(testProjectDir.root, "build/reports/visteg.dot")
    }

    @Unroll
    def "vis for single task project with Gradle v#gradleVersion"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile << """
            plugins {
                id("cz.malohlava.visteg")
            }

            task helloWorld {
                doLast {
                    println 'Hello world!'
                }
            }
            
            visteg {
                colorscheme = 'puor11'
                color = 4
            }
        """

        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments('helloWorld')
                .withPluginClasspath()
                .build()

        then:
        // Baseline expectation
        result.output.contains('Hello world!')
        result.task(":helloWorld").outcome == SUCCESS
        // Check the report
        vistegReportFile.exists()
        def actOutput = new File(testProjectDir.root, "build/reports/visteg.dot").readLines().join("\n")
        def expOutput = VisTegPluginFuncTest.class.getClassLoader().getResource("single_task_project.dot").readLines().join("\n")
        actOutput == expOutput

        where:
        gradleVersion << ['6.0.1', '6.1.1', '6.4']
    }
}