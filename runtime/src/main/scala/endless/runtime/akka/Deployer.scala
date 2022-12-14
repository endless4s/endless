package endless.runtime.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext, EntityTypeKey}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted, RecoveryFailed}
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.event.EventApplier
import endless.core.interpret.EffectorT._
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, CommandRouter, EntityIDCodec}
import endless.runtime.akka.data._
import org.typelevel.log4cats.Logger

trait Deployer {

  /** This function brings everything together and delivers a `Resource` with the repository
    * instance in context `F` bundled with the ref to the shard region actor returned by the call to
    * `ClusterSharding`.
    *
    * The function is parameterized with the context `F` and the various involved types: `S` for
    * entity state, `E` for events, `ID` for entity ID and `Alg` & `RepositoryAlg` for entity and
    * repository algebras respectively (both higher-kinded type constructors).
    *
    * In order to bridge Akka's implicit asynchronicity with the side-effect free context `F` used
    * for algebras, it requires `Async` from `F`. This makes it possible to use the `Dispatcher`
    * mechanism for running the command handling monadic chain "seemingly" synchronously (but in
    * fact submitting for execution and blocking for completion from within a thread managed by
    * akka).
    *
    * `Logger` is also required as the library supports some basic logging capabilities. Entity
    * algebra `Alg` must also be equipped with an instance of `FunctorK` to support natural
    * transformations.
    *
    * First parameters of the function are constructor functions for instances of entity &
    * repository algebras and effector, implicitly interpreted with their respective monad
    * transformers. An optional behavior customization hook is also provided for client code to
    * configure aspects such as recovery, etc.
    *
    * All remaining typeclass instances for entity operation are pulled from implicit scope: entity
    * name provider, entity ID encoder, command protocol, event application function in addition to
    * the usual akka ask timeout, actor system and cluster sharding extension.
    *
    * Although its signature looks complicated, in practice usage of this method isn't difficult
    * with the proper implicits and definitions: refer to the sample application for example usage:
    *
    * \```scala deployEntity[IO, Booking, BookingEvent, BookingID, BookingAlg,
    * BookingRepositoryAlg]( BookingEntity(_), BookingRepository(_), BookingEffector(_) )```
    *
    * '''Important''': `deployEntity` needs to be called upon application startup, before joining
    * the cluster as the `ClusterSharding` extension needs to know about the various entity types
    * beforehand.
    *
    * @param createEntity
    *   creator for entity algebra accepting an instance of `Entity`, interpreted with `EntityT`
    * @param createRepository
    *   creator for repository algebra accepting an instance of `Repository`
    * @param createEffector
    *   creator for effector accepting an instance of `Effector` and repository (for scenarios where
    *   effector processes require access to the repository itself) and entity, interpreted with
    *   `EffectorT` (you can pass in `(_) => EffectorT.unit` for unit effector)
    * @param customizeBehavior
    *   hook to further customize Akka `EventSourcedBehavior`. By default the behavior enforces
    *   replies, and is configured with command handler and event handler. It also triggers the
    *   effector upon successful recovery as well as logs in warning upon recovery failure.
    * @param sharding
    *   Akka cluster sharding extension
    * @param actorSystem
    *   actor system
    * @param nameProvider
    *   entity name provider
    * @param commandProtocol
    *   instance of command protocol for the algebra (serialization specification)
    * @param eventApplier
    *   instance of event application function (event folding on state)
    * @param askTimeout
    *   Akka ask timeout
    * @tparam F
    *   context, requires instances of `Async` and `Logger`
    * @tparam S
    *   state
    * @tparam E
    *   event
    * @tparam ID
    *   entity ID, requires an instance of `EntityIDCodec`
    * @tparam Alg
    *   entity algebra, requires an instance of `FunctorK`
    * @tparam RepositoryAlg
    *   repository algebra
    * @return
    *   resource (with underlying allocated dispatcher) containing the algebra in `F` context to
    *   interact with the entity together with Akka shard region actor ref
    */
  def deployEntity[F[_]: Async: Logger, S, E, ID: EntityIDCodec, Alg[_[_]]: FunctorK, RepositoryAlg[
      _[_]
  ]](
      createEntity: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]],
      createRepository: Repository[F, ID, Alg] => RepositoryAlg[F],
      createEffector: EffectorParameters[F, S, Alg, RepositoryAlg] => EffectorT[F, S, Alg, Unit],
      customizeBehavior: (
          EntityContext[Command],
          EventSourcedBehavior[Command, E, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: EventSourcedBehavior[Command, E, Option[S]]) =>
          behavior,
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]] =
        identity
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] =
    deployEntityF(
      (entity: Entity[EntityT[F, S, E, *], S, E]) => createEntity(entity).pure[F],
      (repository: Repository[F, ID, Alg]) => createRepository(repository).pure[F],
      (parameters: EffectorParameters[F, S, Alg, RepositoryAlg]) =>
        createEffector(parameters).pure[F],
      customizeBehavior,
      customizeEntity
    )

  /** Overload of [[Deployer.deployEntity]] that accepts creation functions expressed in `F`
    * context. This is particularly useful for creating a stateful effector (which is specific to an
    * entity).
    */
  def deployEntityF[F[_]: Async: Logger, S, E, ID: EntityIDCodec, Alg[
      _[_]
  ]: FunctorK, RepositoryAlg[
      _[_]
  ]](
      createEntity: Entity[EntityT[F, S, E, *], S, E] => F[Alg[EntityT[F, S, E, *]]],
      createRepository: Repository[F, ID, Alg] => F[RepositoryAlg[F]],
      createEffector: EffectorParameters[F, S, Alg, RepositoryAlg] => F[EffectorT[F, S, Alg, Unit]],
      customizeBehavior: (
          EntityContext[Command],
          EventSourcedBehavior[Command, E, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: EventSourcedBehavior[Command, E, Option[S]]) =>
          behavior,
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]] =
        identity
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] = {
    implicit val commandRouter: CommandRouter[F, ID] = ShardingCommandRouter.apply
    val repositoryT = RepositoryT.apply[F, ID, Alg]
    Resource.eval(
      for {
        interpretedEntityAlg <- createEntity(EntityT.instance)
        repository <- createRepository(repositoryT)
      } yield new DeployEntity(
        interpretedEntityAlg,
        repository,
        repositoryT,
        createEffector,
        customizeBehavior,
        customizeEntity
      ).apply
    )
  }.flatten

  final class EventApplierException(error: String) extends RuntimeException(error)

  private type EffectorParameters[F[_], S, Alg[_[_]], RepositoryAlg[_[_]]] =
    (Effector[EffectorT[F, S, Alg, *], S, Alg], RepositoryAlg[F], Alg[F])

  private class DeployEntity[F[_]: Async: Logger, S, E, ID: EntityIDCodec, Alg[
      _[_]
  ]: FunctorK, RepositoryAlg[_[_]]](
      interpretedEntityAlg: Alg[EntityT[F, S, E, *]],
      repository: RepositoryAlg[F],
      repositoryT: RepositoryT[F, ID, Alg],
      createEffector: EffectorParameters[F, S, Alg, RepositoryAlg] => F[EffectorT[F, S, Alg, Unit]],
      customizeBehavior: (
          EntityContext[Command],
          EventSourcedBehavior[Command, E, Option[S]]
      ) => Behavior[Command],
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]]
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout
  ) {
    private val entityTypeKey = EntityTypeKey[Command](nameProvider())

    def apply: Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] =
      Dispatcher.parallel[F].map { implicit dispatcher =>
        val akkaEntity = akka.cluster.sharding.typed.scaladsl.Entity(
          EntityTypeKey[Command](nameProvider())
        ) { implicit context =>
          Behaviors.setup { implicit actor =>
            implicit val passivator: EntityPassivator[F] =
              dispatcher.unsafeRunSync(EntityPassivator[F])
            implicit val entity: Alg[F] =
              repositoryT.entityFor(implicitly[EntityIDCodec[ID]].decode(context.entityId))
            implicit val effector: EffectorT[F, S, Alg, Unit] =
              dispatcher.unsafeRunSync(createEffector(EffectorT.instance, repository, entity))
            customizeBehavior(
              context,
              EventSourcedBehavior
                .withEnforcedReplies[Command, E, Option[S]](
                  PersistenceId(entityTypeKey.name, context.entityId),
                  Option.empty[S],
                  commandHandler = handleCommand,
                  eventHandler = handleEvent
                )
                .receiveSignal {
                  case (state, RecoveryCompleted) =>
                    dispatcher.unsafeRunAndForget(
                      Logger[F].info(
                        show"Recovery of ${nameProvider()} entity ${context.entityId} completed"
                      ) >> effector.runS(state, entity).flatMap(passivator.apply)
                    )
                  case (_, RecoveryFailed(failure)) =>
                    dispatcher.unsafeRunSync(
                      Logger[F].warn(
                        show"Recovery of ${nameProvider()} entity ${context.entityId} failed with error ${failure.getMessage}"
                      )
                    )
                }
            )
          }
        }
        (repository, sharding.init(customizeEntity(akkaEntity)))
      }

    private def handleEvent(state: Option[S], event: E)(implicit
        entity: Alg[F],
        dispatcher: Dispatcher[F],
        passivator: EntityPassivator[F],
        effector: EffectorT[F, S, Alg, Unit]
    ) = eventApplier.apply(state, event) match {
      case Left(error) =>
        dispatcher.unsafeRunSync(Logger[F].warn(error))
        throw new EventApplierException(error)
      case Right(newState) => newState
    }

    private def handleCommand(state: Option[S], command: Command)(implicit
        entity: Alg[F],
        dispatcher: Dispatcher[F],
        passivator: EntityPassivator[F],
        effector: EffectorT[F, S, Alg, Unit]
    ) = {
      val incomingCommand =
        commandProtocol.server[EntityT[F, S, E, *]].decode(command.payload)
      val effect = Logger[F].debug(
        show"Handling command for ${nameProvider()} entity ${command.id}"
      ) >> incomingCommand
        .runWith(interpretedEntityAlg)
        .run(state)
        .flatMap {
          case Left(error) =>
            Logger[F].warn(error) >> Effect.unhandled[E, Option[S]].thenNoReply().pure[F]
          case Right((events, reply)) if events.nonEmpty =>
            Effect
              .persist(events.toList)
              .thenRun((state: Option[S]) =>
                // run the effector asynchronously, as it can describe long-running processes
                dispatcher
                  .unsafeRunAndForget(effector.runS(state, entity).flatMap(passivator.apply))
              )
              .thenReply(command.replyTo) { _: Option[S] =>
                Reply(incomingCommand.replyEncoder.encode(reply))
              }
              .pure[F]
          case Right((_, reply)) =>
            Effect
              .reply[Reply, E, Option[S]](command.replyTo)(
                Reply(incomingCommand.replyEncoder.encode(reply))
              )
              .pure[F]
        }
      dispatcher.unsafeRunSync(effect)
    }
  }

}
