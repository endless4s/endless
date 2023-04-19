package endless.runtime.akka

import endless.core.interpret.LoggerLiftingHelper
import endless.runtime.akka.deploy.{Deployer, DurableDeployer}

package object syntax {
  object deploy extends Deployer with DurableDeployer with LoggerLiftingHelper
}
