package com.github.lipen.circuition.circuit

sealed interface Node {
    val name: String
    val args: List<String>
}

data class Gate(
    override val name: String,
    val type: String,
    override val args: List<String>, // [name]
) : Node

data class Input(
    override val name: String,
) : Node {
    override val args: List<String> = emptyList()
}
