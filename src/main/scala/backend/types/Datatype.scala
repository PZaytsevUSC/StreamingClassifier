package backend.types

/**
  * Created by pzaytsev on 6/5/17.
  */
abstract class Datatype {

  def defaultSize: Int
  private[types] type InternalType
}

// Enforce singleton pattern

class StringType private() extends Datatype {

  override def defaultSize = 20
  private[types] type InternalType = String
  private[types] val ordering = implicitly[Ordering[InternalType]]

}

object StringType extends StringType

class IntType private () extends Datatype {

  private[types] type InternalType = Int
  private[types] val numeric = implicitly[Numeric[Int]]
  private[types] val integral = implicitly[Integral[Int]]
  private[types] val ordering = implicitly[Ordering[InternalType]]
  override def defaultSize = 4

}

object IntType extends IntType

