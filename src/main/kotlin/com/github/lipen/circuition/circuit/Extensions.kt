package com.github.lipen.circuition.circuit

fun findRelevantGates(circuit: Circuit): List<Node> {
    val relevant : MutableList<String> = mutableListOf()

    for (layer in circuit.layers) {
        relevant.addAll(layer)
    }

    return relevant.map { circuit.node(it) }
}
