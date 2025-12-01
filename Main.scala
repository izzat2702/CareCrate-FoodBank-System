package carecrate

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, Alert}
import scalafx.scene.layout.{BorderPane, VBox, HBox}
import scalafx.geometry.Insets
import scalafx.scene.control.Alert

import carecrate.model._
import carecrate.repo.Repository
import carecrate.ui.{InventoryView, BeneficiariesView, DeliveriesView}
import carecrate.util.JsonStore

import java.time.LocalDate
import java.util.UUID
import java.nio.file.Paths

object Main extends JFXApp3 {
  override def start(): Unit = {

    // --- repositories + a couple of seed rows ---
    val invRepo = new Repository[String, InventoryItem]
    invRepo.upsert(NonPerishable(UUID.randomUUID().toString, "Rice 5kg", 12))
    invRepo.upsert(Perishable(UUID.randomUUID().toString, "Milk 1L", 6, LocalDate.now().plusDays(5)))

    val benRepo = new Repository[String, Beneficiary]
    benRepo.upsert(Beneficiary(UUID.randomUUID().toString, "Aisyah Karim", 4, "No peanuts"))
    benRepo.upsert(Beneficiary(UUID.randomUUID().toString, "Mr Lim", 2, "Low sodium"))

    val delRepo = new Repository[String, Delivery]

    // where we store the snapshot
    val dataPath = Paths.get("carecrate-data.json")

    // small helper for info popups
    def info(titleTxt: String, msg: String): Unit =
      new Alert(Alert.AlertType.Information) {
        headerText = titleTxt 
        this.title = titleTxt
        contentText = msg
      }.showAndWait()

    // --- root + pages ---
    val rootPane = new BorderPane

    val mainBox = new VBox {
      spacing = 12
      padding = Insets(12)
      children = Seq(
        new Label("CareCrate: Zero Hunger helper") {
          style = "-fx-font-size: 16px; -fx-font-weight: bold;"
        },
        new Label("Use the navbar above to switch between pages. Start with Inventory to add items.")
      )
    }

    // page switchers
    def showHome(): Unit          = rootPane.center = mainBox
    def showInventory(): Unit     = rootPane.center = new InventoryView(invRepo)
    def showBeneficiaries(): Unit = rootPane.center = new BeneficiariesView(benRepo)
    def showDeliveries(): Unit    = rootPane.center = new DeliveriesView(invRepo, benRepo, delRepo)

    // top navbar (with Save/Load)
    val navBar = new HBox {
      spacing = 8
      padding = Insets(8)
      children = Seq(
        new Label("CareCrate"),
        new Button("Home")          { onAction = _ => showHome() },
        new Button("Inventory")     { onAction = _ => showInventory() },
        new Button("Beneficiaries") { onAction = _ => showBeneficiaries() },
        new Button("Deliveries")    { onAction = _ => showDeliveries() },
        new Button("Save JSON") {
          onAction = _ =>
            JsonStore.save(dataPath, invRepo, benRepo, delRepo)
            info("Saved", s"Wrote ${dataPath.toAbsolutePath}")
        },
        new Button("Load JSON") {
          onAction = _ =>
            if JsonStore.loadInto(dataPath, invRepo, benRepo, delRepo) then
              info("Loaded", s"Loaded ${dataPath.toAbsolutePath}")
              showInventory() // show updated data
            else
              info("No file", s"No data at ${dataPath.toAbsolutePath}")
        }
      )
    }

    rootPane.top = navBar
    rootPane.center = mainBox

    stage = new JFXApp3.PrimaryStage {
      title = "CareCrate — Food Bank Planner"
      scene = new Scene(900, 600) { root = rootPane }
    }
  }
}
