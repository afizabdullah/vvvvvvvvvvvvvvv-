package com.example

class PatternAnalyzer {
    fun generateCards(examples: List<String>, count: Int): List<String> {
        if (examples.isEmpty()) return emptyList()

        val first = examples.first()
        val result = mutableListOf<String>()

        // Find the last numeric part in the string
        val match = Regex("""(\d+)(?!.*\d)""").find(first)
        if (match != null) {
            val numStr = match.value
            val prefix = first.substring(0, match.range.first)
            val suffix = first.substring(match.range.last + 1)

            var currentNum = numStr.toLongOrNull() ?: 0L
            val format = "%0${numStr.length}d"

            for (i in 0 until count) {
                currentNum++
                result.add("$prefix${String.format(format, currentNum)}$suffix")
            }
        } else {
            // Fallback strategy if no digits exist - just append numbers
            for (i in 1..count) {
                result.add("${first}_$i")
            }
        }

        return result
    }

    // Unused, but kept for compatibility logic just in case
    fun analyzePattern(examples: List<String>): Any {
        return Any()
    }
}

