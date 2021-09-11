package akka.persistence.tagless.core

import akka.persistence.tagless.\/
import cats.data.Chain

package object data {
  type Folded[E, A] = String \/ (Chain[E], A)
}
