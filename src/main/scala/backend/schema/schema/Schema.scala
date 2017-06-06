package backend.schema.schema

/**
  * Created by pzaytsev on 6/5/17.
  */

sealed trait Struct

class StructField(fieldName: String, dataType: , nullable: Boolean) extends Struct

object test extends App {

  val a: StructField[String] = new StructField("lol", String, false)



}