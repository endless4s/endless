package endless.protobuf

import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import endless.protobuf.test.proto.dummy.DummyCommand

class ScalaPbSerializerSuite extends munit.FunSuite {
  val akkaSerialization = new Fixture[Serialization]("system") {
    def apply(): Serialization = {
      SerializationExtension(
        new TestKit(
          ActorSystem(
            "ScalaPbSerializerSpec",
            ConfigFactory.parseString(ScalaPbSerializerSuite.serializationConfig)
          )
        ).system
      )
    }
  }

  test("be found by akka serialization extension when asked for it for test type") {
    val clasz = akkaSerialization().serializerFor(classOf[DummyCommand]).getClass
    assert(clasz == classOf[ScalaPbSerializer])
  }

  test("serialize and deserialize event correctly") {
    val dummy = DummyCommand("dummy")
    val serialized = akkaSerialization().serialize(dummy)
    assert(serialized.isSuccess)
    val deserialized =
      akkaSerialization().deserialize(serialized.get, classOf[DummyCommand])
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
      |      scalapb = "endless.protobuf.ScalaPbSerializer"
      |    }
      |
      |    serialization-bindings {
      |      "scalapb.GeneratedMessage" = scalapb
      |    }
      |
      |    serialization-identifiers {
      |      "endless.protobuf.ScalaPbSerializer" = 4242
      |    }
      |  }
      |}
    """.stripMargin
}
