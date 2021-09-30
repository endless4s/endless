package endless.core.typeclass.entity

trait EntityNameProvider[ID] extends (() => String)
