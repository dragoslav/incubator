package nl.lpdiy.incubator.store

import nl.lpdiy.pishake.util.ActorSupport

trait ElasticSearch {
  this: ActorSupport =>

  lazy val elasticSearch = actorFor(ElasticSearchActor)
}
