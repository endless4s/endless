package endless.runtime.pekko

import endless.core.interpret.LoggerLiftingHelper
import endless.runtime.pekko.deploy.{PekkoDeployer, PekkoDurableDeployer}

package object syntax {
  object deploy extends PekkoDeployer with PekkoDurableDeployer with LoggerLiftingHelper
}
