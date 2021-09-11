package akka.persistence.tagless.core.typeclass

import cats.Monad

trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E] with Monad[F]
