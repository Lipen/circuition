package com.github.lipen.circuition.aig

sealed interface AigNode {
    val id: Int
    val children: List<Ref>
}

data class AigInput(
    override val id: Int,
) : AigNode {
    init {
        require(id > 0)
    }

    override val children: List<Ref> = emptyList()
}

data class AigAndGate(
    override val id: Int,
    val left: Ref,
    val right: Ref,
) : AigNode {
    init {
        require(id > 0)
    }

    override val children: List<Ref> = listOf(left, right)
}
