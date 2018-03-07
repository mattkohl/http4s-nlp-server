package com.mattkohl.nlp

import java.util.UUID
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{PartOfSpeechAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.CoreMap

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


package object model {

  case class Token(token: String, partOfSpeech: String)

  case class Job(id: UUID, text: String, tokens: Option[List[Token]], parseTrees: Option[List[String]], status: Option[String])

  case object JobNotFoundError

  object Annotator {
    private val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, parse")
    private val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

    def getAnnotation(text: String): Try[Annotation] = Try(pipeline.process(text))

    def getSentencesAnnotations(annotation: Annotation): Try[List[CoreMap]] = Try(annotation.get(classOf[SentencesAnnotation]).asScala.toList)

    def buildParseTrees(sentencesAnnotations: List[CoreMap]): Try[List[Tree]] = Try(sentencesAnnotations.map(_.get(classOf[TreeAnnotation])))

    def tokenizeAndTag(annotation: Annotation): Try[List[Token]] = Try {
      val tokens = annotation.get(classOf[TokensAnnotation]).asScala.toList
      tokens.map { token =>
        val word = token.get(classOf[TextAnnotation])
        val pos = token.get(classOf[PartOfSpeechAnnotation])
        Token(word, pos)
      }
    }

    def annotate(t: Job): Either[String, Job] = {
      val piped: Try[Job] = for {
        annotation <- Annotator.getAnnotation(t.text)
        sentencesAnnotations <- Annotator.getSentencesAnnotations(annotation)
        parseTrees <- Annotator.buildParseTrees(sentencesAnnotations)
        tokens <- Annotator.tokenizeAndTag(annotation)
      } yield t.copy(tokens = Some(tokens), parseTrees = Some(parseTrees.map(_.toString)))
      piped match {
        case Success(result) => Right(result)
        case Failure(e) => Left(s"Annotation failed: $e")
      }
    }
  }
}
