package doodler.types

typealias Provider<T> = () -> T
typealias Mapper<T, R> = (T) -> R
typealias BooleanProvider = () -> Boolean

val TrueProvider: BooleanProvider = { true }
val FalseProvider: BooleanProvider = { false }

val EmptyLambda = {  }
