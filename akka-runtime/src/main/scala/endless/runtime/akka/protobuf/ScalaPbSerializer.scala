package endless.runtime.akka.protobuf

import akka.actor.ExtendedActorSystem
import akka.serialization.BaseSerializer
import scalapb.GeneratedMessageCompanion

import java.util.concurrent.atomic.AtomicReference

/*
 Akka serializer making use of scalapb-generated classes for protobuf serialization
 Inspired by https://gist.github.com/thesamet/5d0349b40d3dc92859a1a2eafba448d5
 */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Null"
  )
)
class ScalaPbSerializer(val system: ExtendedActorSystem) extends BaseSerializer {
  private val classToCompanionMapRef =
    new AtomicReference[Map[Class[?], GeneratedMessageCompanion[?]]](Map.empty)

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: scalapb.GeneratedMessage => e.toByteArray
    case _ => throw new IllegalArgumentException("Need a subclass of scalapb.GeneratedMessage")
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[?]]): AnyRef =
    manifest match {
      case Some(clazz) =>
        // noinspection ScalaStyle
        @scala.annotation.tailrec
        def messageCompanion(
            companion: GeneratedMessageCompanion[?] = null
        ): GeneratedMessageCompanion[?] = {
          val classToCompanion = classToCompanionMapRef.get()
          classToCompanion.get(clazz) match {
            case Some(cachedCompanion) => cachedCompanion
            case None                  =>
              val uncachedCompanion =
                if (companion eq null)
                  Class
                    .forName(clazz.getName + "$", true, clazz.getClassLoader)
                    .getField("MODULE$")
                    .get(())
                    .asInstanceOf[GeneratedMessageCompanion[?]]
                else companion
              if (
                classToCompanionMapRef.compareAndSet(
                  classToCompanion,
                  classToCompanion.updated(clazz, uncachedCompanion)
                )
              )
                uncachedCompanion
              else
                messageCompanion(uncachedCompanion)
          }
        }
        messageCompanion().parseFrom(bytes).asInstanceOf[AnyRef]
      case _ =>
        throw new IllegalArgumentException(
          "Need a ScalaPB companion class to be able to deserialize."
        )
    }
}
