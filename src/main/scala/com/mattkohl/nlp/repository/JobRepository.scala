package com.mattkohl.nlp.repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import com.mattkohl.nlp.model._
import doobie._
import doobie.implicits._

class JobRepository(transactor: Transactor[IO]) {

  def getJobs: Stream[IO, Job] = {
    sql"SELECT id, text FROM job".query[Job].stream.transact(transactor)
  }

  def getJob(id: Long): IO[Either[JobNotFoundError.type, Job]] = {
    sql"SELECT id, text FROM job WHERE id = $id".query[Job].option.transact(transactor).map {
      case Some(job) => Right(job)
      case None => Left(JobNotFoundError)
    }
  }

  def createJob(job: Job): IO[Job] = {
    sql"INSERT INTO job (text) VALUES (${job.text})".update.withUniqueGeneratedKeys[Long]("id").transact(transactor).map { id =>
      job.copy(id = Some(id))
    }
  }

  def deleteJob(id: Long): IO[Either[JobNotFoundError.type, Unit]] = {
    sql"DELETE FROM job WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(())
      } else {
        Left(JobNotFoundError)
      }
    }
  }

  def updateJob(id: Long, job: Job): IO[Either[JobNotFoundError.type, Job]] = {
    sql"UPDATE job SET text = ${job.text} WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(job.copy(id = Some(id)))
      } else {
        Left(JobNotFoundError)
      }
    }
  }
}
