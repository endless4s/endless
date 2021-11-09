package endless.runtime.akka

import endless.core.interpret.LoggerLiftingHelper

package object syntax {
  object deploy extends Deployer with LoggerLiftingHelper
}
