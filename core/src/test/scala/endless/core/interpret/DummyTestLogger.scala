package endless.core.interpret

import cats.tests.ListWrapper
import org.typelevel.log4cats.Logger

class DummyTestLogger extends Logger[ListWrapper] {
  override def error(message: => String): ListWrapper[Unit] = ListWrapper.applicative.unit
  override def warn(message: => String): ListWrapper[Unit] = ListWrapper.applicative.unit
  override def info(message: => String): ListWrapper[Unit] = ListWrapper.applicative.unit
  override def debug(message: => String): ListWrapper[Unit] = ListWrapper.applicative.unit
  override def trace(message: => String): ListWrapper[Unit] = ListWrapper.applicative.unit
  override def error(t: Throwable)(message: => String): ListWrapper[Unit] =
    ListWrapper.applicative.unit
  override def warn(t: Throwable)(message: => String): ListWrapper[Unit] =
    ListWrapper.applicative.unit
  override def info(t: Throwable)(message: => String): ListWrapper[Unit] =
    ListWrapper.applicative.unit
  override def debug(t: Throwable)(message: => String): ListWrapper[Unit] =
    ListWrapper.applicative.unit
  override def trace(t: Throwable)(message: => String): ListWrapper[Unit] =
    ListWrapper.applicative.unit
}
