package akka.persistence.tagless.core.interpret

import akka.persistence.tagless.core.typeclass.Entity

/** Any effect of type G[A] can be lifted into F[A] thus enabling entity capability - we typically
  * lift EntityT for interpretation
  */
trait EntityLift[G[_], F[_], S, E] extends Entity[G, S, E] {
  def liftF[A](fa: F[A]): G[A]
}
