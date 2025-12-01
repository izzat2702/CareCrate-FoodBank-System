# CareCrate — Food Bank Planner (ScalaFX)

Small desktop app that helps a food bank keep track of **inventory**, **beneficiaries**, and simple **deliveries**.  
Built with **Scala 3 + ScalaFX**.

---

## Why this project?
- Food banks often lose track of **expiry** and **low-stock** items.
- Volunteers need a quick way to **register beneficiaries** and **pack deliveries**.
- Good fit to demonstrate **traits, inheritance, generics, and event-driven GUIs** in Scala.

---

## Quick start

**Requirements:** JDK 21+, SBT

```bash
# from the project root
sbt run
```

### What to click
Top navbar:
- **Inventory** – add perishable/non-perishable items, delete, see expiry status
- **Beneficiaries** – add / edit / delete people + diet notes
- **Deliveries** – pick a beneficiary, choose item + qty, **Add to cart**, then **Confirm delivery** (stock is deducted)
- **Save JSON** – write snapshot to `carecrate-data.json`
- **Load JSON** – load snapshot back into the app

---

## Core features
- Inventory view with **Perishable**/**NonPerishable** + expiry status
- Beneficiaries with **household size** and **diet notes**
- Deliveries: cart workflow with **qty validation** and **stock deduction**
- **Save / Load** the whole app state to a single JSON file (uPickle)

---

## Data model (short version)
- `InventoryItem` (trait) → `Perishable(expiry)` and `NonPerishable`
- `Beneficiary(id, name, householdSize, dietNotes)`
- `Delivery(id, date, route, beneficiaryId, items: List[DeliveryItem(itemId, qty)])`

---

## Tech choices
- **GUI:** ScalaFX 21 (layouts, tables, forms, dialogs)
- **JSON:** uPickle (third-party lib; small and type-safe)
- **Build:** SBT, Java 21, Scala 3

---

## Folder structure
```
src/main/scala/carecrate/
  Main.scala                   # navbar + page switching + Save/Load
  model/Domain.scala           # Perishable, NonPerishable, Beneficiary, Delivery, etc.
  repo/Repository.scala        # tiny in-memory repo
  ui/InventoryView.scala
  ui/BeneficiariesView.scala
  ui/DeliveriesView.scala
  util/JsonStore.scala         # uPickle + save/load snapshot
build.sbt
README.md
```
---

## Project status / roadmap
- [x] Scaffold ScalaFX app (`Main.scala`)
- [x] Inventory CRUD + alerts
- [x] Beneficiaries
- [x] Deliveries (deduct stock)
- [x] JSON import/export (uPickle)
- [x] UML block (in report)
- [x] Screenshots + short demo video

---

## Credits
- ScalaFX docs
- uPickle docs (for the JSON codecs)

