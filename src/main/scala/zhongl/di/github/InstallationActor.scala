package zhongl.di.github

import akka.actor.{Actor, ActorRefFactory, Props}
import zhongl.di.github.InstallationActor._
import zhongl.di.rest.{Api => RestApi}

class InstallationActor(createApi: ActorRefFactory => RestApi) extends Actor {

  private val api = createApi(context)

  override def receive = {
    case Test(repo, sha)    => test(repo, sha)
    case Release(repo, sha) => release(repo, sha)
  }

  private def release(repo: String, sha: String): Unit = ???

  private def test(repo: String, sha: String): Unit = ???
}

object InstallationActor {

  final case class Test(repositoryUri: String, sha: String)

  final case class Release(repositoryUri: String, sha: String)

  def props(restApi: ActorRefFactory => RestApi): Props = Props(new InstallationActor(restApi))
}
