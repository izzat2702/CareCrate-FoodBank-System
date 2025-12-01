package carecrate.ui

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, HBox, VBox, GridPane, Priority}
import scalafx.beans.property.ReadOnlyStringWrapper

import carecrate.model._
import carecrate.repo.Repository

import java.time.LocalDate
import java.util.UUID
import javafx.util.StringConverter   // JavaFX converter (works best with ComboBox)

final class DeliveriesView(
  invRepo: Repository[String, InventoryItem],
  benRepo: Repository[String, Beneficiary],
  delRepo: Repository[String, Delivery]
) extends BorderPane {

  // --- UI/state ---
  private case class CartLine(item: InventoryItem, qty: Int)
  private val cart = ObservableBuffer[CartLine]()

  // Beneficiaries dropdown
  private val benBox = new ComboBox[Beneficiary](ObservableBuffer(benRepo.all*))
  benBox.converter = new StringConverter[Beneficiary] {
    override def toString(b: Beneficiary): String = if (b == null) "" else b.name
    override def fromString(s: String): Beneficiary = null
  }

  // Inventory items dropdown
  private val itemBox = new ComboBox[InventoryItem](ObservableBuffer(invRepo.all*))
  itemBox.converter = new StringConverter[InventoryItem] {
    override def toString(it: InventoryItem): String = if (it == null) "" else it.name
    override def fromString(s: String): InventoryItem = null
  }

  private val qtyField   = new TextField { promptText = "Qty" }
  private val routeField = new TextField { promptText = "Route / notes (optional)" }

  // sizes so it doesn't look cramped
  benBox.prefWidth = 220
  itemBox.prefWidth = 220
  qtyField.prefWidth = 80
  routeField.prefWidth = 320

  private val cartTable = new TableView[CartLine](cart) {
    columnResizePolicy = TableView.ConstrainedResizePolicy
    selectionModel().selectionMode = SelectionMode.Single
    columns ++= Seq(
      new TableColumn[CartLine, String] {
        text = "Item"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.item.name)
      },
      new TableColumn[CartLine, String] {
        text = "Kind"
        cellValueFactory = f => ReadOnlyStringWrapper(
          f.value.item match {
            case _: Perishable    => "Perishable"
            case _: NonPerishable => "Non-perishable"
          }
        )
      },
      new TableColumn[CartLine, String] {
        text = "Qty"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.qty.toString)
      }
    )
  }

  private val addBtn     = new Button("Add to cart")
  private val removeBtn  = new Button("Remove selected")
  private val confirmBtn = new Button("Confirm delivery")

  addBtn.onAction = _ => addToCart()
  removeBtn.onAction = _ => {
    val sel = cartTable.selectionModel().getSelectedItem
    if (sel != null) cart -= sel
  }
  confirmBtn.onAction = _ => confirmDelivery()

  // --- layout ---
  top = new VBox {
    padding = Insets(10); spacing = 8
    children = Seq(
      new Label("Deliveries") { style = "-fx-font-size: 14px; -fx-font-weight: bold;" },
      new GridPane {
        hgap = 8; vgap = 6
        add(new Label("Beneficiary:"), 0, 0); add(benBox,     1, 0); add(addBtn, 3, 0)
        add(new Label("Item:"),        0, 1); add(itemBox,    1, 1)
        add(new Label("Qty:"),         2, 1); add(qtyField,   3, 1)
        add(new Label("Route / notes:"),0, 2); add(routeField, 1, 2, 3, 1)
      }
    )
  }

  VBox.setVgrow(cartTable, Priority.Always)
  center = cartTable

  bottom = new HBox {
    spacing = 8; padding = Insets(10); alignment = Pos.CenterRight
    children = Seq(removeBtn, confirmBtn)
  }

  // --- actions ---
  private def addToCart(): Unit = {
    val maybeItem = Option(itemBox.value.value)
    val qtyOpt    = qtyField.text.value.trim.toIntOption

    (maybeItem, qtyOpt) match {
      case (Some(it), Some(qty)) if qty > 0 =>
        val current = it.qty
        if (qty > current) {
          info("Not enough stock", s"'${it.name}' has only $current available.")
        } else {
          val idx = cart.indexWhere(_.item.id == it.id)
          if (idx >= 0) {
            val line = cart(idx)
            val newQ = line.qty + qty
            if (newQ > current) info("Not enough stock", s"'${it.name}' has only $current available.")
            else cart.update(idx, line.copy(qty = newQ))
          } else cart += CartLine(it, qty)
          qtyField.clear()
        }
      case _ =>
        info("Missing input", "Pick an item and enter a positive quantity.")
    }
  }

  private def confirmDelivery(): Unit = {
    val benOpt = Option(benBox.value.value)
    if (benOpt.isEmpty) { info("No beneficiary", "Select a beneficiary first."); return }
    if (cart.isEmpty)   { info("Empty cart", "Add at least one item."); return }

    val lines = cart.toList

    // validate first (no returns inside loop)
    var valid = true
    for (line <- lines) {
      invRepo.find(line.item.id) match {
        case Some(it) if line.qty <= it.qty => ()
        case Some(it) =>
          info("Not enough stock", s"'${it.name}' has only ${it.qty} left.")
          valid = false
        case None =>
          info("Item missing", s"'${line.item.name}' no longer exists in inventory.")
          valid = false
      }
    }

    if (!valid) {
      () // stop; messages already shown
    } else {
      // deduct
      for (line <- lines) {
        invRepo.find(line.item.id).foreach { it =>
          val newQty = it.qty - line.qty
          val updated = it match {
            case p: Perishable    => p.copy(qty = newQty)
            case n: NonPerishable => n.copy(qty = newQty)
          }
          invRepo.upsert(updated)
        }
      }

      // record delivery
      val delivery = Delivery(
        id = UUID.randomUUID().toString,
        date = LocalDate.now(),
        route = routeField.text.value,
        beneficiaryId = benOpt.get.id,
        items = lines.map(l => DeliveryItem(l.item.id, l.qty))
      )
      delRepo.upsert(delivery)

      // reset UI & refresh item list
      cart.clear()
      itemBox.items = ObservableBuffer(invRepo.all*)
      info("Success", "Delivery recorded and stock updated.")
    }
  }

  // --- helper ---
  private def info(titleTxt: String, msg: String): Unit =
    new Alert(Alert.AlertType.Information) {
      title = titleTxt; headerText = titleTxt; contentText = msg
    }.showAndWait()
}
