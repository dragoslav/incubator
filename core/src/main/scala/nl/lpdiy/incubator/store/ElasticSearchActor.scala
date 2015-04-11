package nl.lpdiy.incubator.store

import java.io.File

import akka.actor._
import com.typesafe.config.ConfigFactory
import nl.lpdiy.incubator.Bootstrap
import nl.lpdiy.incubator.Bootstrap.{Shutdown, Start}
import Bootstrap.{Shutdown, Start}
import nl.lpdiy.incubator.json.{SerializationFormat, OffsetDateTimeSerializer}
import nl.lpdiy.incubator.json.SerializationFormat
import ElasticSearchActor.IndexDocument
import nl.lpdiy.pishake.util.{ActorDescription, ActorSupport, FutureSupport}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import org.json4s.native.Serialization._

import scala.language.{implicitConversions, postfixOps}

object ElasticSearchActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticSearchActor]

  case class IndexDocument(index: String, `type`: String, any: AnyRef)

}

class ElasticSearchActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  private val config = ConfigFactory.load().getConfig("incubator.elasticsearch")

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
      implicit val format = SerializationFormat(OffsetDateTimeSerializer)
      client.prepareIndex(ind, typ).setSource(write(any)).execute().actionGet()
  }
}
