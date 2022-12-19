package com.github.lipen.circuition.blif

data class Blif(
    val models: List<Model>,
)

data class Model(
    val name: String,
    val inputs: List<String>,
    val outputs: List<String>,
    // val clocks: List<String>,
    val commands: List<Command>,
) {
    override fun toString(): String {
        return "Model(name: '$name', inputs: ${inputs.size}, outputs: ${outputs.size}, commands: ${commands.size})"
    }
}

sealed interface Command

// ".names"
data class LogicGate(
    val inputs: List<String>,
    val output: String,
    val pla: List<Row>, // PLA table
) : Command {
    init {
        require(pla.all { it.inputs.size == pla.first().inputs.size })
    }
}

enum class Tri {
    // '1', '0', '-', respectively
    True, False, DontCare;

    companion object {
        fun from(char: Char): Tri {
            return when (char) {
                '1' -> True
                '0' -> False
                '-' -> DontCare
                else -> error("Bad char '$char'")
            }
        }
    }
}

// e.g., row="01-0 1": inputs=[True,False,DC,False], output=True
data class Row(
    val inputs: List<Tri>,
    val output: Boolean,
)

// ".gate"
data class LibraryGate(
    val name: String,
    val mapping: Map<String, String>, // "formal-actual-list"
) : Command
