package endless.runtime.pekko

import endless.core.interpret.LoggerLiftingHelper
import endless.runtime.pekko.deploy.{Deployer, DurableDeployer}

package object syntax {
  object deploy extends Deployer with DurableDeployer with LoggerLiftingHelper
}
