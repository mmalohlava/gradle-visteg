package cz.malohlava

import java.lang.reflect.Field

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.execution.plan.ExecutionPlan
import org.gradle.execution.plan.TaskNode

import groovy.transform.Memoized

/**
 * VisTEG
 *
 * <p>Gradle plugin visualizing Gradle task execution graphs.</p>
 *
 * @author Michal Malohlava
 */
class VisTaskExecGraphPlugin implements Plugin<Project> {
    static final Logger LOG = Logging.getLogger(VisTaskExecGraphPlugin.class)

    /** List of supported color schemas - see http://www.graphviz.org/content/color-names . */
    static def SUPPORTED_COLOR_SCHEMAS = [
            accent3:3, accent4:4, accent5:5, accent6:6, accent7:7, accent8:8,
            blues3:3, blues4:4, blues5:5, blues6:6, blues7:7, blues8:8, blues9:9,
            brbg10:10, brbg11:11, brbg3:3, brbg4:4, brbg5:5, brbg6:6, brbg7:7, brbg8:8, brbg9:9,
            bugn3:3, bugn4:4, bugn5:5, bugn6:6, bugn7:7, bugn8:8, bugn9:9,
            bupu3:3, bupu4:4, bupu5:5, bupu6:6, bupu7:7, bupu8:8, bupu9:9,
            dark23:3, dark24:4, dark25:5, dark26:6, dark27:7, dark28:8,
            gnbu3:3, gnbu4:4, gnbu5:5, gnbu6:6, gnbu7:7, gnbu8:8, gnbu9:9,
            greens3:3, greens4:4, greens5:5, greens6:6, greens7:7, greens8:8, greens9:9,
            greys3:3, greys4:4, greys5:5, greys6:6, greys7:7, greys8:8, greys9:9,
            oranges3:3, oranges4:4, oranges5:5, oranges6:6, oranges7:7, oranges8:8, oranges9:9,
            orrd3:3, orrd4:4, orrd5:5, orrd6:6, orrd7:7, orrd8:8, orrd9:9,
            paired10:10, paired11:11, paired12:12, paired3:3, paired4:4, paired5:5, paired6:6, paired7:7, paired8:8, paired9:9,
            pastel13:3, pastel14:4, pastel15:5, pastel16:6, pastel17:7, pastel18:8, pastel19:9,
            pastel23:3, pastel24:4, pastel25:5, pastel26:6, pastel27:7, pastel28:8,
            piyg10:10, piyg11:11, piyg3:3, piyg4:4, piyg5:5, piyg6:6, piyg7:7, piyg8:8, piyg9:9,
            prgn10:10, prgn11:11, prgn3:3, prgn4:4, prgn5:5, prgn6:6, prgn7:7, prgn8:8, prgn9:9,
            pubu3:3, pubu4:4, pubu5:5, pubu6:6, pubu7:7, pubu8:8, pubu9:9,
            pubugn3:3, pubugn4:4, pubugn5:5, pubugn6:6, pubugn7:7, pubugn8:8, pubugn9:9,
            puor10:10, puor11:11, puor3:3, puor4:4, puor5:5, puor6:6, puor7:7, puor8:8, puor9:9,
            purd3:3, purd4:4, purd5:5, purd6:6, purd7:7, purd8:8, purd9:9,
            purples3:3, purples4:4, purples5:5, purples6:6, purples7:7, purples8:8, purples9:9,
            rdbu10:10, rdbu11:11, rdbu3:3, rdbu4:4, rdbu5:5, rdbu6:6, rdbu7:7, rdbu8:8, rdbu9:9,
            rdgy10:10, rdgy11:11, rdgy3:3, rdgy4:4, rdgy5:5, rdgy6:6, rdgy7:7, rdgy8:8, rdgy9:9,
            rdpu3:3, rdpu4:4, rdpu5:5, rdpu6:6, rdpu7:7, rdpu8:8, rdpu9:9,
            rdylbu10:10, rdylbu11:11, rdylbu3:3, rdylbu4:4, rdylbu5:5, rdylbu6:6, rdylbu7:7, rdylbu8:8, rdylbu9:9,
            rdylgn10:10, rdylgn11:11, rdylgn3:3, rdylgn4:4, rdylgn5:5, rdylgn6:6, rdylgn7:7, rdylgn8:8, rdylgn9:9,
            reds3:3, reds4:4, reds5:5, reds6:6, reds7:7, reds8:8, reds9:9,
            set13:3, set14:4, set15:5, set16:6, set17:7, set18:8, set19:9,
            set23:3, set24:4, set25:5, set26:6, set27:7, set28:8,
            set310:10, set311:11, set312:12, set33:3, set34:4, set35:5, set36:6, set37:7, set38:8, set39:9,
            spectral10:10, spectral11:11, spectral3:3, spectral4:4, spectral5:5, spectral6:6, spectral7:7, spectral8:8, spectral9:9,
            ylgn3:3, ylgn4:4, ylgn5:5, ylgn6:6, ylgn7:7, ylgn8:8, ylgn9:9,
            ylgnbu3:3, ylgnbu4:4, ylgnbu5:5, ylgnbu6:6, ylgnbu7:7, ylgnbu8:8, ylgnbu9:9,
            ylorbr3:3, ylorbr4:4, ylorbr5:5, ylorbr6:6, ylorbr7:7, ylorbr8:8, ylorbr9:9,
            ylorrd3:3, ylorrd4:4, ylorrd5:5, ylorrd6:6, ylorrd7:7, ylorrd8:8, ylorrd9:9
    ]

