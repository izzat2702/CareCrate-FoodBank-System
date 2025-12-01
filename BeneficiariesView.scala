package carecrate.ui

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{TableView, TableColumn, Label, Button, TextInputDialog, SelectionMode, Alert}
import scalafx.scene.layout.{BorderPane, HBox, VBox}
import scalafx.beans.property.ReadOnlyStringWrapper

import carecrate.model.Beneficiary
import carecrate.repo.Repository

import java.util.UUID

final class BeneficiariesView(repo: Repository[String, Beneficiary]) extends BorderPane {

  private val rows  = ObservableBuffer[Beneficiary]()
  private val table = new TableView[Beneficiary](rows) {
    columnResizePolicy = TableView.ConstrainedResizePolicy
    selectionModel().selectionMode = SelectionMode.Single

    columns ++= Seq(
      new TableColumn[Beneficiary, String] {
        text = "Name"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.name)
      },
      new TableColumn[Beneficiary, String] {
        text = "Household"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.householdSize.toString)
      },
      new TableColumn[Beneficiary, String] {
        text = "Diet notes"
        cellValueFactory = f => ReadOnlyStringWrapper(f.value.dietNotes)
      }
    )
  }

  private val addBtn  = new Button("Add")
  private val editBtn = new Button("Edit Selected")
  private val delBtn  = new Button("Delete Selected")

  addBtn.onAction  = _ => addDialog()
  editBtn.onAction = _ => editSelected()
  delBtn.onAction  = _ => deleteSelected()

  top = new VBox {
    padding = Insets(10)
    spacing = 6
    children = Seq(new Label("Beneficiaries"))
  }
  center = table
  bottom = new HBox {
    spacing = 8
    padding = Insets(10)
    children = Seq(addBtn, editBtn, delBtn)
  }

  reload()

  private def reload(): Unit =
    rows.setAll(repo.all*)

  private def addDialog(): Unit =
    val name    = prompt("Name?")
    val hhStr   = prompt("Household size?")
    val diet    = prompt("Diet notes? (optional)").orElse(Some(""))

    for
      n <- name
      h <- hhStr.flatMap(_.toIntOption)
      d <- diet
    do
      repo.upsert(Beneficiary(UUID.randomUUID().toString, n, h, d))
      reload()

  private def editSelected(): Unit =
    val b = table.selectionModel().getSelectedItem
    if b == null then
      info("No selection", "Please select a beneficiary to edit.")
    else
      val name = prompt(s"Name? (current: ${b.name})").orElse(Some(b.name))
      val hh   = prompt(s"Household size? (current: ${b.householdSize})").flatMap(_.toIntOption).orElse(Some(b.householdSize))
      val diet = prompt(s"Diet notes? (current: ${b.dietNotes})").orElse(Some(b.dietNotes))

      for n <- name; h <- hh; d <- diet do
        repo.upsert(b.copy(name = n, householdSize = h, dietNotes = d))
        reload()

  private def deleteSelected(): Unit =
    val b = table.selectionModel().getSelectedItem
    if b == null then
      info("No selection", "Please select a beneficiary to delete.")
    else
      repo.delete(b.id)
      reload()

  private def prompt(msg: String): Option[String] =
    val d = new TextInputDialog(defaultValue = "")
    d.setHeaderText(msg)
    d.showAndWait().map(_.trim).filter(_.nonEmpty)

  private def info(titleTxt: String, msg: String): Unit =
    new Alert(Alert.AlertType.Information) {
      title = titleTxt
      headerText = titleTxt
      contentText = msg
    }.showAndWait()
}

