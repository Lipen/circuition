package com.github.lipen.circuition.aig

import com.github.lipen.circuition.secondsSince
import com.github.lipen.circuition.timeNow
import com.github.lipen.circuition.writeln
import com.github.lipen.toposort.toposortLayers
import okio.buffer
import okio.sink
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

private val logger = mu.KotlinLogging.logger {}

class Aig(
    val inputs: List<AigInput>,
    val outputs: List<Ref>,
    val andGates: List<AigAndGate>,
    val mapping: Map<Int, AigNode>, // {id: node}
    // val latches: List<AigLatch>,
    // TODO: val symbolTable...
) {
    init {
        require(inputs.size + andGates.size == mapping.size)
        require(inputs.intersect(andGates).isEmpty())
    }

    val size: Int = mapping.size

    val inputIds: List<Int> = inputs.map { it.id }
    val outputIds: List<Int> = outputs.map { it.id }
    val andGateIds: List<Int> = andGates.map { it.id }

    val layers: List<List<Int>> = toposortLayers(dependencyGraph()).map { it.sorted() }.toList()

    private val parentsTable: Map<Int, List<Ref>> =
        mapping.keys.associateWith { mutableListOf<Ref>() }.also {
            for (layer in layers) {
                for (id in layer) {
                    for (child in children(id)) {
                        it.getValue(child.id).add(Ref(id, child.negated))
                    }
                }
            }
        }

    fun node(id: Int): AigNode = mapping.getValue(id)
    fun input(id: Int): AigInput = node(id) as AigInput
    fun andGate(id: Int): AigAndGate = node(id) as AigAndGate

    fun children(id: Int): List<Ref> = node(id).children
    fun parents(id: Int): List<Ref> = parentsTable.getValue(id)
    fun layerIndex(id: Int): Int = layers.indexOfFirst { it.contains(id) }

    fun dependencyGraph(
        origin: Collection<Int> = outputs.map { it.id },
    ): Map<Int, List<Int>> {
        logger.debug { "Building a dependency graph" }

        val deps: MutableMap<Int, List<Int>> = mutableMapOf()
        val queue = ArrayDeque(origin)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            deps.computeIfAbsent(id) {
                node(id).children.map { it.id }.also {
                    queue.addAll(it)
                }
            }
        }

        return deps
    }

    fun eval(inputValues: List<Boolean>): Map<Int, Boolean> {
        // {id: value}
        val nodeValue: MutableMap<Int, Boolean> = mutableMapOf()

        // Add input values
        for ((i, input) in inputs.withIndex()) {
            nodeValue[input.id] = inputValues[i]
        }

        // Walk by layers and compute gate values
        for ((i, layer) in layers.withIndex()) {
            // 0-th layer contains only inputs
            if (i == 0) continue

            for (id in layer) {
                val node = node(id)
                check(node is AigAndGate)
                val left = nodeValue.getValue(node.left.id) xor node.left.negated
                val right = nodeValue.getValue(node.right.id) xor node.right.negated
                nodeValue[id] = left and right
            }
        }

        check(nodeValue.size == size)
        return nodeValue
        // return outputs.map { nodeValue.getValue(it.id) xor it.negated }
    }

    override fun toString(): String {
        return "Aig(inputs: ${inputs.size}, outputs: ${outputs.size}, ands: ${andGates.size})"
    }
}

fun main() {
    val timeStart = timeNow()

    val filename = "data/examples/aag/and.aag"
    // val filename = "data/examples/aag/halfadder.aag"

    val dumpToDot = false

    val aig = parseAig(filename)
    logger.info { aig }

    if (dumpToDot) {
        val path = Path(filename)
        val pathDot = Path("data/dot", path.nameWithoutExtension + ".dot")
        pathDot.sink().buffer().use {
            println("Dumping AIG to DOT '$pathDot'")
            for (line in convertAigToDot(aig)) {
                it.writeln(line)
            }
        }
        val pathPdf = Path("data/pdf", pathDot.nameWithoutExtension + ".pdf")
        Runtime.getRuntime().exec("dot -Tpdf $pathDot -o $pathPdf").waitFor()
    }

    println("Inputs: ${aig.inputs.size}")
    for (input in aig.inputs) {
        println("  - $input")
    }
    println("Outputs: ${aig.outputs.size}")
    for (output in aig.outputs) {
        println("  - $output")
    }
    println("Gates: ${aig.andGates.size}")
    for (gate in aig.andGates) {
        println("  - $gate")
    }
    println("Layers: ${aig.layers.size}")
    for ((i, layer) in aig.layers.withIndex()) {
        println("  - layer #${i + 1} (${layer.size} nodes): $layer")
    }

    logger.info("All done in %.3fs".format(secondsSince(timeStart)))
}
