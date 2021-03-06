package backend.types

/**
  * Created by pzaytsev on 6/5/17.
  */
abstract class DataType {

  def defaultSize: Int
  private[types] type InternalType
}

// Enforce singleton pattern

class StringType private() extends DataType {

  override def defaultSize = 20
  private[types] type InternalType = String
  private[types] val ordering = implicitly[Ordering[InternalType]]

}



object StringType extends StringType

class IntType private () extends DataType {

  private[types] type InternalType = Int
  private[types] val numeric = implicitly[Numeric[Int]]
  private[types] val integral = implicitly[Integral[Int]]
  private[types] val ordering = implicitly[Ordering[InternalType]]
  override def defaultSize = 4

}

object IntType extends IntType


class FloatType private() extends DataType {
  private[types] type InternalType = Float
  override def defaultSize: Int = 4
  private[types] val numeric = implicitly[Numeric[Float]]
  private[types] val fractional = implicitly[Fractional[Float]]
  private[types] val ordering = implicitly[Ordering[InternalType]]

}

object FloatType extends FloatType