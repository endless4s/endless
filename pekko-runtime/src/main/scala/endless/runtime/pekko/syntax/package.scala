package endless.runtime.pekko

import endless.runtime.pekko.deploy.{PekkoDeployer, PekkoDurableDeployer}

package object syntax {
  object deploy extends PekkoDeployer with PekkoDurableDeployer
}
