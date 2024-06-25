package com.example.audiocapturer.utils

class Text {
}

fun String?.indexesOf(pat: String): List<Int> =
    pat.toRegex()
        .findAll(this?: "")
        .map { it.range.first }
        .toList()