# Native runtime

Endless's native runtime allows running its abstractions with cats-effect directly (no actor system). It is designed for Kubernetes deployments. It has built-in support for grpc point-to-point transport between the pods, and doobie persistence (but these are behind generic traits that can be overridden). 

## Sharding
A shard designates a group of entities that are co-located in a pod. For a specific entity, the shard identifier must be stable and unique (otherwise, the entity might accidentally be started in several pods). Shards are distributed on available pods in the cluster. The decision of where to allocate a shard is done by a shard allocation strategy.

Each pod in the cluster owns a number of shards. This is a simple distribution based on a function that maps the entity ID to a numerical value in the range `[0, shardCount]`. In other words, the range for each pod is simply decided by dividing `shardCount` by `podCount`.

Cluster membership is implemented using Kubernetes leases: pods register a lease with a certain label. Each pod periodically refreshes its list of other pods in the cluster. 

### Allocation strategies

#### Stateless balanced (default)
A unique ordering is defined between the pods so always know which pod holds what range: this is done via the initial lease registration mechanism. Membership leases are numbered with a postfix in the name: Kubernetes guarantees uniqueness of resource names within a certain namespace. When registering, the pod retrieves the list of already registered leases and competes to register the smallest available lease number. If that's already taken (concurrency), it refreshes the list again and aims for the next slot. When pods disappear (cluster downscaling), holes are potentially left in the numbering, but the ordering is still preserved.

This stateless shard allocation strategy is more simple than a stateful allocation strategy (with e.g. cluster singleton) as no synchronization is required between pods. It also reduces latency because there is no need to look up the location of the shard for a certain entity.

When a pod is added or removed to/from the cluster the shard range per pod changes to distribute the shards evenly. Shards on existing pods will be shuffled between the pods to reflect the new distribution: each pod looks at its new range, and downs entities that are no longer part of it. Outstanding commands for the downed entities are forwarded to the new owner. Conversely, it also recovers any remembered entities that are now part of its range.  

Since propagation of pod membership changes is eventually consistent (via the periodic refresh mechanism), but we need strong consistency on a single shard owner at a time, ownership of a shard is ensured by a kubernetes lease. This lease is renewed periodically (with a frequency just lower than the pod list refresh so that shards are available when rebalancing).      

#### External
External strategy is used when the shards are managed by an external entity, e.g. the kafka broker. Concretely, for kafka, this means that we keep shard allocation in the cluster aligned with Kafka consumers partition assignments.

## Rebalancing
Interpretation of state reads for entities that are being rebalanced are delayed until the rebalancing is complete. When attempting to send a command to a remote shard that is rebalancing, the underlying rpc protocol explicitly replies with a "rebalancing in progress" message. In such cases the command is resent after a delay (longer than the pods list refresh, so that we have a better hope of success for the following attempt). 

## Partitions
Each pod sends keep-alives to the others. It does several attempts at this, as it can be that the list of pods will be refreshed. The in-memory list of pods keeps track of the connectivity status and the number of keep-alive failures. At a certain threshold, it's likely that there is a partition between the pods (otherwise kube would have detected the dead pod and started another, which in turn means the list would have been refreshed).

At this point, we try acquiring the SBR lease. The timeout for this acquisition is inversely proportional to the number of nodes we are connected to, so that the majority side of the partition has a better chance of grabbing it. If we succeed, we propagate a "flag" to the other pods that we can contact. If we don't succeed in acquiring the lease, we wait to see if we'll receive the flag. If we don't receive the flag after a while, it means we are on the wrong side of the partition and we should shut down.    

Impossibility to refresh the lease (partition to Kubernetes control plane) always leads to downing ourselves after a few attempts.

## Passivation
Only explicit via abstraction. A helper function is provided to setup passivation after a delay. 

## Recovery
Recovery upon first state reading with `StateReader`. Note that we don't need to recover the entity to write events if the entity interaction leading to event writing does not involve the state.

## Remember entities
Auto-recovery aka "remember entities" via an explicit abstraction. Need to provide a store for the entity IDs. Actually to enable at least once, adding the entity to the "remembrance set" should be transactional with the event writing. We could expose this it as a flag in the event writing abstraction.

## Event persistence
Interpretation of `EventWriter` writes events to a journal trait, with JDBC implementation with doobie provided out of the box.
