/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.requisition.domain.requisition;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.openlmis.requisition.domain.AvailableRequisitionColumnOption.DEFAULT;
import static org.openlmis.requisition.domain.OpenLmisNumberUtils.zeroIfNull;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.domain.AvailableRequisitionColumnOption;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.utils.Message;

@SuppressWarnings("PMD.TooManyMethods")
public final class LineItemFieldsCalculator {

  private LineItemFieldsCalculator() {
  }

  /**
   * Calculates beginningBalance (A) value of current requsition line item based on previous one
   * and returns it.
   * The formula is A = E + K
   * E = Stock on Hand
   * K = Approved Quantity
   *
   * @param previous line item from previous requsition
   */
  public static int calculateBeginningBalance(RequisitionLineItem previous) {
    if (null != previous) {
      return zeroIfNull(previous.getStockOnHand());
    }
    return 0;
  }

  /**
   * Calculates TotalConsumedQuantity (C) value and returns it.
   * The formula is C = A + B (+/-) D - E
   * A = Beginning Balance
   * B = Total Received Quantity
   * C = Total Consumed Quantity
   * D = Total Losses/Adjustments
   * E = Stock on Hand
   */
  public static int calculateTotalConsumedQuantity(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity())
        + zeroIfNull(lineItem.getTotalLossesAndAdjustments())
        - zeroIfNull(lineItem.getStockOnHand());
  }

  /**
   * Calculates Total (Y) value and returns it.
   * The formula is Y = A + B
   * A = Beginning Balance
   * B = Total Received Quantity
   */
  public static int calculateTotal(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity());
  }

  /**
   * Calculates StockOnHand (E) value and returns it.
   * The formula is E = A + B (+/-) D - C
   * A = Beginning Balance
   * B = Total Received Quantity
   * C = Total Consumed Quantity
   * D = Total Losses/Adjustments
   * E = Stock on Hand
   */
  public static int calculateStockOnHand(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity())
        + zeroIfNull(lineItem.getTotalLossesAndAdjustments())
        - zeroIfNull(lineItem.getTotalConsumedQuantity());
  }

  /**
   * Calculates TotalLossesAndAdjustments (D) value and returns it.
   * The property is calculated by taking all item's StockAdjustments and adding their quantities.
   * Values, whose StockAdjustmentReasons are additive, count as positive, and negative otherwise.
   */
  public static int calculateTotalLossesAndAdjustments(RequisitionLineItem lineItem,
      Collection<StockAdjustmentReason> reasons) {
    int totalLossesAndAdjustments = 0;
    if (null != lineItem.getStockAdjustments()) {
      for (StockAdjustment adjustment : lineItem.getStockAdjustments()) {
        Optional<StockAdjustmentReason> reason = reasons
            .stream()
            .filter(r -> r.getReasonId().equals(adjustment.getReasonId()))
            .findFirst();

        if (reason.isPresent()) {
          int sign = reason.get().isCreditReasonType() ? 1 : -1;

          totalLossesAndAdjustments += adjustment.getQuantity() * sign;
        }
      }
    }
    return totalLossesAndAdjustments;
  }

  /**
   * Calculates the total cost of the requisition line item, by multiplying price per pack
   * and packs to ship. If either one is null, zero will be returned.
   *
   * @param lineItem the line item to calculate the value for
   * @return a {@link Money} object representing the total cost for this line
   */
  public static Money calculateTotalCost(RequisitionLineItem lineItem,
      ProgramOrderableDto programOrderable, CurrencyUnit currencyUnit) {
    Money pricePerPack = programOrderable.getPricePerPack();

    if (pricePerPack == null) {
      pricePerPack = Money.of(currencyUnit, RequisitionLineItem.PRICE_PER_PACK_IF_NULL);
    }

    long packsToShip = zeroIfNull(lineItem.getPacksToShip());

    return pricePerPack.multipliedBy(packsToShip);
  }

  /**
   * Calculates Adjusted Consumption (N) value and returns it.
   * <p>
   *   The calculations used can be different depending on Additional Quantity Required column
   *   being enabled or disabled in the requisition template.
   * </p>
   *
   * <p>
   *   When Additional Quantity Required field is not enabled in template,
   *   The formula used is N = RoundUp(C * ((M * 30) / ((M * 30) - X)))
   *   If Total Stockout Days is zero, the formula used is N = C
   * </p>
   *
   * <p>
   *   When Additional Quantity Required column is enabled in the template,
   *   The formula used is N = (RoundUp(C * ((M * 30) / ((M * 30) - X))) + Z)
   *   If Total Stockout Days is zero, the formula used is N = C + Z
   * </p>
   *
   * <p>
   *   C = Total Consumed Quantity
   *   M = Months in the period (integer)
   *   N = Adjusted Consumption
   *   X = Total Stockout Days
   *   Z = Additional Quantity Required
   * </p>
   *
   * @param lineItem the line item to calculate the value for
   * @param monthsInThePeriod number of months in period
   * @param additionalQuantityColumnPresent Boolean value weather additional quantity required
   *                                         column is enabled on the requisition template
   * @return an integer with the value of calculated adjusted consumption
   */
  public static int calculateAdjustedConsumption(RequisitionLineItem lineItem,
                                                 int monthsInThePeriod,
                                                 Boolean additionalQuantityColumnPresent) {
    return calculateAdjustedConsumptionValue(lineItem.getTotalConsumedQuantity(),
        lineItem.getTotalStockoutDays(), monthsInThePeriod,
        additionalQuantityColumnPresent ? lineItem.getAdditionalQuantityRequired() : null);
  }

  /**
   * Calculates Average Consumption (N) value and returns it.
   * The formula is
   * P = (N<sub>t0</sub> + N<sub>t-1</sub> + N<sub>t-2</sub> + N<sub>t-(n-1)</sub>) / (n).
   * N = Adjusted Consumption.
   * P = Average Consumption.
   * n = number of periods to be averaged.
   * t = indicates relative period (t0 = current reporting period).
   * If no previous periods, and there is only t0 formula is P = N.
   * If one previous period, so t0 and t-1 formula is
   * P = Roundup( (N<sub>t0</sub> + N<sub>t-1</sub>) / 2).
   */
  public static int calculateAverageConsumption(List<Integer> adjustedConsumptions) {
    int numberOfPeriods = adjustedConsumptions.size();
    if (numberOfPeriods == 1) {
      return adjustedConsumptions.get(0);
    }

    if (numberOfPeriods == 2) {
      return (int) Math.ceil((adjustedConsumptions.get(0) + adjustedConsumptions.get(1)) / 2.0);
    }

    int sum = adjustedConsumptions.stream().reduce(0, Integer::sum);

    return (int) Math.ceil((double)sum / numberOfPeriods);
  }

  /**
   * Calculates Maximum Stock Quantity (H) value and returns it.
   * The formula depends on selected option:
   * default => P * MaxPeriodsOfStock
   * P = Average Consumption
   *
   * @param line     the line item to calculate the value for.
   * @param template template related with the requisition.
   * @return a {@link Integer} object representing the maximum stock quantity for this line.
   */
  public static int calculateMaximumStockQuantity(RequisitionLineItem line,
      RequisitionTemplate template, Double maxPeriodsOfStockValue) {
    RequisitionTemplateColumn column = template
        .findColumn(RequisitionLineItem.MAXIMUM_STOCK_QUANTITY);
    AvailableRequisitionColumnOption option = column.getOption();
    String optionName = null != option
        ? defaultIfBlank(option.getOptionName(), DEFAULT)
        : DEFAULT;

    // currently we only support default option for this column. When new option will be added
    // this condition should be removed/updated
    if (!DEFAULT.equalsIgnoreCase(optionName)) {
      throw new IllegalArgumentException(
          "Unsupported option for maximum stock quantity: " + optionName
      );
    }

    int averageConsumption = zeroIfNull(line.getAverageConsumption());
    BigDecimal maxPeriodsOfStock = zeroIfNull(BigDecimal.valueOf(maxPeriodsOfStockValue));

    return BigDecimal.valueOf(averageConsumption)
        .multiply(maxPeriodsOfStock)
        .setScale(0, BigDecimal.ROUND_HALF_UP)
        .intValue();
  }

  /**
   * Calculates Calculated Order Quantity (I) value and returns it.
   * The formula is
   * I = H - E
   *
   * @param line the line item to calculate the value for.
   * @return a {@link Integer} object representing the Calculated Order Quantity for this line.
   */
  public static int calculateCalculatedOrderQuantity(RequisitionLineItem line,
      RequisitionTemplate template, Double maxPeriodsOfStockValue) {
    Integer maximumStockQuantity = line.getMaximumStockQuantity();
    Integer stockOnHand = line.getStockOnHand();

    if (null == maximumStockQuantity) {
      maximumStockQuantity = calculateMaximumStockQuantity(line, template, maxPeriodsOfStockValue);
    }

    if (null == stockOnHand) {
      stockOnHand = calculateStockOnHand(line);
    }

    return Math.max(0, zeroIfNull(maximumStockQuantity) - zeroIfNull(stockOnHand));
  }

  /**
   * Calculates Calculated Order Quantity ISA (S) value and returns it.
   * The formula is
   * I = G - E
   *
   * @param line the line item to calculate the value for.
   * @return a {@link Integer} object representing the Calculated Order Quantity ISA for this line.
   */
  public static Integer calculateCalculatedOrderQuantityIsa(RequisitionLineItem line) {
    if (null == line.getIdealStockAmount()) {
      return null;
    }

    return Math.max(0, line.getIdealStockAmount() - zeroIfNull(line.getStockOnHand()));
  }

  /**
   * Returns skipped column value from requisition line item
   * This method assumes that the product was skipped if no value was found.
   *
   * @param  currentLineItem {@link RequisitionLineItem}
   * @param  previousLineItem {@link RequisitionLineItem}
   * @return {@link Boolean}
   */
  public static Boolean canSkipLineItem(RequisitionLineItem currentLineItem,
                                        RequisitionLineItem previousLineItem) {
    // check if there is any non-zero column value in current lineItem. if so,
    // skipping is not allowed.
    if (hasNonZeroStockValue(currentLineItem)) {
      return false;
    }
    if (previousLineItem == null) {
      return true;
    }
    return previousLineItem.getSkipped();
  }

  /**
   * Sets value to Total Consumed Quantity column based on stock range summaries.
   */
  public static Integer calculateStockBasedTotalConsumedQuantity(RequisitionTemplate template,
      StockCardRangeSummaryDto summaryDto, UUID orderableId) {
    RequisitionTemplateColumn column = template.findColumn(TOTAL_CONSUMED_QUANTITY);
    int value = 0;
    if (null != summaryDto) {
      value = summaryDto.getTagAmount(column.getTag());
      if (value > 0) {
        throw new ValidationMessageException(new Message(
            MessageKeys.ERROR_VALIDATION_NON_NEGATIVE_NUMBER, TOTAL_CONSUMED_QUANTITY,
            orderableId));
      }
    }
    return Math.abs(value);
  }

  /**
   * Sets value to Total Received Quantity column based on stock range summaries.
   */
  public static Integer calculateStockBasedTotalReceivedQuantity(RequisitionTemplate template,
      StockCardRangeSummaryDto summaryDto, UUID orderableId) {
    RequisitionTemplateColumn column = template.findColumn(TOTAL_RECEIVED_QUANTITY);
    int value = 0;
    if (null != summaryDto) {
      value = summaryDto.getTagAmount(column.getTag());
      if (value < 0) {
        throw new ValidationMessageException(new Message(
            MessageKeys.ERROR_VALIDATION_NON_POSITIVE_NUMBER, TOTAL_RECEIVED_QUANTITY,
            orderableId.toString()));
      }
    }
    return value;
  }

  /**
   * Sets value to Total Stockout Days column based on stock range summaries.
   */
  public static Integer calculateStockBasedTotalStockoutDays(
      StockCardRangeSummaryDto summaryDto, Integer numberOfMonthsInPeriod) {
    int value = 0;
    if (null != summaryDto) {
      value = summaryDto.getStockOutDays();
    }
    return Math.min(value, 30 * (null == numberOfMonthsInPeriod ? 1 : numberOfMonthsInPeriod));
  }

  /**
   * Sets value to Total Losses and Adjustments column based on stock range summaries.
   */
  public static Integer calculateStockBasedTotalLossesAndAdjustments(RequisitionTemplate template,
      StockCardRangeSummaryDto summaryDto) {
    RequisitionTemplateColumn column = template.findColumn(TOTAL_LOSSES_AND_ADJUSTMENTS);
    int value = 0;
    if (null != summaryDto) {
      value = summaryDto.getTagAmount(column.getTag());
    }
    return value;
  }

  /**
   * Calculates Average Consumption column value based on stock range summaries and returns it.
   *
   * @param summaryDto the stock card range summary to calculate Total Consumed Quantity
   *                   and Adjusted Consumption values
   * @param orderableId the UUID of orderable to calculate Total Consumed Quantity value for
   * @param template the requisition template
   * @param periods the periods to calculate Adjusted Consumption value
   * @param additionalQuantityRequired the value of Additional Quantity Required
   * @return a {@link Integer} object representing the Average Consumption value.
   */
  public static Integer calculateStockBasedAverageConsumption(
      StockCardRangeSummaryDto summaryDto, UUID orderableId, RequisitionTemplate template,
      List<ProcessingPeriodDto> periods, Integer additionalQuantityRequired) {
    int value = 0;
    if (null != summaryDto) {
      Integer totalConsumedQuantity =
          calculateStockBasedTotalConsumedQuantity(template, summaryDto, orderableId);
      int adjustedConsumption = calculateAdjustedConsumptionValue(
          totalConsumedQuantity,
          summaryDto.getStockOutDays(),
          periods.stream().mapToInt(ProcessingPeriodDto::getDurationInMonths).sum(),
          additionalQuantityRequired);
      value = (int) Math.ceil((double) adjustedConsumption / periods.size());
    }
    return value;
  }

  private static int calculateAdjustedConsumptionValue(Integer totalConsumedQuantity,
      Integer totalStockoutDays, int monthsInThePeriod, Integer additionalQuantityRequired) {
    int consumedQuantity = zeroIfNull(totalConsumedQuantity);

    if (consumedQuantity == 0) {
      return 0;
    }

    final int totalDays = 30 * monthsInThePeriod;
    final int stockoutDays = zeroIfNull(totalStockoutDays);
    final int nonStockoutDays = totalDays - stockoutDays;

    if (nonStockoutDays == 0) {
      return consumedQuantity;
    }

    BigDecimal divide = new BigDecimal(totalDays)
        .divide(new BigDecimal(nonStockoutDays), 1000, BigDecimal.ROUND_HALF_UP);

    BigDecimal adjustedConsumption = new BigDecimal(consumedQuantity)
        .multiply(divide)
        .setScale(0, RoundingMode.CEILING);

    if (null != additionalQuantityRequired && additionalQuantityRequired > 0) {
      return adjustedConsumption.intValue() + additionalQuantityRequired;
    }
    return adjustedConsumption.intValue();
  }

  private static boolean hasNonZeroStockValue(RequisitionLineItem currentLineItem) {
    if (currentLineItem == null) {
      return false;
    }
    Integer nonZeroValues = zeroIfNull(currentLineItem.getBeginningBalance())
        + zeroIfNull(currentLineItem.getStockOnHand())
        + zeroIfNull(currentLineItem.getTotalConsumedQuantity())
        + zeroIfNull(currentLineItem.getTotalReceivedQuantity())
        + Math.abs(zeroIfNull(currentLineItem.getTotalLossesAndAdjustments()));

    return (nonZeroValues > 0);
  }
}
