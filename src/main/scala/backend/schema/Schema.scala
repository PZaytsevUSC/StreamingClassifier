package backend.schema

/**
  * Created by pzaytsev on 6/5/17.
  */

import backend.types.{DataType, StringType}

sealed trait Struct

case class StructField(fieldName: String, dataType: DataType, nullable: Boolean) extends Struct {

  override def toString: String = s"StructField($fieldName,$dataType,$nullable)"

}

/**
  * Schema is user defined and is known for the connectors of the applications.
  * It is used to define a universal format of application and for parsing data.
  */

case class Schema(fields: Array[StructField]) extends DataType with Seq[StructField] {

  def this() = this(Array.empty[StructField])
  def fieldNames: Array[String] = fields.map(_.fieldName)

  override def iterator: Iterator[StructField] = fields.iterator
  override  def length: Int = fields.length

  override def defaultSize: Int = {
    if(fields.isEmpty) 0
    else {
      fields.reduce((x, y) => x.dataType.defaultSize + y.dataType.defaultSize)
    }
  }

  override def equals(that: Any): Boolean = {
    that match {
      case Schema(otherFields) =>
        java.util.Arrays.equals(
          fields.asInstanceOf[Array[AnyRef]], otherFields.asInstanceOf[Array[AnyRef]])
      case _ => false
    }
  }

  def ==(that: Any): Boolean = equals(that)

}