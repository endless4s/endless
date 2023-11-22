# Deployer

```scala
trait Deployer {
  def deployRepository[F[_]: Async, ID: EntityIDCodec, S, E, Alg[_[_]], RepositoryAlg[_[_]]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      entity: EntityInterpreter[F, S, E, Alg],
      effector: F[EffectorInterpreter[F, S, Alg, RepositoryAlg]]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      parameters: DeploymentParameters[F, ID, S, E]
  ): Resource[F, Deployment[F, RepositoryAlg]]
}
```

@scaladoc[Deployer](endless.core.entity.Deployer) brings everything together to "materialize" an entity repository. It returns a `Deployment` instance wrapped in a cats-effect `Resource` (to support finalization). The `Deployment` type is defined by implementers (aka. the runtime, e.g. @scaladoc[PekkoDeployer](endless.runtime.pekko.deploy.PekkoDeployer)), and is meant to provide a `RepositoryAlg` instance allowing for interactions with the deployed repository. It can also expose extra fields or methods that are relevant to the particular runtime. 

`deployRepository` is parametrized with the following type parameters:

 - `F[_]`: abstract effectful context `F` encapsulating all values, e.g. `IO[*]`
 - `Alg[_[_]]`: algebra allowing interaction with the entity, e.g. @github[BookingAlg\[IO\[*\]\]](/example/src/main/scala/endless/example/algebra/BookingAlg.scala)
 - `ID`: entity ID, e.g.  `final case class BookingID(id: UUID) extends AnyVal`
 - `S`: entity state, e.g. @github[Booking](/example/src/main/scala/endless/example/data/Booking.scala)
 - `E`: entity event, e.g. @github[BookingEvent](/example/src/main/scala/endless/example/data/BookingEvent.scala)
 - `RepositoryAlg[_[_]]`: repository algebra, e.g. @github[BookingRepositoryAlg\[IO\[*\]\]](/example/src/main/scala/endless/example/repository/BookingRepositoryAlg.scala)

Repository operation relies on the three provided interpreted algebras for `repository`, `entity` and `effector`. They are used in combination, following a strictly defined sequence:

 1. the repository is used to create a handle on the entity with the specified ID. This handle implements the entity algebra, using with the caller can interact with the entity.
 2. when a function of the entity algebra is invoked, this invocation is serialized using the `commandProtocol` and sent over the wire thanks to [CommandSender](endless.core.protocol.CommandSender). On the receiving node, the message is decoded and interpreted by the provided `EntityT`-based interpreter: typically involving reading the entity state (e.g. for validation), and writing events (which can lead to a new version of the state via the `eventApplier` function)
 3. after events are written, a possible side-effect is triggered: this supports asynchronicity (i.e. starting fibers)
 4. the function finally returns to the caller with the result of the operation, encoded over the wire as a reply using `commandProtocol` and routed back to the caller thanks to `CommandSender`.

See also sequence diagrams in @ref:[Example App](example.md) that represent this flow for a concrete entity.

@@@ Note
The entity interaction pattern described above occurs with "actor-like" semantics: all calls on a specific entity instance are processed in sequence.
@@@
