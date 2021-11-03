# Inspiration

Our journey towards elevation of our event-sourcing code abstraction level started with an inheritance-based model, that we described in [this article](https://medium.com/bestmile/domain-driven-event-sourcing-with-akka-typed-5f5b8bbfb823). We introduced `EntityCommand` and `EntityEvent` traits, which allowed us in turn to define some`CommandProcessor` and `EventApplier` constructs with generic implementations and mappings for Akka. This was already a good step in the right direction as it allowed us to host command handling and event handling logic in the domain, free from any Akka dependencies. 

## Implicit commands & replies 
However, with this approach, commands and replies are still an integral part of the domain model. And if you think about it, such models and associated mappings to/from values do not add any value to domain expression, rather the contrary. Domain code is more compact if we can use straight algebras and values, without concern for the underlying necessary reification to cross node boundaries in the cluster. It was also always puzzling to us that event-sourcing looks a lot like a reader-writer monad. 

## Aecor
At this point, we ran into the outstanding work done by Denis Mikhaylov & contributors on [Aecor](https://github.com/notxcain/aecor) as well as the excellent [blog series](https://pavkin.ru/aecor-intro/) by Vladimir Pavkin. This was a real eye-opener, and we highly recommend the read!

However, we could not envision using the library for a number of reasons, chiefly because we are attached to keeping the layer above Akka as lightweight as possible, and we want to keep control on serialization aspects. There also are other more subtle differences: for instance we consider command rejections (and more generally, "business errors") as first-order values, therefore opting for `Either` for replies. In *endless*, we've also tried to stick to close to DDD & Akka nomenclature as much as possible. 

## Bridging the gap
By providing of typeclasses allowing for expressive description of entity behavior and embedding common event sourcing patterns into these abstractions, we hope that we make it more approachable for the functional community to enjoy side-effect free programming while still benefiting from the vast power of Akka with an experience as frictionless as possible.   
