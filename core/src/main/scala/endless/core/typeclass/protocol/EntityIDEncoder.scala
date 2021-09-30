package endless.core.typeclass.protocol

trait EntityIDEncoder[-ID] extends (ID => String)
