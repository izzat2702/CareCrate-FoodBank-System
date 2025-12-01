package carecrate.ui

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{TableView, TableColumn, Label, Button, TextInputDialog, SelectionMode, Alert}
import scalafx.scene.layout.{BorderPane, HBox, VBox}
import scalafx.beans.property.ReadOnlyStringWrapper

import carecrate.model.{InventoryItem, Perishable, NonPerishable}
import carecrate.repo.Repository
import carecrate.util.DateUtil

import java.time.LocalDate
import java.util.UUID

final class InventoryView(invRepo: Repository[String, InventoryItem]) extends BorderPane {

  private val rows  = ObservableBuffer[InventoryItem]()
  private val table = new TableView[InventoryItem](rows) {
    columnResizePolicy = TableView.ConstrainedResizePolicy
    selectionModel().selectionMode = SelectionMode.Single

    columns ++= Seq(
      new TableColumn[InventoryItem, String] {
        text = "Name"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.name)
      },
      new TableColumn[InventoryItem, String] {
        text = "Qty"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.qty.toString)
      },
      new TableColumn[InventoryItem, String] {
        text = "Kind"
        cellValueFactory = f =>
          ReadOnlyStringWrapper(
            f.value match
              case _: Perishable    => "Perishable"
              case _: NonPerishable => "Non-perishable"
          )
      },
      new TableColumn[InventoryItem, String] {
        text = "Expiry"
        cellValueFactory = f =>
          ReadOnlyStringWrapper(
            f.value match
              case p: Perishable    => p.expiry.toString
              case _: NonPerishable => "-"
          )
      },
      new TableColumn[InventoryItem, String] {
        text = "Status"
        cellValueFactory = f =>
          val it = f.value
          val status =
            it match
              case p: Perishable if DateUtil.isExpiringSoon(p.expiry) => "EXPIRING"
              case _ if DateUtil.isLowStock(it.qty)                   => "LOW"
              case _                                                   => ""
          ReadOnlyStringWrapper(status)
      }
    )
  }

  private val addPerishableBtn    = new Button("Add Perishable")
  private val addNonPerishableBtn = new Button("Add Non-Perishable")
  private val deleteBtn           = new Button("Delete Selected")

  addPerishableBtn.onAction    = _ => addPerishableDialog()
  addNonPerishableBtn.onAction = _ => addNonPerishableDialog()
  deleteBtn.onAction           = _ => deleteSelected()

  top = new VBox {
    padding = Insets(10)
    spacing = 6
    children = Seq(new Label("Inventory"))
  }
  center = table
  bottom = new HBox {
    spacing = 8
    padding = Insets(10)
    children = Seq(addPerishableBtn, addNonPerishableBtn, deleteBtn)
  }

  reload()

  private def reload(): Unit =
    rows.setAll(invRepo.all*)

  private def addPerishableDialog(): Unit =
    val name    = prompt("Item name?")
    val qtyStr  = prompt("Quantity?")
    val daysStr = prompt("Expiry in how many days?")

    for
      n <- name
      q <- qtyStr.flatMap(_.toIntOption)
      d <- daysStr.flatMap(_.toIntOption)
    do
      invRepo.upsert(Perishable(UUID.randomUUID().toString, n, q, LocalDate.now().plusDays(d)))
      reload()

  private def addNonPerishableDialog(): Unit =
    val name   = prompt("Item name?")
    val qtyStr = prompt("Quantity?")

    for
      n <- name
      q <- qtyStr.flatMap(_.toIntOption)
    do
      invRepo.upsert(NonPerishable(UUID.randomUUID().toString, n, q))
      reload()

  private def deleteSelected(): Unit =
    val item = table.selectionModel().getSelectedItem
    if item == null then
      new Alert(Alert.AlertType.Information) {
        title = "Delete"
        headerText = "No selection"
        contentText = "Please select an item to delete."
      }.showAndWait()
    else
      invRepo.delete(item.id)
      reload()

  private def prompt(msg: String): Option[String] =
    val d = new TextInputDialog(defaultValue = "")
    d.setHeaderText(msg)
    d.showAndWait().map(_.trim).filter(_.nonEmpty)

}
