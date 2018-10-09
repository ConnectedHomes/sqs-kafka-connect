import sbt.Keys._
import sbt._
import sbt.plugins.IvyPlugin

object LoadCreds extends AutoPlugin {

  override def requires: Plugins = IvyPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val environmentRealm = settingKey[String]("Realm used for environment derived credentials")

    lazy val discoveredCredentials = taskKey[Seq[Credentials]](
      s"""Gathers Ivy2 credentials from:
         | - .credentials* files under '${Path.userHome}/.ivy2 (global)'
         | - .credentials* files under '$$baseDirectory/.ivy2 (project specific)'
         | _ constructed from the 'IVY_USR' and 'IVY_PSW' environment variables, if both present
         |""".stripMargin
    )
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(

    environmentRealm := "Artifactory Realm",

    discoveredCredentials := {

      val log = streams.value

      val fileCredentials = Seq(
        ((Path.userHome / ".ivy2") * ".credentials*").get,
        ((baseDirectory.value / ".ivy2") * ".credentials*").get
      ) flatMap { x => x map { Credentials(_) } }

      val envCredential = for {
        user <- sys.env.get("IVY_USR")
        pass <- sys.env.get("IVY_PSW")
        host = sys.env.getOrElse("IVY_HOST", "bgchops.jfrog.io")
      } yield {
        log.log(s"Credential loaded from environment for realm $environmentRealm")
        Credentials(environmentRealm.value, host, user, pass)
      }

      fileCredentials collect { case cred: FileCredentials => log.log(s"Loaded credentials from ${cred.path}") }

      envCredential match {
        case Some(cred) => cred +: fileCredentials
        case _ => fileCredentials
      }
    },

    credentials := discoveredCredentials.value
  )
}
