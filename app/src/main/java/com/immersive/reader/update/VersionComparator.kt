package com.immersive.reader.update

object VersionComparator {
    fun isNewer(remote: String, current: String): Boolean = compare(remote, current) > 0

    fun compare(left: String, right: String): Int {
        val a = parse(left)
        val b = parse(right)
        val core = compareValuesBy(a, b, { it.major }, { it.minor }, { it.patch })
        if (core != 0) return core
        return when {
            a.prerelease == null && b.prerelease == null -> 0
            a.prerelease == null -> 1
            b.prerelease == null -> -1
            else -> a.prerelease.compareTo(b.prerelease)
        }
    }

    private fun parse(version: String): ParsedVersion {
        var value = version.trim()
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1)
        }
        val dash = value.indexOf('-')
        val core = if (dash >= 0) value.substring(0, dash) else value
        val prerelease = if (dash >= 0) value.substring(dash + 1).ifBlank { null } else null
        val parts = core.split('.')
        return ParsedVersion(
            major = numericPart(parts, 0),
            minor = numericPart(parts, 1),
            patch = numericPart(parts, 2),
            prerelease = prerelease,
        )
    }

    private fun numericPart(parts: List<String>, index: Int): Int {
        val raw = parts.getOrNull(index) ?: return 0
        val digits = raw.takeWhile { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    private data class ParsedVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val prerelease: String?,
    )
}
