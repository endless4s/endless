package endless.core.interpret

import endless.core.typeclass.entity.Entity

/** Any effect of type G[A] can be lifted into F[A] thus enabling entity capability - we typically
  * lift EntityT for interpretation
  */
trait EntityLift[G[_], F[_], S, E] extends Entity[G, S, E]
