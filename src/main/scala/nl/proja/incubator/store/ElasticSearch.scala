package nl.proja.incubator.store

import nl.proja.pishake.util.ActorSupport

trait ElasticSearch {
  this: ActorSupport =>

  lazy val elasticSearch = actorFor(ElasticSearchActor)
}
