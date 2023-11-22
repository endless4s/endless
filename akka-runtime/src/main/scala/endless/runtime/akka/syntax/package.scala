package endless.runtime.akka

import endless.runtime.akka.deploy.{AkkaDeployer, AkkaDurableDeployer}

package object syntax {
  object deploy extends AkkaDeployer with AkkaDurableDeployer
}
