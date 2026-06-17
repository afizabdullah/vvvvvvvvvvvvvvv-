package com.example

enum class CharType {
    CONSTANT, DIGIT, UPPERCASE, LOWERCASE, MIXED_LETTER, ALPHANUMERIC
}

data class PatternElement(val type: CharType, val constantValue: Char? = null)

class PatternAnalyzer {
    fun analyzePattern(examples: List<String>): List<PatternElement> {
        if (examples.isEmpty()) return emptyList()
        val length = examples[0].length
        if (examples.any { it.length != length }) {
            throw IllegalArgumentException("جميع الأمثلة يجب أن تكون بنفس الطول.")
        }

        val pattern = mutableListOf<PatternElement>()
        for (i in 0 until length) {
            val chars = examples.map { it[i] }
            val c1 = chars[0]

            if (chars.all { it == c1 }) {
                pattern.add(PatternElement(CharType.CONSTANT, c1))
            } else if (chars.all { it.isDigit() }) {
                pattern.add(PatternElement(CharType.DIGIT))
            } else if (chars.all { it.isLetter() && it.isUpperCase() }) {
                pattern.add(PatternElement(CharType.UPPERCASE))
            } else if (chars.all { it.isLetter() && it.isLowerCase() }) {
                pattern.add(PatternElement(CharType.LOWERCASE))
            } else if (chars.all { it.isLetter() }) {
                pattern.add(PatternElement(CharType.MIXED_LETTER))
            } else {
                pattern.add(PatternElement(CharType.ALPHANUMERIC))
            }
        }
        return pattern
    }

    fun generateCards(pattern: List<PatternElement>, count: Int): List<String> {
        val result = mutableListOf<String>()
        val charPoolUpper = ('A'..'Z').toList()
        val charPoolLower = ('a'..'z').toList()
        val digitPool = ('0'..'9').toList()
        val alphaNumPool = charPoolUpper + charPoolLower + digitPool
        val letterPool = charPoolUpper + charPoolLower

        for (i in 0 until count) {
            val sb = StringBuilder()
            for (element in pattern) {
                when (element.type) {
                    CharType.CONSTANT -> sb.append(element.constantValue)
                    CharType.DIGIT -> sb.append(digitPool.random())
                    CharType.UPPERCASE -> sb.append(charPoolUpper.random())
                    CharType.LOWERCASE -> sb.append(charPoolLower.random())
                    CharType.MIXED_LETTER -> sb.append(letterPool.random())
                    CharType.ALPHANUMERIC -> sb.append(alphaNumPool.random())
                }
            }
            result.add(sb.toString())
        }
        return result
    }
}
