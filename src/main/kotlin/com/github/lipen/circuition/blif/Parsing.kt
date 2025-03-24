@file:Suppress("PrivatePropertyName")

package com.github.lipen.circuition.blif

import com.github.lipen.circuition.lineSequence
import com.github.lipen.circuition.useWith
import mu.KotlinLogging
import okio.buffer
import okio.source
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

fun parseBlif(
    path: Path,
    defaultName: String = path.nameWithoutExtension,
): Blif {
    logger.debug { "Parsing BLIF from '$path'" }

    path.source().buffer().useWith {
        val lines = lineSequence()
            .map { it.substringBefore('#') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .let { mergeBackslash(it) }
            .iterator()

        val models = parseModels(lines, defaultName).toList()
        logger.debug { "Total models: ${models.size}" }

        return Blif(models)
    }
}

private fun mergeBackslash(lines: Sequence<String>): Sequence<String> = sequence {
    @Suppress("NAME_SHADOWING")
    val lines = lines.iterator()
    var line: String

    while (lines.hasNext()) {
        line = lines.next()
        while (line.endsWith('\\')) {
            check(line.startsWith('.')) {
                "Non-keyword line ends with a backslash"
            }
            check(lines.hasNext()) {
                "Iterator of lines is empty after a backslash"
            }
            val next = lines.next()
            if (next.startsWith('.')) {
                error("Keyword line after backslash")
            }
            // Cut the last char ('\\') and append the next line:
            line = line.substring(0, line.length - 1) + " " + next
        }
        yield(line)
    }
}

private const val IDENT = """[a-zA-Z_][\w.\[\]]*"""
private val RE_SPACE = Regex("\\s+")
private val RE_MODEL = Regex("""\.model\s+(?<name>[\w.]+)""")
private val RE_INPUTS = Regex("""\.inputs(?<inputs>(?:\s+$IDENT)*)""")
private val RE_OUTPUTS = Regex("""\.outputs(?<outputs>(?:\s+$IDENT)*)""")
private val RE_LOGIC_GATE = Regex("""\.names(?<ids>(?:\s+$IDENT)+)""")
private val RE_PLA_ROW = Regex("""(?<inputs>[01-]*)\s+(?<output>[01]+)""")
private val RE_LIBRARY_GATE = Regex("""\.gate\s+(?<name>$IDENT)\s+(?<mappings>(?:\s+$IDENT=$IDENT)*)""")

private fun parseModels(
    lines: Iterator<String>,
    defaultName: String,
): Sequence<Model> = sequence {
    var flagModel = false
    var modelName = ""
    val modelInputs: MutableList<String> = mutableListOf()
    val modelOutputs: MutableList<String> = mutableListOf()
    val modelCommands: MutableList<Command> = mutableListOf()

    lateinit var line: String
    var state = "READY"

    while (lines.hasNext()) {
        when (state) {
            "READY" -> {
                line = lines.next()
                if (line.startsWith('.')) {
                    state = "KEYWORD"
                } else {
                    error("Expected a line starting from '.'")
                }
            }

            "KEYWORD" -> {
                if (line.startsWith(".model")) {
                    if (flagModel) {
                        logger.debug { "Encountered a new model without model end" }

                        // Yield a model
                        if (modelName == "") {
                            logger.debug { "Using default model name: '$defaultName'" }
                            modelName = defaultName
                        }
                        val model = Model(modelName, modelInputs, modelOutputs, modelCommands)
                        yield(model)
                    }
                    state = "PARSING_MODEL"
                } else if (line.startsWith(".end")) {
                    if (!flagModel) {
                        error("Encountered model end without model header")
                    }
                    state = "PARSING_END"
                } else if (line.startsWith(".inputs")) {
                    if (!flagModel) {
                        logger.debug { "Encountered model inputs without model header" }
                        flagModel = true
                    }
                    state = "PARSING_INPUTS"
                } else if (line.startsWith(".outputs")) {
                    if (!flagModel) {
                        logger.debug { "Encountered model outputs without model header" }
                        flagModel = true
                    }
                    state = "PARSING_OUTPUTS"
                } else if (line.startsWith(".names")) {
                    if (!flagModel) {
                        logger.debug { "Encountered logic gate without model header" }
                        flagModel = true
                    }
                    state = "PARSING_LOGIC_GATE"
                } else if (line.startsWith(".latch")) {
                    if (!flagModel) {
                        logger.debug { "Encountered latch without model header" }
                        flagModel = true
                    }
                    state = "PARSING_LATCH"
                } else if (line.startsWith(".gate")) {
                    if (!flagModel) {
                        logger.debug { "Encountered library gate without model header" }
                        flagModel = true
                    }
                    state = "PARSING_LIBRARY_GATE"
                } else if (line.startsWith(".mlatch")) {
                    if (!flagModel) {
                        logger.debug { "Encountered library latch without model header" }
                        flagModel = true
                    }
                    state = "PARSING_LIBRARY_LATCH"
                } else {
                    error("Could not parse '$line'")
                }
            }

            "PARSING_MODEL" -> {
                val name = run {
                    val m = RE_MODEL.matchEntire(line)
                    checkNotNull(m) {
                        "Could not parse model name from '$line'"
                    }
                    m.groups["name"]!!.value
                }

                // Reset a model
                flagModel = true
                modelName = name
                modelInputs.clear()
                modelOutputs.clear()
                modelCommands.clear()

                state = "READY"
            }

            "PARSING_END" -> {
                check(line == ".end") {
                    "Could not parse model end from line '$line'"
                }

                // Yield a model
                if (modelName == "") {
                    logger.debug { "Using default model name: '$defaultName'" }
                    modelName = defaultName
                }
                val model = Model(modelName, modelInputs, modelOutputs, modelCommands)
                yield(model)

                // Reset a model
                flagModel = false
                modelName = ""
                modelInputs.clear()
                modelOutputs.clear()
                modelCommands.clear()

                state = "READY"
            }

            "PARSING_INPUTS" -> {
                val inputs = run {
                    val m = RE_INPUTS.matchEntire(line)
                    checkNotNull(m) {
                        "Could not parse inputs from '$line'"
                    }

                    val inputs = m.groups["inputs"]!!.value

                    inputs.trim().split(RE_SPACE)
                }

                // Update a model
                modelInputs.addAll(inputs)

                state = "READY"
            }

            "PARSING_OUTPUTS" -> {
                val outputs = run {
                    val m = RE_OUTPUTS.matchEntire(line)
                    checkNotNull(m) {
                        "Could not parse outputs from '$line'"
                    }

                    val outputs = m.groups["outputs"]!!.value

                    outputs.trim().split(RE_SPACE)
                }

                // Update a model
                modelOutputs.addAll(outputs)

                state = "READY"
            }

            "PARSING_LOGIC_GATE" -> {
                val ids = run {
                    val m = RE_LOGIC_GATE.matchEntire(line)
                    checkNotNull(m) {
                        "Could not parse logic gate from '$line'"
                    }

                    val ids = m.groups["ids"]!!.value

                    ids.trim().split(RE_SPACE)
                }
                val inputs = ids.dropLast(1)
                val output = ids.last()

                // Note: update the state BEFORE the loop!
                // Note: in the loop, the state can change to 'KEYWORD'.
                state = "READY"

                val rows: MutableList<Row> = mutableListOf()

                while (lines.hasNext()) {
                    line = lines.next()
                    if (line.startsWith('.')) {
                        state = "KEYWORD"
                        break
                    } else {
                        val row = run {
                            val m = RE_PLA_ROW.matchEntire(line)
                            checkNotNull(m) {
                                "Could not parse logic gate from '$line'"
                            }

                            @Suppress("NAME_SHADOWING")
                            val inputs = m.groups["inputs"]!!.value

                            @Suppress("NAME_SHADOWING")
                            val output = m.groups["output"]!!.value

                            Row(
                                inputs = inputs.map { Tri.from(it) },
                                output = when (output) {
                                    "0" -> false
                                    "1" -> true
                                    else -> error("Bad PLA output '$output'")
                                }
                            )
                        }
                        rows.add(row)
                    }
                }

                // Update a model
                modelCommands.add(LogicGate(inputs, output, rows))

                check(state == "KEYWORD" || state == "READY")
            }

            "PARSING_LATCH" -> {
                TODO()
                state = "READY"
            }

            "PARSING_LIBRARY_GATE" -> {
                val libraryGate = run {
                    val m = RE_LIBRARY_GATE.matchEntire(line)
                    checkNotNull(m) {
                        "Could not parse logic gate from '$line'"
                    }
                    val name = m.groups["name"]!!.value
                    val mappings = m.groups["mappings"]!!.value
                    val mapping = mappings.split(RE_SPACE).associate {
                        val (a, b) = it.split('=', limit = 2)
                        a to b
                    }
                    LibraryGate(name, mapping = mapping)
                }

                // Update a model
                modelCommands.add(libraryGate)

                state = "READY"
            }

            "PARSING_LIBRARY_LATCH" -> {
                TODO()
                state = "READY"
            }

            else -> {
                error("Unknown state '$state'")
            }
        }
    }

    if (flagModel) {
        // Yield a model
        if (modelName == "") {
            logger.debug { "Using default model name: '$defaultName'" }
            modelName = defaultName
        }
        val model = Model(modelName, modelInputs, modelOutputs, modelCommands)
        yield(model)
    }
}
