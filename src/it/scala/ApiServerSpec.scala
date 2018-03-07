import cats.effect.IO
import config.Config
import db.Database
import io.circe.Json
import io.circe.literal._
import org.http4s.circe._
import org.http4s.client.blaze.Http1Client
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import io.circe.optics.JsonPath._
import org.http4s.server.{Server => Http4sServer}
import org.http4s.server.blaze.BlazeBuilder
import repository.JobRepository
import service.JobService

class ApiServerSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  private lazy val client = Http1Client[IO]().unsafeRunSync()

  private lazy val config = Config.load("test.conf").unsafeRunSync()

  private lazy val urlStart = s"http://${config.server.host}:${config.server.port}"

  private val server = createServer().unsafeRunSync()

  override def afterAll(): Unit = {
    client.shutdown.unsafeRunSync()
    server.shutdown.unsafeRunSync()
  }

  "Job server" should {
    "create a job" in {
      val text = "It was all a dream"
      val createJson =json"""
        {
          "text": $text,
        }"""
      val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/jobs")).withBody(createJson).unsafeRunSync()
      val json = client.expect[Json](request).unsafeRunSync()
      root.id.long.getOption(json).nonEmpty shouldBe true
      root.text.string.getOption(json) shouldBe Some(text)
    }

    "update a job" in {
      val id = createJob("I used to read Word Up magazine")

      val text = "updated job"
      val updateJson = json"""
        {
          "text": $text,
        }"""
      val request = Request[IO](method = Method.PUT, uri = Uri.unsafeFromString(s"$urlStart/jobs/$id")).withBody(updateJson).unsafeRunSync()
      client.expect[Json](request).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "text": $text,
        }"""
    }

    "return a single job" in {
      val text = "Salt 'n' Pepa and Heavy D up in the limousine"
      val id = createJob(text)
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/jobs/$id")).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "text": $text
        }"""
    }

    "delete a job" in {
      val text = "Hangin pictures on my wall"
      val id = createJob(text)
      val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/jobs/$id"))
      client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent

      val getRequest = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/jobs/$id"))
      client.status(getRequest).unsafeRunSync() shouldBe Status.NotFound
    }

    "return all jobs" in {
      // Remove all existing jobs
      val json = client.expect[Json](Uri.unsafeFromString(s"$urlStart/jobs")).unsafeRunSync()
      root.each.id.long.getAll(json).foreach { id =>
        val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/jobs/$id"))
        client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent
      }

      // Add new jobs
      val text1 = "It was all a dream"
      val text2 = "I used to read Word Up magazine"
      val id1 = createJob(text1)
      val id2 = createJob(text2)

      // Retrieve jobs
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/jobs")).unsafeRunSync shouldBe json"""
        [
          {
            "id": $id1,
            "text": $text1
          },
          {
            "id": $id2,
            "text": $text2
          }
        ]"""
    }
  }

  private def createJob(text: String): Long = {
    val createJson =json"""
      {
        "text": $text
      }"""
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/jobs")).withBody(createJson).unsafeRunSync()
    val json = client.expect[Json](request).unsafeRunSync()
    root.id.long.getOption(json).nonEmpty shouldBe true
    root.id.long.getOption(json).get
  }

  private def createServer(): IO[Http4sServer[IO]] = {
    for {
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      repository = new JobRepository(transactor)
      server <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new JobService(repository).service, "/").start
    } yield server
  }
}
