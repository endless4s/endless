package object endless {
  type \/[A, B] = Either[A, B]

  implicit class EitherT[F[_], A, B](f: F[A \/ B]) {
    def attempt: EitherT[F, A, B] = EitherT(f)
  }
}
