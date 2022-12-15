package com.github.lipen.circuition.aig

import com.github.lipen.circuition.isOdd

data class Ref(
    val id: Int,
    val negated: Boolean,
) {
    init {
        require(id > 0)
    }

    override fun toString(): String {
        return "${if (negated) "~" else ""}@$id"
    }

    fun toInt(): Int {
        return if (negated) -id else id
    }
}
