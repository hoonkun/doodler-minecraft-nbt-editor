package doodler.extension

fun String.ellipsisStart(maxLength: Int, prefix: String = "...") = this.let {
    if (this.length > maxLength) "$prefix${this.substring(this.length - maxLength, this.length)}"
    else this
}

fun String.ellipsisLast(maxLength: Int, suffix: String = "...") = this.let {
    if (this.length > maxLength) "${this.substring(0, maxLength)}$suffix"
    else this
}

fun String.ellipsisMiddle(maxLength: Int, replacement: String = "...") = this.let {
    if (this.length > maxLength) "${this.substring(0, maxLength / 2 - replacement.length / 2)}$replacement${this.substring(this.length - (maxLength / 2 - replacement.length / 2), this.length)}"
    else this
}