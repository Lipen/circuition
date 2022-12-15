package com.github.lipen.circuition.logic

sealed interface Logic {
    fun eval(mapping: Map<Variable, Boolean>): Boolean
    fun toPrettyString(): String
}

data class Variable(
    val name: String,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return mapping.getValue(this)
    }

    override fun toPrettyString(): String {
        return name
    }
}

sealed class Constant : Logic

object True : Constant() {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean = true
    override fun toPrettyString(): String = "⊤"
}

object False : Constant() {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean = false
    override fun toPrettyString(): String = "⊥"
}

data class Not(
    val arg: Logic,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return !arg.eval(mapping)
    }

    override fun toPrettyString(): String {
        val s = arg.toPrettyStringMaybeEmbraced()
        return "~$s"
    }
}

data class And(
    val lhs: Logic,
    val rhs: Logic,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return lhs.eval(mapping) && rhs.eval(mapping)
    }

    override fun toPrettyString(): String {
        val left = lhs.toPrettyStringMaybeEmbraced()
        val right = lhs.toPrettyStringMaybeEmbraced()
        return "$left & $right"
    }
}

data class Or(
    val lhs: Logic,
    val rhs: Logic,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return lhs.eval(mapping) || rhs.eval(mapping)
    }

    override fun toPrettyString(): String {
        val left = lhs.toPrettyStringMaybeEmbraced()
        val right = lhs.toPrettyStringMaybeEmbraced()
        return "$left | $right"
    }
}

data class Imply(
    val lhs: Logic,
    val rhs: Logic,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return !lhs.eval(mapping) || rhs.eval(mapping)
    }

    override fun toPrettyString(): String {
        val left = lhs.toPrettyStringMaybeEmbraced()
        val right = rhs.toPrettyStringMaybeEmbraced()
        return "$left => $right"
    }
}

data class Iff(
    val lhs: Logic,
    val rhs: Logic,
) : Logic {
    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return lhs.eval(mapping) == rhs.eval(mapping)
    }

    override fun toPrettyString(): String {
        val left = lhs.toPrettyStringMaybeEmbraced()
        val right = rhs.toPrettyStringMaybeEmbraced()
        return "$left <=> $right"
    }
}

data class Operation(
    val type: Type,
    val args: List<Logic>,
) : Logic {
    enum class Type {
        AND, OR
    }

    override fun eval(mapping: Map<Variable, Boolean>): Boolean {
        return when (type) {
            Type.AND -> args.all { it.eval(mapping) }
            Type.OR -> args.any { it.eval(mapping) }
        }
    }

    override fun toPrettyString(): String {
        val symbol = when (type) {
            Type.AND -> "&"
            Type.OR -> "|"
        }
        return args.joinToString(" $symbol ") { it.toPrettyStringMaybeEmbraced() }
    }

    companion object {
        fun and(args: List<Logic>): Operation {
            return Operation(Type.AND, args)
        }

        fun and(vararg args: Logic): Operation {
            return and(args.asList())
        }

        fun or(args: List<Logic>): Operation {
            return Operation(Type.OR, args)
        }

        fun or(vararg args: Logic): Operation {
            return or(args.asList())
        }
    }
}

private fun Logic.toPrettyStringMaybeEmbraced(): String = when (this) {
    is Constant, is Variable, is Not -> toPrettyString()
    else -> "(${toPrettyString()})"
}

operator fun Logic.not(): Not = Not(this)
infix fun Logic.and(rhs: Logic): And = And(this, rhs)
infix fun Logic.or(rhs: Logic): Or = Or(this, rhs)
infix fun Logic.imply(rhs: Logic): Imply = Imply(this, rhs)
infix fun Logic.iff(rhs: Logic): Iff = Iff(this, rhs)
