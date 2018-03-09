package com.mattkohl.nlp

import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.{PartOfSpeechAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.CoreMap

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


package object model {

  case class Job(id: Option[Long], text: String)

  case class Token(id: Option[Long], position: Int, token: String, partOfSpeech: String)

  case object JobNotFoundError

}
