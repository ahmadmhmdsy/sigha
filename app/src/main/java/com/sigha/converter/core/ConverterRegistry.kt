package com.sigha.converter.core

import java.util.concurrent.CopyOnWriteArrayList

/** Holds every registered converter edge and answers routing questions. */
class ConverterRegistry(private val formats: FormatRegistry) {
    private val all = CopyOnWriteArrayList<Converter>()
    private val edgesByFrom = HashMap<String, MutableList<Edge>>()

    data class Edge(val converter: Converter, val target: FileFormat)

    fun register(converter: Converter) {
        all += converter
        for (input in converter.inputs) {
            for (output in converter.outputs) {
                if (input.id == output.id) continue
                edgesByFrom.getOrPut(input.id) { mutableListOf() }.add(Edge(converter, output))
            }
        }
    }

    fun converters(): List<Converter> = all.toList()

    fun edgesFrom(format: FileFormat): List<Edge> = edgesByFrom[format.id]?.toList().orEmpty()

    /** Look up a known format by id (used by the router when walking the graph). */
    fun format(id: String): FileFormat? = formats.get(id)

    fun directConverter(from: FileFormat, to: FileFormat): Converter? =
        edgesFrom(from).firstOrNull { it.target.id == to.id }?.converter

    /** All reachable targets from [from] inside [maxHops], for the UI picker. */
    fun reachableTargets(from: FileFormat, maxHops: Int = 3): List<FileFormat> {
        val seen = HashSet<String>()
        val out = LinkedHashMap<String, FileFormat>()
        var frontier = listOf(from)
        repeat(maxHops) {
            val next = mutableListOf<FileFormat>()
            for (format in frontier) {
                for (edge in edgesFrom(format)) {
                    if (seen.add(edge.target.id)) {
                        out[edge.target.id] = edge.target
                        next += edge.target
                    }
                }
            }
            frontier = next
        }
        return out.values.toList()
    }
}