    /** Platform dependent line separator */
    def ls = System.getProperty("line.separator")

    @Override
    void apply(Project project) {
        project.extensions.create("visteg", VisTegPluginExtension)

        project.gradle.taskGraph.whenReady { g ->


            VisTegPluginExtension vistegExt = project.visteg
            // Unify parameters
            if (vistegExt.colorscheme == null ||
                    (vistegExt.colorscheme != 'random' && !SUPPORTED_COLOR_SCHEMAS.containsKey(vistegExt.colorscheme))) {
                LOG.warn("VisTEG colorscheme is not specified - falling back to 'spectral11' color scheme")
                vistegExt.colorscheme = 'spectral11'
            }
            if (vistegExt.enabled) {
                // Access private variables of tasks graph
                def tep = getTEP(g)
                // Execution starts on these tasks
                def entryTasks = getEntryTasks(tep)
                // Already processed edges
                def edges = [] as Set
                // Create output buffer
                def dotGraph = new StringBuilder("digraph compile { ").append(ls)
                if (vistegExt.colorscheme != 'random') {
                    dotGraph.append("colorscheme=${vistegExt.colorscheme};$ls")
                }
                dotGraph.append("rankdir=${vistegExt.rankdir};$ls")
                dotGraph.append("splines=${vistegExt.splines};$ls")
                // Generate graph for each input
                entryTasks.each { et ->
                    printGraph(vistegExt, dotGraph, ls, et, edges)
                }
                // Entry tasks nodes have the same priority
                dotGraph.append("{ rank=same; ")
                entryTasks.each { et -> dotGraph.append('"').append(et.task.path).append("\" ") }
                dotGraph.append("}").append(ls)
                // Finalize graph
                dotGraph.append("}").append(ls)

                // Save graph
                def outputFile = getDestination(project)
                outputFile.parentFile.mkdirs()
                outputFile.write(dotGraph.toString())

                LOG.info("VisTEG: Dependency graph written into $outputFile")
            }
            if (vistegExt.dryRun) {
                dryRun(g)
            }
        }
    }

    private void dryRun(TaskExecutionGraph taskExecutionGraph) {
        taskExecutionGraph.allTasks.each { it.enabled = false }
    }

    private File getDestination(Project p) {
        p.file(p.visteg.destination)
    }

    private ExecutionPlan getTEP(TaskExecutionGraph teg) {
        Field f = teg.class.getDeclaredField("executionPlan")
        f.accessible = true
        f.get(teg)
    }

    private Set<TaskNode> getEntryTasks(ExecutionPlan tep) {
        Field f = tep.class.getDeclaredField("entryTasks")
        f.accessible = true
        Set<TaskNode> entryTasks = f.get(tep)
        entryTasks
    }


