# Repository

```scala
trait Repository[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
```

@scaladoc[Repository](endless.core.entity.Repository) is parametrized with entity ID type `ID` and entity algebra `Alg[_[_]]`. It represents the ability to obtain an instance of that algebra (the entity) for a specific ID. It is used by the interpreter of the repository algebra (e.g. @github[BookingRepository](/example/src/main/scala/endless/example/logic/BookingRepository.scala)). 
