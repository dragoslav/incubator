package nl.proja.pistraw

import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.util.{ActorDescription, ActorSupport, FutureSupport}
import nl.proja.pistraw.ElasticSearchActor.IndexDocument
import nl.proja.pistraw.PiStraw.{Shutdown, Start}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import org.json4s.JsonAST.JString
import org.json4s._
import org.json4s.native.Serialization._

import scala.language.{implicitConversions, postfixOps}

object ElasticSearchActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticSearchActor]

  case class IndexDocument(index: String, `type`: String, any: AnyRef)

}

class ElasticSearchActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  private val config = ConfigFactory.load().getConfig("pistraw.elasticsearch")

  private val directory = {
    val file = new File(config.getString("data-directory"))
    if (!file.exists()) file.mkdirs()
    file
  }

  private val settings = ImmutableSettings.settingsBuilder
    .put("path.data", directory.getAbsolutePath)
    .put("cluster.name", config.getString("cluster-name"))
    .build

  private lazy val node = nodeBuilder.local(true).settings(settings).build
  private lazy val client = node.client()

  def receive = {
    case Start => node.start()

    case Shutdown => node.close()

    case IndexDocument(ind, typ, any) =>
      implicit val format = DefaultFormats + new OffsetDateTimeSerializer
      client.prepareIndex(ind, typ).setSource(write(any)).execute().actionGet()
  }
}

class OffsetDateTimeSerializer extends Serializer[OffsetDateTime] {
  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case timestamp: OffsetDateTime => JString(timestamp.format(DateTimeFormatter.ISO_INSTANT))
  }

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), OffsetDateTime] = {
    case (_, timestamp: JString) => OffsetDateTime.parse(timestamp.values, DateTimeFormatter.ISO_INSTANT)
  }
}