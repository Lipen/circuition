package com.github.lipen.circuition

import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan
import okio.BufferedSink
import okio.BufferedSource

internal fun timeNow(): TimeSpan = PerformanceCounter.reference
internal fun timeSince(timeStart: TimeSpan): TimeSpan = timeNow() - timeStart
internal fun secondsSince(timeStart: TimeSpan): Double = timeSince(timeStart).seconds

internal fun isEven(i: Int): Boolean = (i and 1) == 0
internal fun isOdd(i: Int): Boolean = (i and 1) != 0

internal fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun Iterable<Boolean>.toBinaryString(): String = joinToString("") { if (it) "1" else "0" }

/** Returns the [i]-th bit (0-based, LSB-to-MSB order) of the number. */
internal fun Int.bit(i: Int): Boolean = (this and (1 shl i)) != 0
internal fun Long.bit(i: Int): Boolean = (this and (1L shl i)) != 0L

internal fun BufferedSource.lineSequence(): Sequence<String> =
    sequence { while (true) yield(readUtf8Line() ?: break) }.constrainOnce()

internal fun BufferedSink.write(s: String): BufferedSink = writeUtf8(s)
internal fun BufferedSink.writeln(s: String): BufferedSink = write(s).writeByte(10) // 10 is '\n'

internal inline fun <T : AutoCloseable?, R> T.useWith(block: T.() -> R): R = use(block)
