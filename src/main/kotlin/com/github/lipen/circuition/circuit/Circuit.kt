package com.github.lipen.circuition.circuit

import com.github.lipen.toposort.toposortLayers
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Circuit(
    val inputs: List<Input>,
    val outputs: List<Node>,
    val gates: List<Gate>,
    private val mapping: Map<String, Node>, // {name: node}
) {
    init {
        require(inputs.size + gates.size == mapping.size)
        require(inputs.map { it.name }.intersect(gates.map { it.name }).isEmpty())
    }

    val size: Int = mapping.size

    val layers: List<List<String>> by lazy {
        toposortLayers(dependencyGraph()).map { it.sorted() }.toList()
    }

    fun node(name: String): Node = mapping.getValue(name)
    fun input(name: String): Input = node(name) as Input
    fun gate(name: String): Gate = node(name) as Gate

    fun children(name: String): List<String> = node(name).args
    fun parents(name: String): List<String> = TODO()
    fun layerIndex(name: String): Int = layers.indexOfFirst { it.contains(name) }

    fun dependencyGraph(
        origin: Collection<String> = outputs.map { it.name },
    ): Map<String, List<String>> {
        logger.debug { "Building a dependency graph" }

        val deps: MutableMap<String, List<String>> = mutableMapOf()
        val queue = ArrayDeque(origin)

        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            deps.getOrPut(name) {
                node(name).args.also { queue.addAll(it) }
            }
        }

        return deps
    }

    override fun toString(): String {
        return "Circuit(inputs: ${inputs.size}, outputs: ${outputs.size}, gates: ${gates.size})"
    }
}
