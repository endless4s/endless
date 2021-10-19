package endless.core.typeclass.entity

import cats.Monad
import endless.core.typeclass.event.EventWriter

/** `Entity[F, S, E]` is the ability to read an event-sourced entity state of type `S` and append
  * events of type `E` affecting this state, together with the ability to compose such dependent
  * effectful functions.
  *
  * These dual reader/writer abilities are what is needed to describe command handler behavior. When
  * interpreting code involving `Entity`, the final resulting value in the monadic chain is
  * typically understood as the reply, and all appended events are persisted to the journal
  * atomically by the runtime. Read always provides the up-to-date state however, thanks to event
  * folding happening within the interpreter, ensuring consistency throughout the chain.
  *
  * @tparam F
  *   context
  * @tparam S
  *   state
  * @tparam E
  *   event
  */
trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E] with Monad[F]
