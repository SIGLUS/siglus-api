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

package org.siglus.siglusapi.dto;

import java.util.Comparator;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;

public final class StockCardLineItemDtoComparators {
  private static final Comparator<StockCardLineItemDto> BY_OCCURRED_DATE = new ByOccurredDate();
  private static final Comparator<StockCardLineItemDto> BY_PROCESSED_DATE = new ByProcessedDate();
  private static final Comparator<StockCardLineItemDto> BY_REASON_PRIORITY = new ByReasonPriority();

  private StockCardLineItemDtoComparators() {
    throw new UnsupportedOperationException();
  }

  public static Comparator<StockCardLineItemDto> byOccurredDate() {
    return BY_OCCURRED_DATE;
  }

  public static Comparator<StockCardLineItemDto> byProcessedDate() {
    return BY_PROCESSED_DATE;
  }

  public static Comparator<StockCardLineItemDto> byReasonPriority() {
    return BY_REASON_PRIORITY;
  }

  /**
   * Comparator that will use occurred date to compare instance of {@link StockCardLineItemDto}.
   */
  private static final class ByOccurredDate implements Comparator<StockCardLineItemDto> {

    @Override
    public int compare(StockCardLineItemDto left, StockCardLineItemDto right) {
      return left.getOccurredDate().compareTo(right.getOccurredDate());
    }

  }

  /**
   * Comparator that will use processed date to compare instance of {@link StockCardLineItemDto}.
   */
  private static final class ByProcessedDate implements Comparator<StockCardLineItemDto> {

    @Override
    public int compare(StockCardLineItemDto left, StockCardLineItemDto right) {
      return left.getProcessedDate()
          .compareTo(right.getProcessedDate());
    }

  }

  /**
   * Comparator that will use reason priority to compare instance of {@link StockCardLineItemDto}.
   * The stock card line item with higher reason priority will be first in the list.
   */
  private static final class ByReasonPriority implements Comparator<StockCardLineItemDto> {

    @Override
    public int compare(StockCardLineItemDto left, StockCardLineItemDto right) {
      int leftPriority = null != left.getReason()
          ? ReasonType.fromString(left.getReason().getType()).getPriority()
          : -1;

      int rightPriority = null != right.getReason()
          ? ReasonType.fromString(right.getReason().getType()).getPriority()
          : -1;

      // the minus at the beginning is use to reverse the Integer.compare method
      return -Integer.compare(leftPriority, rightPriority);
    }

  }
}
