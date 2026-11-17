package com.sigha.converter.core

import java.util.PriorityQueue

/**
 * Fewest-hop converter chain from one format to another (uniform edge cost, <= maxHops).
 * Mirrors the idea proven by Filee's RoutePlanner: never hard-code recipes, let the graph
 * discover chains such as DOCX -> PDF -> JPG.
 *
 * Deterministic: equal-cost nodes are ordered by format id, so the chosen chain never
 * depends on hash iteration order.
 *
 * Multi-output converters are refused except as the final hop: a mid-route step that
 * writes several files would lose all but the first.
 */
class RoutePlanner(private val registry: ConverterRegistry) {

    fun plan(from: FileFormat, to: FileFormat, maxHops: Int = 3): List<Converter>? {
        if (from.id == to.id) return emptyList()
        data class Node(val formatId: String, val cost: Int)

        val best = HashMap<String, Int>()
        val previous = HashMap<String, Pair<String, Converter>>()
        val queue = PriorityQueue(compareBy<Node> { it.cost }.thenBy { it.formatId })
        best[from.id] = 0
        queue += Node(from.id, 0)

        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue
            if (node.cost > (best[node.formatId] ?: Int.MAX_VALUE)) continue
            if (node.formatId == to.id) break
            if (node.cost >= maxHops) continue
            val format = registry.format(node.formatId) ?: continue
            for (edge in registry.edgesFrom(format)) {
                // A multi-file converter is only usable as the final step.
                if (edge.converter.producesMultipleOutputs && edge.target.id != to.id) continue
                val nextCost = node.cost + 1
                if (nextCost < (best[edge.target.id] ?: Int.MAX_VALUE)) {
                    best[edge.target.id] = nextCost
                    previous[edge.target.id] = node.formatId to edge.converter
                    queue += Node(edge.target.id, nextCost)
                }
            }
        }

        if (!best.containsKey(to.id)) return null
        val route = ArrayList<Converter>()
        var cursor = to.id
        while (cursor != from.id) {
            val (parent, converter) = previous[cursor] ?: return null
            route += converter
            cursor = parent
        }
        return route.asReversed()
    }
}
