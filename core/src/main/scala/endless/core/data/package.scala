package endless.core

import cats.data.Chain
import endless.\/

package object data {

  /** Type alias for either an error or a tuple of events chain and value
    * @tparam E
    *   event
    * @tparam A
    *   value
    */
  type Folded[E, A] = String \/ (Chain[E], A)
}
