package akka.persistence.tagless.core.typeclass

import akka.persistence.tagless.core.interpret.EntityT
import cats.data.ReaderT
import cats.~>

trait Partitioner[F[_], S, E, ID]
    extends (EntityT[F, S, E, *] ~> ReaderT[EntityT[F, S, E, *], ID, *])
