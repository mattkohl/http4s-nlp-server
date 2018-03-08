package com.mattkohl.nlp

import cats.effect.IO
import fs2.Stream
import io.circe.Json
import io.circe.literal._
import com.mattkohl.nlp.model.Job
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{Request, Response, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import com.mattkohl.nlp.repository.JobRepository
import service.JobService

class ApiSpec extends WordSpec with MockFactory with Matchers {
  private val repository = stub[JobRepository]

  private val service = new JobService(repository).service

  "JobService" should {
    "create a job" in {
      val id = 1
      val job = Job(None, "my job")
      (repository.createJob _).when(job).returns(IO.pure(job.copy(id = Some(id))))
      val createJson = json"""
        {
          "text": ${job.text}
        }"""
      val response = serve(Request[IO](POST, uri("/jobs")).withBody(createJson).unsafeRunSync())
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "text": ${job.text}
        }"""
    }

    "update a job" in {
      val id = 1
      val job = Job(None, "updated job")
      (repository.updateJob _).when(id, job).returns(IO.pure(Right(job.copy(id = Some(id)))))
      val updateJson = json"""
        {
          "text": ${job.text}
        }"""

      val response = serve(Request[IO](PUT, Uri.unsafeFromString(s"/jobs/$id")).withBody(updateJson).unsafeRunSync())
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "text": ${job.text}
        }"""
    }

    "return a single job" in {
      val id = 1
      val job = Job(Some(id), "my job")
      (repository.getJob _).when(id).returns(IO.pure(Right(job)))

      val response = serve(Request[IO](GET, Uri.unsafeFromString(s"/jobs/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "text": ${job.text}
        }"""
    }

    "return all jobs" in {
      val id1 = 1
      val job1 = Job(Some(id1), "my job 1")
      val id2 = 2
      val job2 = Job(Some(id2), "my job 2")
      val jobs = Stream(job1, job2)
      (repository.getJobs _).when().returns(jobs)

      val response = serve(Request[IO](GET, uri("/jobs")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        [
         {
           "id": $id1,
           "text": ${job1.text}
         },
         {
           "id": $id2,
           "text": ${job2.text}
         }
        ]"""
    }

    "delete a job" in {
      val id = 1
      (repository.deleteJob _).when(id).returns(IO.pure(Right(())))

      val response = serve(Request[IO](DELETE, Uri.unsafeFromString(s"/jobs/$id")))
      response.status shouldBe Status.NoContent
    }
  }

  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }
}
