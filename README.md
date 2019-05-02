Gradle VisTEG
=============

[![][travis img]][travis]
[![][license img]][license]

[travis]:https://travis-ci.org/mmalohlava/gradle-visteg
[travis img]:https://travis-ci.org/mmalohlava/gradle-visteg.svg?branch=master
[license]:LICENSE
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

# Overview
VisTeg is a Gradle plugin for exporting task execution graph as `.dot` file.


# Configuration

## Apply visteg plugin

### Gradle 1.x and 2.0

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'cz.malohlava:visteg:1.0.4'
    }
}

apply plugin: 'cz.malohlava.visteg'
```

### Gradle 2.1 and higher

```groovy
plugins {
  id 'cz.malohlava.visteg' version '1.0.4'
}
```

## Configure graph generator

The plugin publishes `visteg` extension with following configuration options:
```groovy
visteg {
    enabled        = true
    colouredNodes  = true
    colouredEdges  = true
    destination    = 'build/reports/visteg.dot'
    exporter       = 'dot'
    colorscheme    = 'spectral11'
    nodeShape      = 'box'
    startNodeShape = 'hexagon'
    endNodeShape   = 'doubleoctagon'
}

The plugin supports the following options:
```
 * `enabled` - Enables plugin for the project.
 * `colouredNodes` - Produces colored nodes.
 * `colouredEdges` - Produces colored edges.
 * `destination` - The output file location.
 * `exporter` - Name of graph exporter, only `dot` value is supported now.
 * `colorscheme` - Name of color scheme used for graph coloring. For full list of values see [Graphviz page](http://www.graphviz.org/content/color-names).
 * `nodeShape` - Name of shape used for graph inner nodes. See [Graphviz page](http://www.graphviz.org/content/node-shapes) for full list of values.
 * `startNodeShape` - Name of shape used for graph start nodes. See [Graphviz page](http://www.graphviz.org/content/node-shapes) for full list of values.
 * `endNodeShape` - Name of shape used for graph leaf nodes. See [Graphviz page](http://www.graphviz.org/content/node-shapes) for full list of values.

## Graph generation
Perform any Gradle task, for example `build`:
```
./gradlew build
```

It will generate a `.dot` file containing graph description `build/reports/visteg.dot`.

## Image generation
The generated file can be post-processed via [Graphviz](http://www.graphviz.org) `dot` utility.

For example, png image is produced as follows:
```
cd build/reports/
dot -Tpng ./visteg.dot -o ./visteg.dot.png
```

For more information, please visit [Graphviz home page](http://www.graphviz.org).

# Design
The plugin installs itself as a listener to Gradle lifecycle via `gradle.taskGraph.whenReady`. During execution it obtains reference to task execution graph via reflection and performs a walk through the graph.


# Acknowledgements
Based on idea published by Code Wader - http://codewader.blogspot.com/2011/11/show-gradle-dependencies-as-graphwiz.html