    StringBuilder printGraph(VisTegPluginExtension vistegExt,
                             StringBuilder sb, String ls, TaskNode entry, Set<Integer> edges) {
        def q = new LinkedList<TaskNode>()
        def seen = new HashSet<String>()
        boolean colouredNodes = vistegExt.colouredNodes
        boolean colouredEdges = vistegExt.colouredEdges
        String colorscheme = vistegExt.colorscheme == "random" ? null : vistegExt.colorscheme
        q.add(entry)
        while (!q.empty) {
            def ti = q.remove()
            def tproject = ti.task.project
            def tname = ti.task.path

            if (seen.contains(tname)) {
                continue
            }
            seen.add(tname)

            def tcolor = colorscheme == null ? getRandomColor(tproject) : getSchemeColor(tproject, colorscheme, vistegExt.color)
            def nodeKind = ti.dependencyPredecessors.empty ? NodeKind.START
                    : ti.dependencySuccessors.empty ? NodeKind.END : NodeKind.INNER

            ti.dependencySuccessors.each { succ ->
                def sname = succ.task.path
                if (edges.add(edgeHash(tname, sname))) {
                    // Generate edge between two nodes
                    sb.append("\"$tname\" -> \"$sname\"")
                    if (colouredEdges) {
                        sb.append(" [")
                        if (colorscheme != null) sb.append("colorscheme=\"${colorscheme}\",")
                        sb.append("color=${tcolor}]")
                    }
                    sb.append(";").append(ls)
                }
            }
            if (vistegExt.includeMustRunAfter) {
                ti.mustSuccessors.each { succ ->
                    def sname = succ.task.path
                    if (edges.add(edgeHash(tname, sname))) {
                        // Generate edge between two nodes
                        sb.append("\"$tname\" -> \"$sname\"")
                        if (colouredEdges) {
                            sb.append(" [style=dashed,")
                            if (colorscheme != null) sb.append("colorscheme=\"${colorscheme}\",")
                            sb.append("color=${tcolor}]")
                        }
                        sb.append(";").append(ls)
                    }
                }
            }
            if (vistegExt.includeShouldRunAfter) {
                ti.shouldSuccessors.each { succ ->
                    def sname = succ.task.path
                    if (edges.add(edgeHash(tname, sname))) {
                        // Generate edge between two nodes
                        sb.append("\"$tname\" -> \"$sname\"")
                        if (colouredEdges) {
                            sb.append(" [style=dotted,")
                            if (colorscheme != null) sb.append("colorscheme=\"${colorscheme}\",")
                            sb.append("color=${tcolor}]")
                        }
                        sb.append(";").append(ls)
                    }
                }
            }
            sb.append("\"$tname\"")
            sb.append(" [")
            sb.append("shape=\"")
            switch (nodeKind) {
                case NodeKind.START: sb.append(vistegExt.startNodeShape); break
                case NodeKind.INNER: sb.append(vistegExt.nodeShape); break
                case NodeKind.END: sb.append(vistegExt.endNodeShape); break
            }
            sb.append("\",")
            if (colouredNodes) {
                if (colorscheme != null) sb.append("colorscheme=\"${colorscheme}\",")
                sb.append("style=filled,color=$tcolor]")
            }
            sb.append(";").append(ls)
            q.addAll(ti.dependencySuccessors)
        }
        sb
    }

    @Memoized
    int getSchemeColor(Project p, String scheme, int configColor) {
        int schemeLen = SUPPORTED_COLOR_SCHEMAS[scheme]
        if (configColor == -1) {
            return Math.abs(p.hashCode() % schemeLen) + 1 // schemas are 1-based
        } else {
            return Math.min(configColor, schemeLen)
        }
    }

    String getRandomColor(Project p) {
        // Generate pastel colors
        // See: http://stackoverflow.com/questions/43044/algorithm-to-randomly-generate-an-aesthetically-pleasing-color-palette
        def l = (Math.abs(p.path.hashCode() >> 16) % 50) + 50
        def h = (Math.abs(p.path.hashCode() ^ 0xFFFFFFFF00000000) % 50) + 50
        def m = (Math.abs(p.path.hashCode() / 13) % 50) + 50
        return "\"0.${m} 0.${h} 0.${l}\""
    }

    int edgeHash(String from, String to) {
        return from.hashCode() * 37 + to.hashCode()
    }

    enum NodeKind {
        START, INNER, END
    }
}

class VisTegPluginExtension {
    /** Enables the plugin for given project */
    boolean enabled = true
    /** Produces coloured edges */
    boolean colouredNodes = true
    /** Produces coloured nodes */
    boolean colouredEdges = true
    /** Includes task ordering info mustRunAfter*/
    boolean includeMustRunAfter = true
    /** Includes task ordering info shouldRunAfter*/
    boolean includeShouldRunAfter = true
    /** Gradle 4.+ does not call taskgraph.whenReady anymore if --dry-run is used, so set all tasks to enabled=false*/
    boolean dryRun = false
    /** Output file destination file */
    String destination = 'build/reports/visteg.dot'
    String exporter = 'dot'
    /** Name of used color scheme - see {@link "http://www.graphviz.org/content/color-names"} for possible values. */
    String colorscheme = 'spectral11'
    /** Force node color, use -1 for semi-random generation*/
    int color = -1
    /** Shape of inner node - see {@link "http://www.graphviz.org/content/node-shapes"} for possible values. */
    String nodeShape = 'box'
    /** Shape of start node - see {@link "http://www.graphviz.org/content/node-shapes"} for possible values. */
    String startNodeShape = 'hexagon'
    /** Shape of end node - see {@link "http://www.graphviz.org/content/node-shapes"} for possible values. */
    String endNodeShape = 'doubleoctagon'
    /** Sets direction of graph layout. {@link "http://www.graphviz.org/doc/info/attrs.html#a:rankdir"}  for possible values.*/
    String rankdir = 'TB'
    /** Controls how, and if, edges are represented. {@link "http://www.graphviz.org/doc/info/attrs.html#a:splines"}  for possible values.*/
    String splines = 'spline'
}
