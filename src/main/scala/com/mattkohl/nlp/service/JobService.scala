package com.mattkohl.nlp.service

import cats.effect.IO
import com.mattkohl.nlp.model._
import org.http4s.{HttpService, MediaType, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import com.mattkohl.nlp.repository.JobRepository
import io.circe.generic.auto._
import io.circe.syntax._
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.headers.{Location, `Content-Type`}

class JobService(repository: JobRepository) extends Http4sDsl[IO] {
  val service = HttpService[IO] {
    case GET -> Root / "jobs" =>
      Ok(Stream("[") ++ repository.getJobs.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "jobs" / LongVar(id) =>
      for {
        getResult <- repository.getJob(id)
        response <- jobResult(getResult)
      } yield response

    case req @ POST -> Root / "jobs" =>
      for {
        job <- req.decodeJson[Job]
        createdJob <- repository.createJob(job)
        response <- Created(createdJob.asJson, Location(Uri.unsafeFromString(s"/jobs/${createdJob.id.get}")))
      } yield response

    case req @ PUT -> Root / "jobs" / LongVar(id) =>
      for {
        job <-req.decodeJson[Job]
        updateResult <- repository.updateJob(id, job)
        response <- jobResult(updateResult)
      } yield response

    case DELETE -> Root / "jobs" / LongVar(id) =>
      repository.deleteJob(id).flatMap {
        case Left(JobNotFoundError) => NotFound()
        case Right(_) => NoContent()
      }
  }

  private def jobResult(result: Either[JobNotFoundError.type, Job]) = {
    result match {
      case Left(JobNotFoundError) => NotFound()
      case Right(job) => Ok(job.asJson)
    }
  }
}
