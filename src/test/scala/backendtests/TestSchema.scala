package backendtests

import backend.schema.{Schema, StructField}
import backend.types.{FloatType, IntType, StringType}
import org.scalatest.FlatSpec

/**
  * Created by pzaytsev on 6/16/17.
  */
class TestSchema extends FlatSpec{

  "Schema" should "be created with parameters and without parameters" in {
    val schema = new Schema()
    assert(schema.length == 0)
    val structs = Array(StructField("name", StringType, true), StructField("age", IntType, true))
    val schema2 = new Schema(structs)
    assert(schema2.length == 2)

  }

  it should "be indexable" in {
    val structs = Array(StructField("name", StringType, true), StructField("age", IntType, true))
    val schema = new Schema(structs)
    val name = schema(0)
    assert(name.fieldName == "name")
  }

  it should "be iterable" in {
    val structs = Array(StructField("name", StringType, true), StructField("age", IntType, true))
    val it = structs.iterator
    val name = it.next()
    assert(name.fieldName == "name")
    val age = it.next()
    assert(age.fieldName == "age")
  }

  it should "be comparable" in {
    val schema1 = new Schema(Array(StructField("money", FloatType, true), StructField("age", IntType, true)))
    val schema2 = new Schema(Array(StructField("money", FloatType, true), StructField("age", IntType, true)))
    assert(schema1 == schema2)

    val schema3 = new Schema()
    assert(schema1 != schema3)
    val schema4 = new Schema(Array(StructField("age", IntType, true)))
    assert(schema2 != schema4)
  }

  it should "have a default size" in {
    val schema1 = new Schema(Array(StructField("money", FloatType, true), StructField("age", IntType, true)))
    assert(schema1.defaultSize == 8)
    val schema2 = new Schema()
    assert(schema2.defaultSize == 0)
  }

  it should "be extendable" in {
    val schema1 = new Schema(Array(StructField("money", FloatType, true), StructField("age", IntType, true)))
    val append = Array(StructField("name", StringType, true))
    val new_schema = schema1.add(append)
    assert(new_schema.length == 3)
    val new_schema2 = schema1.add(StructField("name", StringType, true))
    assert(new_schema.length == 3)
  }


}
