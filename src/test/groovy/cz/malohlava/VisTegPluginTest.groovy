package cz.malohlava

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class VisTegPluginTest {

    @Test
    void testPluginApplication() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'cz.malohlava.visteg'

        ((ProjectInternal) project).evaluate()
        println (project.path)
    }
}