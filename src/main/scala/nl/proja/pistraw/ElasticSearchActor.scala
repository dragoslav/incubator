package nl.proja.pistraw

import java.io.File

import akka.actor._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.DocumentSource
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.util.{ActorDescription, ActorSupport, FutureSupport}
import nl.proja.pistraw.ElasticSearchActor.Store
import nl.proja.pistraw.PiStraw.{Shutdown, Start}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._

import scala.language.{implicitConversions, postfixOps}

object ElasticSearchActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticSearchActor]

  case class Store(index: String, any: AnyRef)

}

class ElasticSearchActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  import ObjectSource._

  private val config = ConfigFactory.load().getConfig("elasticsearch")

  private val directory = {
    val file = new File(config.getString("data-directory"))
    file.mkdirs()
    file
  }

  private val settings = ImmutableSettings.settingsBuilder
    .put("path.data", directory.getAbsolutePath)
    .put("cluster.name", config.getString("cluster-name"))
    .build

  private lazy val node = nodeBuilder.local(true).settings(settings).build

  private lazy val client = ElasticClient.fromNode(node)

  def receive = {
    case Start => node.start()

    case Shutdown => node.close()

    case Store(store, any) => client.execute(index into store doc any)
  }
}


object ObjectSource {
  val mapper = new ObjectMapper().findAndRegisterModules().registerModule(DefaultScalaModule)

  implicit def anyToObjectSource(any: Any): ObjectSource = new ObjectSource(any)
}

class ObjectSource(any: Any) extends DocumentSource {
  override def json: String = ObjectSource.mapper.writeValueAsString(any)
}