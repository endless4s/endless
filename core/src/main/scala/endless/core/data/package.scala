package endless.core

import cats.data.Chain
import endless.\/

package object data {
  type Folded[E, A] = String \/ (Chain[E], A)
}
