package endless.core.typeclass.entity

import cats.Monad
import endless.core.typeclass.event.EventWriter

trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E] with Monad[F]
