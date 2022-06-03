package activator.doodler.extensions

fun <A, B> Pair<A?, B?>.nullable(): Pair<A, B>? {
    val f = first
    val s = second
    return if (f == null || s == null) null else Pair(f, s)
}