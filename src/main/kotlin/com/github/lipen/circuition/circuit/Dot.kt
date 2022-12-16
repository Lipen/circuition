package com.github.lipen.circuition.circuit

@Suppress("LocalVariableName")
fun convertCircuitToDot(
    circuit: Circuit,
): Sequence<String> = sequence {
    val STYLE_PI = "shape=invtriangle,color=blue" // Primary Input style
    val STYLE_PO = "shape=triangle,color=blue" // Primary Output style
    val STYLE_INPUT = "shape=box" // Input style
    val STYLE_GATE = "shape=rect" // Gate style
    val STYLE_EDGE = "arrowhead=none" // Edge style

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
            yield("  \"${node.name}\" -> \"$arg\" [$STYLE_EDGE];")
        }
    }

    yield("// Output connections")
    for ((i, node) in circuit.outputs.withIndex()) {
        yield("  o$i -> \"${node.name}\" [$STYLE_EDGE];")
    }

    yield("}")
}
