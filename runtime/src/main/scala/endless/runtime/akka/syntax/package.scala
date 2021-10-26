package endless.runtime.akka

import endless.core.interpret.LoggerLiftingHelpers

package object syntax {
  object deploy extends Deployer with LoggerLiftingHelpers
}
