package carecrate.model

import java.time.LocalDate

trait Identifiable[T]:
  def id: T

enum Role:
  case Admin, Volunteer

abstract class User(val id: String, val username: String, val role: Role) extends Identifiable[String]

sealed trait InventoryItem extends Identifiable[String]:
  def name: String
  def qty: Int

final case class Perishable(id: String, name: String, qty: Int, expiry: LocalDate) extends InventoryItem
final case class NonPerishable(id: String, name: String, qty: Int) extends InventoryItem

final case class Beneficiary(
  id: String,
  name: String,
  householdSize: Int,
  dietNotes: String
) extends Identifiable[String]

final case class DeliveryItem(itemId: String, qty: Int)
final case class Delivery(
  id: String,
  date: LocalDate,
  route: String,
  beneficiaryId: String,
  items: List[DeliveryItem]
) extends Identifiable[String]
