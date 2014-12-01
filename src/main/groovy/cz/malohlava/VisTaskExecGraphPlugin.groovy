package cz.malohlava

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.execution.taskgraph.TaskDependencyGraph
import org.gradle.execution.taskgraph.TaskExecutionPlan
import org.gradle.execution.taskgraph.TaskInfo

import java.lang.reflect.Field

/**
 * Created by michal on 11/30/14.
 */
class VisTaskExecGraphPlugin implements Plugin<Project> {
    static final Logger LOG = Logging.getLogger(VisTaskExecGraphPlugin.class)

    /** Platform dependent line separator */
    def ls = System.getProperty("line.separator")

    @Override
    void apply(Project project) {
        project.extensions.create("visteg", VisTegPluginExtension)
        if (project.visteg.enabled) {
            project.gradle.taskGraph.whenReady { g ->
                // Access private variables of tasks graph
                def tep = getTEP(g)
                // Execution starts on these tasks
                def entryTasks = getEntryTasks(tep)
                // Already processed edges
                def edges = [] as Set
                // Create output buffer
                def dotGraph = new StringBuilder("digraph compile { ").append(ls)
                entryTasks.each { et ->
                    printGraph(dotGraph, ls, et, edges)
                }
                // Entry tasks nodes have the same priority
                dotGraph.append("{ rank=same; ")
                entryTasks.each { et -> dotGraph.append('"').append(et.getTask().getPath()).append("\" ")}
                dotGraph.append("}").append(ls)
                // Finalize graph
                dotGraph.append("}").append(ls)

                // Save graph
                def outputFile = getDestination(project)
                outputFile.parentFile.mkdirs()
                outputFile.write(dotGraph.toString())

                LOG.info("Dependency graph written into $outputFile")
            }
        }
    }

    private File getDestination(Project p) {
        p.file(p.visteg.destination)
    }

    private TaskExecutionPlan getTEP(TaskExecutionGraph teg) {
        Field f = teg.getClass().getDeclaredField("taskExecutionPlan")
        f.setAccessible(true)
        f.get(teg)
    }

    private Set<TaskInfo> getEntryTasks(TaskExecutionPlan tep) {
        Field f = tep.getClass().getDeclaredField("entryTasks")
        f.setAccessible(true)
        Set<org.gradle.execution.taskgraph.TaskInfo> entryTasks = f.get(tep)
        entryTasks
    }

    private TaskDependencyGraph getTDG(TaskExecutionPlan tep) {
        Field f2 = tep.getClass().getDeclaredField("graph")
        f2.setAccessible(true)
        f2.get(tep)
    }

    StringBuilder printGraph(StringBuilder sb,  String ls, TaskInfo entry, Set<Integer> edges) {
        def q = new LinkedList<TaskInfo>()
        q.add(entry)
        while (!q.isEmpty()) {
            def ti = q.remove()
            def tname = ti.task.path
            def tcolor = getColor(ti.task.project)
            ti.dependencySuccessors.each { succ ->
                def sname = succ.task.path
                if (edges.add( edgeHash(tname, sname) )) {
                    // Generate edge between two nodes
                    sb.append("\"$tname\" -> \"$sname\" [color=\"${tcolor}\"];$ls")
                }
            }
            sb.append("\"$tname\" [shape=box,style=filled,color=\"$tcolor\"];$ls")
            q.addAll(ti.dependencySuccessors)
        }
        sb
    }

    String getColor(Project p) {
        // Generate pastel colors
        // See: http://stackoverflow.com/questions/43044/algorithm-to-randomly-generate-an-aesthetically-pleasing-color-palette
        def l = ( Math.abs(p.path.hashCode() >> 16) % 50) + 50
        def h = ( Math.abs(p.path.hashCode() ^ 0xFFFFFFFF00000000) % 50) + 50
        def m = ( Math.abs(p.path.hashCode() / 13 ) % 50) + 50
        return "0.${m} 0.${h} 0.${l}"
    }

    String edgeHash(String from, String to) {
        return from.hashCode()*37 + to.hashCode()
    }
}

class VisTegPluginExtension {
    boolean enabled = true
    boolean colouredNodes = true
    boolean colouredEdges = true
    def destination = "build/reports/visteg.dot"
}
