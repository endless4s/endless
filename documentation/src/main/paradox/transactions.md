# Transactions

<img src="https://raw.githubusercontent.com/endless4s/endless-transaction/master/documentation/src/main/paradox/logo.svg" width="200">

Operations spanning multiple entities in the cluster often require coordination to ensure consistency. The [endless-transaction](https://endless4s.github.io/transaction) side library, itself built with endless, allows to describe such cluster-spanning operations with the two-phase commit protocol and integrates smoothly with endless's abstractions. The library [example](https://endless4s.github.io/transaction/example.html) illustrates this precise use case, with a cluster of bank accounts and a transfer feature implemented with transactions.

