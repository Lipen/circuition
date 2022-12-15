package com.github.lipen.circuition

import com.github.lipen.circuition.aig.parseAig
import com.github.lipen.circuition.circuit.convertCircuitToDot
import com.github.lipen.circuition.circuit.parseBench
import okio.buffer
import okio.sink
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

fun main() {
    val name = "c17"

    println("Name: $name")

    val circuit = parseBench("data/examples/bench/$name.bench")
    println("circuit = $circuit")
    println("Size: ${circuit.size}")

    val pathGv = "data/examples/gv/$name.gv"
    Path(pathGv).also { it.parent.createDirectories() }.sink().buffer().useWith {
        for (line in convertCircuitToDot(circuit)) {
            writeln(line)
        }
    }
    Runtime.getRuntime().exec("dot -Tpdf -O $pathGv").waitFor()

    println()
    val aig = parseAig("data/examples/aag/$name.aag")
    println("aig = $aig")
}
