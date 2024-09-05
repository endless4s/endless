package endless.core.entity

import cats.effect.kernel.{Async, Resource}
import endless.core.event.EventApplier
import endless.core.interpret.{SideEffectInterpreter, BehaviorInterpreter, RepositoryInterpreter}
import endless.core.protocol.{CommandProtocol, EntityIDCodec}

/** `Deployer` deploys event-sourced entity repositories by assembling required interpreters and
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
    * Repository operation is defined by the interpreted repository, behavior and side-effect
    * algebras, following a strictly defined sequence:
    *   - the interpreted repository is used to create a handle on the entity with the specified ID
    *     implementing the entity algebra, so that the caller can interact with it
    *   - when a function of the entity algebra is invoked, this invocation is serialized using the
    *     `commandProtocol` and sent over the wire thanks to `CommandSender`. On the receiving node,
    *     the message is decoded and run with the provided `behavior` interpreter: this typically
    *     involves reading the entity state (e.g. for validation), and writing events (which can
    *     lead to a new version of the state via the `eventApplier` function)
    *   - after events are written, a possible side-effect is triggered: this is by default
    *     asynchronous (i.e. the function doesn't wait for completion of the side-effect to return)
    *     but can be made synchronous by overriding the `runModeFor` method in the `SideEffect`
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
    * asynchronous side-effecting, we expect `Async` from `F`.
    *
    * `EntityIDCodec` is used to encode/decode entity IDs to/from strings.
    *
    * @param repository
    *   interpreter for the repository algebra (used to "materialize" the repository)
    * @param behavior
    *   interpreter for the behavior algebra (used to "materialize" the behavior)
    * @param sideEffect
    *   interpreter for the side-effect algebra (used to "materialize" the side-effect)
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
  def deployRepository[F[_]: Async, ID: EntityIDCodec, S, E, Alg[_[_]], RepositoryAlg[_[_]]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      behavior: BehaviorInterpreter[F, S, E, Alg],
      sideEffect: SideEffectInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[ID, Alg],
      eventApplier: EventApplier[S, E],
      parameters: DeploymentParameters[F, ID, S, E]
  ): Resource[F, Deployment[F, RepositoryAlg]]
}
