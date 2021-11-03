# Typeclasses
All definitions make use of the following type parameters: 

 - `F[_]`: abstract effectful context `F` encapsulating all values, e.g. `IO[Boolean]`
 - `Alg[_[_]]`: algebra allowing interaction with the entity, e.g. @github[BookingAlg\[IO\[*\]\]](/example/src/main/scala/endless/example/algebra/BookingAlg.scala)
 - `ID`: entity ID, e.g.  @github[BookingID](/example/src/main/scala/endless/example/data/Booking.scala)
 - `S`: entity state, e.g. @github[Booking](/example/src/main/scala/endless/example/data/Booking.scala)
 - `E`: entity event, e.g. @github[BookingEvent](/example/src/main/scala/endless/example/data/BookingEvent.scala)

@@@ index
* [Repository](typeclasses/repository.md)
* [Entity](typeclasses/entity.md)
* [EventApplier](typeclasses/applier.md)
* [CommandProtocol](typeclasses/protocol.md)
* [CommandRouter](typeclasses/router.md)
* [Effector](typeclasses/effector.md)
* [NameProvider](typeclasses/name.md)
* [IDEncoder](typeclasses/id.md)
@@@

