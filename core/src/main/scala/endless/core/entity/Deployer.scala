package endless.core.entity

import cats.effect.kernel.{Async, Resource}
import cats.tagless.FunctorK
import endless.core.event.EventApplier
import endless.core.interpret.{EffectorInterpreter, EntityInterpreter, RepositoryInterpreter}
import endless.core.protocol.{CommandProtocol, EntityIDCodec}

/** `Deployer` deploys event-sourced entity repositories by assembling the required interpreters and
  * components.
  */
trait Deployer {

  /** Platform-specific deployment parameters: the final type is to be specified in implementations
    * @tparam F
    *   effect type
    * @tparam ID
    *   entity ID
    * @tparam S
    *   entity state
    * @tparam E
    *   entity event
    */
  type DeploymentParameters[F[_], ID, S, E]

  /** Handle on a deployed repository: the final type is to be specified in implementations
    * @tparam F
    *   effect type
    * @tparam RepositoryAlg
    *   repository algebra
    */
  type Deployment[F[_], RepositoryAlg[_[_]]]

  /** Deploys an event-sourced entity repository in context `F`, returning an instance of
    * implementation-specific `Deployment` typed with the Repository algebra, wrapped in a resource
    * (since deployments typically require finalization).
    *
    * Repository operation uses the three provided interpreters in combination, following a strictly
    * defined sequence:
    *   - the interpreted repository is used to create a handle on the entity with the specified ID
    *     implementing the entity algebra, so that the caller can interact with it
    *   - the entity interpreter runs the invoked function on the entity algebra: this possibly
    *     involves reading the entity state (e.g. for validation), and writing events (which affects
    *     the state)
    *   - after events were written, a possible side-effect is triggered: this can be asynchronous
    *     (i.e. the function doesn't wait for completion of the side-effect to return)
    *   - the function finally returns to the caller with the result of the operation described by
    *     the entity algebra (reply value, typically encoded over the wire in a distributed
    *     deployment)
    *
    * This interaction pattern occurs with "actor-like" semantics: all calls on the entity are
    * processed in sequence.
    *
    * The function is parameterized with the context `F` and the various involved types: `S` for
    * entity state, `E` for events, `ID` for entity ID and `Alg` & `RepositoryAlg` for entity and
    * repository algebras respectively (both higher-kinded type constructors).
    *
    * Since the behavior described above involves concurrent handling of repository interactions and
    * asynchronous side-effects, we expect `Async` from `F`.
    *
    * We also require `FunctorK` for `Alg` to support natural transformations: we use them for
    * interpretation of the entity algebra into actual platform-specific runnable code in `F`, and for
    * translating calls into messages via the `CommandRouter` abstraction.
    *
    * `EntityIDCodec` is used to encode/decode entity IDs to/from strings.
    *
    * @param repository
    *   provides access to to an entity with a specific ID via its algebra
    * @param entity
    *   runs calls on entity algebra
    * @param effector
    *   handles side-effects after entity interaction
    * @param nameProvider
    *   provides a name for the entity (in other words, the "type of entity", e.g. "booking")
    * @param commandProtocol
    *   protocol-centric definition of entity algebra: defines a wire encoding for interactions with
    *   remote entities
    * @param eventApplier
    *   defines how events are applied to the entity state
    * @param parameters
    *   platform-specific deployment parameters
    * @tparam F
    *   effect type
    * @tparam ID
    *   entity ID
    * @tparam S
    *   entity state
    * @tparam E
    *   event type
    * @tparam Alg
    *   entity algebra
    * @tparam RepositoryAlg
    *   repository algebra
    * @return
    *   a resource encapsulating access to the deployed repository algebra
    */
  def deployRepository[F[_]: Async, ID: EntityIDCodec, S, E, Alg[_[_]]: FunctorK, RepositoryAlg[_[
      _
  ]]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      entity: EntityInterpreter[F, S, E, Alg],
      effector: EffectorInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      parameters: DeploymentParameters[F, ID, S, E]
  ): Resource[F, Deployment[F, RepositoryAlg]]
}
