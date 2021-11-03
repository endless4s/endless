# Typeclasses
All definitions make use of the following type parameters: 

 - `F[_]`: abstract effectful context `F` encapsulating all values, e.g. `IO[Boolean]`
 - `Alg[_[_]]`: algebra allowing interaction with the entity, e.g. @github[BookingAlg\[IO\[*\]\]](/example/src/main/scala/endless/example/algebra/BookingAlg.scala)
 - `ID`: entity ID, e.g.  @github[BookingID](/example/src/main/scala/endless/example/data/Booking.scala)
 - `S`: entity state, e.g. @github[Booking](/example/src/main/scala/endless/example/data/Booking.scala)
 - `E`: entity event, e.g. @github[BookingEvent](/example/src/main/scala/endless/example/data/BookingEvent.scala)

@@@ index
* [Repository](repository.md)
* [Entity](entity.md)
* [EventApplier](applier.md)
* [CommandProtocol](protocol.md)
* [CommandRouter](router.md)
* [Effector](effector.md)
* [NameProvider](name.md)
* [IDEncoder](id.md)
@@@

