package is.hail.types.physical.stypes.concrete

import is.hail.annotations.Region
import is.hail.asm4s._
import is.hail.expr.ir.EmitCodeBuilder
import is.hail.types.physical.stypes.{SCode, SSettable, SType, SValue}
import is.hail.types.physical.{PCanonicalString, PString, PType}
import is.hail.types.physical.stypes.interfaces.{SBinaryCode, SString, SStringCode, SStringValue}
import is.hail.types.virtual.{TString, Type}
import is.hail.utils.FastIndexedSeq

case object SJavaString extends SString {
  override val virtualType: TString.type = TString

  override def storageType(): PType = PCanonicalString(false)

  override def copiedType: SType = this

  override def containsPointers: Boolean = false

  override def castRename(t: Type): SType = this

  override def _coerceOrCopy(cb: EmitCodeBuilder, region: Value[Region], value: SCode, deepCopy: Boolean): SJavaStringCode = {
    value.st match {
      case SJavaString => value.asInstanceOf[SJavaStringCode]
      case _ => new SJavaStringCode(value.asString.loadString())
    }
  }

  override def codeTupleTypes(): IndexedSeq[TypeInfo[_]] = FastIndexedSeq(classInfo[String])

  override def fromSettables(settables: IndexedSeq[Settable[_]]): SJavaStringSettable = {
    val IndexedSeq(s: Settable[String@unchecked]) = settables
    new SJavaStringSettable(s)
  }

  override def fromCodes(codes: IndexedSeq[Code[_]]): SJavaStringCode = {
    val IndexedSeq(s: Code[String@unchecked]) = codes
    new SJavaStringCode(s)
  }

  override def fromValues(values: IndexedSeq[Value[_]]): SJavaStringValue = {
    val IndexedSeq(s: Value[String@unchecked]) = values
    new SJavaStringValue(s)
  }

  override def constructFromString(cb: EmitCodeBuilder, r: Value[Region], s: Code[String]): SJavaStringCode = {
    new SJavaStringCode(s)
  }

  def construct(s: Code[String]): SJavaStringCode = new SJavaStringCode(s)
}

class SJavaStringCode(val s: Code[String]) extends SStringCode {
  def st: SString = SJavaString

  def makeCodeTuple(cb: EmitCodeBuilder): IndexedSeq[Code[_]] = FastIndexedSeq(s)

  def loadLength(): Code[Int] = s.invoke[Int]("length")

  def loadString(): Code[String] = s

  def toBytes(): SBinaryCode = {
    new SJavaBytesCode(s.invoke[Array[Byte]]("getBytes"))
  }

  private[this] def memoizeWithBuilder(cb: EmitCodeBuilder, name: String, sb: SettableBuilder): SValue = {
    val s = new SJavaStringSettable(sb.newSettable[String](s"${name}_javastring"))
    s.store(cb, this)
    s
  }

  def memoize(cb: EmitCodeBuilder, name: String): SValue = memoizeWithBuilder(cb, name, cb.localBuilder)

  def memoizeField(cb: EmitCodeBuilder, name: String): SValue = memoizeWithBuilder(cb, name, cb.fieldBuilder)
}

class SJavaStringValue(val s: Value[String]) extends SStringValue {
  override def st: SString = SJavaString

  override def get: SJavaStringCode = new SJavaStringCode(s)
}

object SJavaStringSettable {
  def apply(sb: SettableBuilder, name: String): SJavaStringSettable = {
    new SJavaStringSettable(sb.newSettable[String](s"${ name }_str"))
  }
}

final class SJavaStringSettable(override val s: Settable[String]) extends SJavaStringValue(s) with SSettable {
  override def settableTuple(): IndexedSeq[Settable[_]] = FastIndexedSeq(s)

  override def store(cb: EmitCodeBuilder, v: SCode): Unit = {
    cb.assign(s, v.asInstanceOf[SJavaStringCode].s)
  }
}