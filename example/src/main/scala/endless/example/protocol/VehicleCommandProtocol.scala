package endless.example.protocol

import endless.core.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import endless.example.algebra.VehicleAlg
import endless.example.data.{LatLon, Speed}
import endless.example.proto.vehicle.commands.VehicleCommand.Command
import endless.example.proto.vehicle.commands._
import endless.example.proto.vehicle.models.{LatLonV1Full, SpeedV1Full}
import endless.example.proto.vehicle.replies.{GetPositionV1FullReply, GetSpeedV1FullReply, UnitReply}
import endless.example.protocol.VehicleCommandProtocol.UnexpectedCommandException
import endless.protobuf.{ProtobufCommandProtocol, ProtobufDecoder}

class VehicleCommandProtocol extends ProtobufCommandProtocol[VehicleAlg] {
  def server[F[_]]: Decoder[IncomingCommand[F, VehicleAlg]] =
    ProtobufDecoder[VehicleCommand].map(_.command match {
      case Command.Empty => throw new UnexpectedCommandException
      case Command.SetSpeedV1(value) =>
        incomingCommand[F, UnitReply, Unit](
          _.setSpeed(Speed(value.speed.metersPerSecond)),
          _ => UnitReply()
        )
      case Command.SetPositionV1(value) =>
        incomingCommand[F, UnitReply, Unit](
          _.setPosition(LatLon(value.position.lat, value.position.lon)),
          _ => UnitReply()
        )
      case Command.GetSpeedV1(_) =>
        incomingCommand[F, GetSpeedV1FullReply, Option[Speed]](
          _.getSpeed,
          maybeSpeed =>
            GetSpeedV1FullReply.of(maybeSpeed.map(speed => SpeedV1Full.of(speed.metersPerSecond)))
        )
      case Command.GetPositionV1(_) =>
        incomingCommand[F, GetPositionV1FullReply, Option[LatLon]](
          _.getPosition,
          maybePosition =>
            GetPositionV1FullReply.of(
              maybePosition.map(position => LatLonV1Full.of(position.lat, position.lon))
            )
        )
    })

  def client: VehicleAlg[OutgoingCommand[*]] = new VehicleAlg[OutgoingCommand] {
    def setSpeed(speed: Speed): OutgoingCommand[Unit] =
      outgoingCommand[VehicleCommand, UnitReply, Unit](
        VehicleCommand.of(
          Command.SetSpeedV1(SetSpeedV1Full.of(SpeedV1Full.of(speed.metersPerSecond)))
        ),
        _ => ()
      )

    def setPosition(position: LatLon): OutgoingCommand[Unit] =
      outgoingCommand[VehicleCommand, UnitReply, Unit](
        VehicleCommand.of(
          Command.SetPositionV1(SetPositionV1Full.of(LatLonV1Full.of(position.lat, position.lon)))
        ),
        _ => ()
      )

    def getSpeed: OutgoingCommand[Option[Speed]] =
      outgoingCommand[VehicleCommand, GetSpeedV1FullReply, Option[Speed]](
        VehicleCommand.of(Command.GetSpeedV1(GetSpeedV1Full())),
        _.speed.map(speed => Speed(speed.metersPerSecond))
      )

    def getPosition: OutgoingCommand[Option[LatLon]] =
      outgoingCommand[VehicleCommand, GetPositionV1FullReply, Option[LatLon]](
        VehicleCommand.of(Command.GetPositionV1(GetPositionV1Full())),
        _.position.map(position => LatLon(position.lat, position.lon))
      )
  }
}

object VehicleCommandProtocol {
  final class UnexpectedCommandException extends RuntimeException("Unexpected command")
}
