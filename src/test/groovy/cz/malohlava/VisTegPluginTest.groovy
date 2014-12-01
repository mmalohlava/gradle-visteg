package cz.malohlava

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class VisTegPluginTest {

    @Test
    public void testPluginApplication() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'cz.malohlava.visteg'

        project.exec {}
        println (project.path)
    }
}