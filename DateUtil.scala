package carecrate.util

import java.time.{LocalDate, ZoneId}
import java.time.temporal.ChronoUnit

object DateUtil:
  def today: LocalDate = LocalDate.now(ZoneId.systemDefault())

  def daysUntil(date: LocalDate): Long =
    ChronoUnit.DAYS.between(today, date)

  def isLowStock(qty: Int, threshold: Int = 10): Boolean =
    qty <= threshold

  def isExpiringSoon(expiry: LocalDate, days: Int = 7): Boolean =
    daysUntil(expiry) <= days
