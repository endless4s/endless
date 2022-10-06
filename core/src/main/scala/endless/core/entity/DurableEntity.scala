package endless.core.entity

/** `DurableEntity[F, S]` is the ability to read and write an entity state of type `S` together with
  * the ability to compose such dependent effectful functions.
  *
  * These dual reader/writer abilities are what is needed to describe command handler behavior. This
  * model enables a stateful entity to store the full state after processing each command instead of
  * using event sourcing. When interpreting code involving `DurableEntity`, the final resulting
  * value in the monadic chain is typically understood as the reply, and any written state is
  * persisted behind the scenes by the runtime. Read always provides the state as it was last
  * written.
  *
  * @tparam F
  *   context
  * @tparam S
  *   state
  *
  * @see
  *   `Entity` for the event-sourcing equivalent
  */
trait DurableEntity[F[_], S]
    extends StateReader[F, S]
    with StateReaderHelpers[F, S]
    with StateWriter[F, S]
