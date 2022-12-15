package com.github.lipen.circuition.circuit

import com.github.lipen.circuition.lineSequence
import com.github.lipen.circuition.useWith
import okio.buffer
import okio.source
import java.nio.file.Path
import kotlin.io.path.Path

fun parseBench(path: String): Circuit {
    return parseBench(Path(path))
}

fun parseBench(path: Path): Circuit {
    val inputs: MutableList<Input> = mutableListOf()
    val outputNames: MutableList<String> = mutableListOf()
    val gates: MutableList<Gate> = mutableListOf()

    path.source().buffer().useWith {
        val reInput = Regex("""INPUT\(([\w.]+)\)""")
        val reOutput = Regex("""OUTPUT\(([\w.]+)\)""")
        val reGate = Regex("""([\w.]+) = ([\w.]+)\(([\w.]+(?:, [\w.]+)*)\)""")

        val lines = lineSequence()
            .filter { it.isNotBlank() }
            .map { it.trim() }
        for (line in lines) {
            // Skip comment
            if (line.startsWith("#")) {
                continue
            }

            // Try parse input
            val m1 = reInput.matchEntire(line)
            if (m1 != null) {
                val (name) = m1.destructured
                inputs.add(Input(name))
            } else {
                // Try parse output
                val m2 = reOutput.matchEntire(line)
                if (m2 != null) {
                    val (name) = m2.destructured
                    outputNames.add(name)
                } else {
                    // Try parse gate
                    val m3 = reGate.matchEntire(line)
                    if (m3 != null) {
                        val (name, type, allArgs) = m3.destructured
                        val args = allArgs.split(",").map { it.trim() }
                        gates.add(Gate(name, type.uppercase(), args))
                    } else {
                        error("Could not parse line '$line'")
                    }
                }
            }
        }
    }

    val mapping = inputs.associateBy { it.name } + gates.associateBy { it.name }
    val outputs = outputNames.map { mapping.getValue(it) }

    return Circuit(inputs, outputs, gates, mapping)
}
