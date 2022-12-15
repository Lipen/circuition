package com.github.lipen.circuition.circuit

@Suppress("LocalVariableName")
fun convertCircuitToDot(
    circuit: Circuit,
    // rankByLayers: Boolean = false,
    // nodeLabel: Map<String, String> = emptyMap(),
    // nodeAddStyle: Map<String, String> = emptyMap(),
): Sequence<String> = sequence {
    val STYLE_PI = "shape=invtriangle,color=blue" // Primary Input style
    val STYLE_PO = "shape=triangle,color=blue" // Primary Output style
    val STYLE_INPUT = "shape=box" // Input style
    val STYLE_GATE = "shape=rect" // Gate style
    val STYLE_EDGE = "arrowhead=none" // Positive edge style

    yield("digraph {")

    yield("// Primary Inputs")
    yield("{ rank=sink")
    for (i in circuit.inputs.indices) {
        yield("  i$i [$STYLE_PI];")
    }
    yield("}")

    yield("// Primary Outputs")
    yield("{ rank=source")
    for (i in circuit.outputs.indices) {
        yield("  o$i [$STYLE_PO];")
    }
    yield("}")

    fun styleFor(node: Node): String {
        val style = when (node) {
            is Input -> STYLE_INPUT
            is Gate -> STYLE_GATE
        }
        return style
        // val label = nodeLabel[node.name]
        // val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
        // val additionalStyle = nodeAddStyle[node.name]
        // val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
        // return "$style$labelS$addStyleS"
    }

    yield("// Inputs")
    yield("{ rank=same")
    for (node in circuit.inputs) {
        val style = styleFor(node)
        yield("  \"${node.name}\" [label=\"IN:${node.name}\",$style]; // $node")
    }
    yield("}")

    yield("// Gates")
    for (node in circuit.gates) {
        val style = styleFor(node)
        yield("  \"${node.name}\" [label=\"${node.type}:${node.name}\",$style]; // $node")
    }

    yield("// Input connections")
    for ((i, node) in circuit.inputs.withIndex()) {
        yield("  \"${node.name}\" -> i$i [$STYLE_EDGE];")
    }

    yield("// Node connections")
    for (node in circuit.gates) {
        for (arg in node.args) {
            val style = STYLE_EDGE
            yield("  \"${node.name}\" -> \"$arg\" [$style];")
        }
    }

    yield("// Output connections")
    for ((i, node) in circuit.outputs.withIndex()) {
        val style = STYLE_EDGE
        yield("  o$i -> \"${node.name}\" [$style];")
    }

    yield("}")
}
