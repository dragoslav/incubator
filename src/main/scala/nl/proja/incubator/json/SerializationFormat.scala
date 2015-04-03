package nl.proja.incubator.json

import org.json4s._

object SerializationFormat {
  def apply(formats: SerializationFormat*) = formats.foldLeft(DefaultFormats: Formats)((f1, f2) => {
    val serializers = f2.customSerializers.foldLeft(f1)((s1, s2) => s1 + s2)
    val keySerializers = f2.customKeySerializers.foldLeft(serializers)((s1, s2) => s1 + s2)
    f2.fieldSerializers.foldLeft(keySerializers)((s1, s2) => s1 + s2)
  })
}

trait SerializationFormat {
  def customSerializers: List[Serializer[_]] = Nil

  def customKeySerializers: List[KeySerializer[_]] = Nil

  def fieldSerializers: List[FieldSerializer[_]] = Nil
}