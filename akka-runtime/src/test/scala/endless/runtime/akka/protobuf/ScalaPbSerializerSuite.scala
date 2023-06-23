package endless.runtime.akka.protobuf

import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import com.typesafe.config.ConfigFactory
import endless.protobuf.test.proto.dummy.DummyCommand

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ScalaPbSerializerSuite extends munit.FunSuite {
  val akkaSerialization = FunFixture[Serialization](
    setup = { _ =>
      val system = ActorSystem(
        "ScalaPbSerializerSpec",
        ConfigFactory.parseString(ScalaPbSerializerSuite.serializationConfig)
      )
      SerializationExtension(system)
    },
    teardown = { fixture => Await.result(fixture.system.terminate(), 30.seconds) }
  )

  akkaSerialization.test(
    "be found by akka serialization extension when asked for it for test type"
  ) { serialization =>
    val clasz = serialization.serializerFor(classOf[DummyCommand]).getClass
    assert(clasz == classOf[ScalaPbSerializer])
  }

  akkaSerialization.test("serialize and deserialize event correctly") { serialization =>
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
      |akka {
      |  actor {
      |    serializers {
      |      scalapb = "endless.runtime.akka.protobuf.ScalaPbSerializer"
      |    }
      |
      |    serialization-bindings {
      |      "scalapb.GeneratedMessage" = scalapb
      |    }
      |
      |    serialization-identifiers {
      |      "endless.runtime.akka.protobuf.ScalaPbSerializer" = 1111
      |    }
      |  }
      |}
    """.stripMargin
}
