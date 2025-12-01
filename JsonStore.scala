package carecrate.util

import carecrate.model._
import carecrate.repo.Repository

import upickle.default._
import java.time.LocalDate
import java.nio.file.{Files, Path, StandardOpenOption}

// JSON codecs + helpers
object JsonStore {

  // LocalDate <-> String
  given rwLocalDate: ReadWriter[LocalDate] =
    readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse)

  // Inventory ADT
  given rwPerishable: ReadWriter[Perishable]       = macroRW
  given rwNonPerishable: ReadWriter[NonPerishable] = macroRW
  given rwInventoryItem: ReadWriter[InventoryItem] =
    ReadWriter.merge(rwPerishable, rwNonPerishable)

  // Other models
  given rwBeneficiary: ReadWriter[Beneficiary]   = macroRW
  given rwDeliveryItem: ReadWriter[DeliveryItem] = macroRW
  given rwDelivery: ReadWriter[Delivery]         = macroRW

  // App snapshot
  final case class AppState(
    inventory: List[InventoryItem],
    beneficiaries: List[Beneficiary],
    deliveries: List[Delivery]
  )
  given rwAppState: ReadWriter[AppState] = macroRW

  private def ensureParent(path: Path): Unit =
    val parent = path.getParent
    if parent != null && !Files.exists(parent) then Files.createDirectories(parent)

  def save(path: Path,
           invRepo: Repository[String, InventoryItem],
           benRepo: Repository[String, Beneficiary],
           delRepo: Repository[String, Delivery]): Unit =
    val state = AppState(invRepo.all, benRepo.all, delRepo.all)
    val json  = write(state, indent = 2)
    ensureParent(path)
    Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

  def load(path: Path): Option[AppState] =
    if Files.exists(path) then Some(read[AppState](Files.readString(path))) else None

  def loadInto(path: Path,
               invRepo: Repository[String, InventoryItem],
               benRepo: Repository[String, Beneficiary],
               delRepo: Repository[String, Delivery]): Boolean =
    load(path) match
      case Some(s) =>
        invRepo.all.foreach(i => invRepo.delete(i.id))
        benRepo.all.foreach(b => benRepo.delete(b.id))
        delRepo.all.foreach(d => delRepo.delete(d.id))
        s.inventory.foreach(invRepo.upsert)
        s.beneficiaries.foreach(benRepo.upsert)
        s.deliveries.foreach(delRepo.upsert)
        true
      case None => false
}

