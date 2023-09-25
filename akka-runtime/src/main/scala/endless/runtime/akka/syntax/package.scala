package endless.runtime.akka

import endless.core.interpret.LoggerLiftingHelper
import endless.runtime.akka.deploy.{AkkaDeployer, AkkaDurableDeployer}

package object syntax {
  object deploy extends AkkaDeployer with AkkaDurableDeployer with LoggerLiftingHelper
}
