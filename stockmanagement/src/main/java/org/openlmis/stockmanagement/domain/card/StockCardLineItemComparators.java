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

package org.openlmis.stockmanagement.domain.card;

import java.util.Comparator;

public final class StockCardLineItemComparators {
  private static final Comparator<StockCardLineItem> BY_OCCURRED_DATE = new ByOccurredDate();
  private static final Comparator<StockCardLineItem> BY_PROCESSED_DATE = new ByProcessedDate();
  private static final Comparator<StockCardLineItem> BY_REASON_PRIORITY = new ByReasonPriority();

  private StockCardLineItemComparators() {
    throw new UnsupportedOperationException();
  }

  public static Comparator<StockCardLineItem> byOccurredDate() {
    return BY_OCCURRED_DATE;
  }

  public static Comparator<StockCardLineItem> byProcessedDate() {
    return BY_PROCESSED_DATE;
  }

  public static Comparator<StockCardLineItem> byReasonPriority() {
    return BY_REASON_PRIORITY;
  }

  /**
   * Comparator that will use occurred date to compare instance of {@link StockCardLineItem}.
   */
  private static final class ByOccurredDate implements Comparator<StockCardLineItem> {

    @Override
    public int compare(StockCardLineItem left, StockCardLineItem right) {
      return left.getOccurredDate().compareTo(right.getOccurredDate());
    }

  }

  /**
   * Comparator that will use processed date to compare instance of {@link StockCardLineItem}.
   */
  private static final class ByProcessedDate implements Comparator<StockCardLineItem> {

    @Override
    public int compare(StockCardLineItem left, StockCardLineItem right) {
      return left.getProcessedDate().compareTo(right.getProcessedDate());
    }

  }

  /**
   * Comparator that will use reason priority to compare instance of {@link StockCardLineItem}.
   * The stock card line item with higher reason priority will be first in the list.
   */
  private static final class ByReasonPriority implements Comparator<StockCardLineItem> {

    @Override
    public int compare(StockCardLineItem left, StockCardLineItem right) {
      int leftPriority = null != left.getReason()
          ? left.getReason().getReasonType().getPriority()
          : -1;

      int rightPriority = null != right.getReason()
          ? right.getReason().getReasonType().getPriority()
          : -1;

      // the minus at the beginning is use to reverse the Integer.compare method
      return -Integer.compare(leftPriority, rightPriority);
    }

  }
}
