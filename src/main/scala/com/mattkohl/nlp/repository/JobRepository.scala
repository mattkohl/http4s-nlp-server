package com.mattkohl.nlp.repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import com.mattkohl.nlp.model._
import doobie._
import doobie.implicits._

class JobRepository(transactor: Transactor[IO]) {

  def getJobs: Stream[IO, Job] = {
    sql"SELECT j.id, j.text FROM jobs AS j"
      .query[Job]
      .stream
      .transact(transactor)
  }

  def getJob(id: Long): IO[Either[JobNotFoundError.type, Job]] = {
    sql"""
         |SELECT j.id, j.text
         |FROM jobs AS j
         |WHERE j.id = $id
         |"""
      .query[Job]
      .option
      .transact(transactor)
      .map {
        case Some(job) => Right(job)
        case None => Left(JobNotFoundError)
      }
  }

  def createJob(job: Job): IO[Job] = {
    sql"INSERT INTO jobs (text) VALUES (${job.text})"
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor)
      .map { id =>
        job.copy(id = Some(id))
      }
  }

  def createToken(token: Token, job: Job): IO[Token] = {
    sql"INSERT INTO tokens (token, part_of_speech, job_id) VALUES (${token.token}, ${token.partOfSpeech}, ${job.id})"
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor)
      .map { id =>
        token.copy(id = Some(id))
      }
  }

  def deleteJob(id: Long): IO[Either[JobNotFoundError.type, Unit]] = {
    sql"DELETE FROM jobs AS j WHERE j.id = $id"
      .update
      .run
      .transact(transactor)
      .map { affectedRows =>
        if (affectedRows == 1) {
          Right(())
        } else {
          Left(JobNotFoundError)
        }
      }
  }

  def updateJob(id: Long, job: Job): IO[Either[JobNotFoundError.type, Job]] = {
    sql"UPDATE jobs AS j SET j.text = ${job.text} WHERE j.id = $id"
      .update
      .run
      .transact(transactor)
      .map { affectedRows =>
        if (affectedRows == 1) {
          Right(job.copy(id = Some(id)))
        } else {
          Left(JobNotFoundError)
        }
      }
  }
}
