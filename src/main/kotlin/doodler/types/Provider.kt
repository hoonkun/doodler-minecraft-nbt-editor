package doodler.types

typealias Provider<T> = () -> T
typealias BooleanProvider = () -> Boolean

val TrueProvider: BooleanProvider = { true }
val FalseProvider: BooleanProvider = { false }

val EmptyLambda = {  }
