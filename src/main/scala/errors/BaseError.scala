package errors

/**
  * @author Maxim Ochenashko
  */
trait BaseError {
  def reason: String
  def cause: Option[Throwable] = None
}
