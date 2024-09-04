package endless.runtime.pekko.protobuf

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.serialization.{Serialization, SerializationExtension}
import com.typesafe.config.ConfigFactory
import endless.protobuf.test.proto.dummy.DummyCommand

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ScalaPbSerializerSuite extends munit.FunSuite {
  val pekkoSerialization = FunFixture[Serialization](
    setup = { _ =>
      val system = ActorSystem(
        "ScalaPbSerializerSpec",
        ConfigFactory.parseString(ScalaPbSerializerSuite.serializationConfig)
      )
      SerializationExtension(system)
    },
    teardown = { fixture => Await.result(fixture.system.terminate(), 30.seconds) }
  )

  pekkoSerialization.test(
    "be found by pekko serialization extension when asked for it for test type"
  ) { serialization =>
    val clasz = serialization.serializerFor(classOf[DummyCommand]).getClass
    assert(clasz == classOf[ScalaPbSerializer])
  }

  pekkoSerialization.test("serialize and deserialize event correctly") { serialization =>
    val dummy = DummyCommand("dummy")
    val serialized = serialization.serialize(dummy)
    assert(serialized.isSuccess)
    val deserialized = serialization.deserialize(serialized.get, classOf[DummyCommand])
    assert(deserialized.isSuccess)
    assertEquals(deserialized.get, dummy)
  }
}

object ScalaPbSerializerSuite {
  val serializationConfig: String =
    """
      |pekko {
      |  actor {
      |    provider = local
      |    serializers {
      |      scalapb = "endless.runtime.pekko.protobuf.ScalaPbSerializer"
      |    }
      |
      |    serialization-bindings {
      |      "scalapb.GeneratedMessage" = scalapb
      |    }
      |
      |    serialization-identifiers {
      |      "endless.runtime.pekko.protobuf.ScalaPbSerializer" = 1111
      |    }
      |  }
      |  coordinated-shutdown.exit-jvm = off
      |}
    """.stripMargin
}
