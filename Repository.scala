package carecrate.repo

import carecrate.model.Identifiable
import scala.collection.mutable

class Repository[Id, T <: Identifiable[Id]]:
  private val data = mutable.LinkedHashMap.empty[Id, T]

  def all: List[T] = data.values.toList
  def find(id: Id): Option[T] = data.get(id)
  def upsert(entity: T): Unit = data.update(entity.id, entity)
  def delete(id: Id): Boolean = data.remove(id).isDefined
  def clear(): Unit = data.clear()
