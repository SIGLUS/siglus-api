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

package org.siglus.siglusapi.dto.android.enumeration;

import static org.siglus.siglusapi.constant.FacilityTypeConstants.AI;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DDM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DPM;

import java.util.UUID;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public enum MovementType {

  PHYSICAL_INVENTORY() {
    @Override
    public String getReason(String reasonName, Integer adjustment) {
      if (INVENTORY.equals(reasonName.trim())) {
        return INVENTORY;
      }
      if (adjustment < 0) {
        return INVENTORY_NEGATIVE;
      } else if (adjustment > 0) {
        return INVENTORY_POSITIVE;
      } else {
        return INVENTORY;
      }
    }

    @Override
    public UUID getInventoryReasonId(UUID programId, String reason) {
      return StockCardCreateContextHolder.getContext().findReasonId(programId, reason.trim())
          .orElseThrow(() -> new NotFoundException(NO_SUCH_REASON + reason));
    }
  },

  RECEIVE() {
    @Override
    public String getReason(String sourceName, Integer adjustment) {
      String newSourceName = sourceName;
      if (Source.UNPACK_FROM_KIT.name().equals(sourceName.trim())) {
        newSourceName = Source.UNPACK_FROM_KIT.getName();
      } else if (sourceName.contains(DDM)) {
        newSourceName = Source.DISTRICT_DDM.getName();
      } else if (sourceName.contains(DPM)) {
        newSourceName = Source.PROVINCE_DPM.getName();
      } else if (sourceName.contains(AI)) {
        newSourceName = Source.INTERMEDIATE_WAREHOUSE.getName();
      } else {
        newSourceName = Source.PROVINCE_DPM.getName();
      }
      return Source.findByName(newSourceName).map(Enum::name)
          .orElseThrow(() -> new NotFoundException(NO_SUCH_SOURCE + sourceName));
    }

    @Override
    public UUID getSourceId(UUID programId, String source) {
      return StockCardCreateContextHolder.getContext().findSourceId(programId, source.trim())
          .orElseThrow(() -> new NotFoundException(NO_SUCH_SOURCE + source));
    }

  },

  ISSUE() {
    @Override
    public String getReason(String destinationName, Integer adjustment) {
      return Destination.findByName(destinationName.trim()).map(Enum::name)
          .orElseThrow(() -> new NotFoundException(NO_SUCH_DESTINATION + destinationName));
    }

    @Override
    public UUID getDestinationId(UUID programId, String destination) {
      return StockCardCreateContextHolder.getContext().findDestinationId(programId, destination.trim())
          .orElseThrow(() -> new NotFoundException(NO_SUCH_DESTINATION + destination));
    }

  },

  ADJUSTMENT() {
    @Override
    public String getReason(String reasonName, Integer adjustment) {
      return AdjustmentReason.findByName(reasonName.trim()).map(Enum::name)
          .orElseThrow(() -> new NotFoundException(NO_SUCH_REASON + reasonName));
    }

    @Override
    public UUID getAdjustmentReasonId(UUID programId, String reason) {
      return StockCardCreateContextHolder.getContext().findReasonId(programId, reason.trim())
          .orElseThrow(() -> new NotFoundException(NO_SUCH_REASON + reason));
    }

  },

  UNPACK_KIT {
    @Override
    public UUID getDestinationId(UUID programId, String destination) {
      return StockCardCreateContextHolder.getContext().findDestinationId(programId, "UNPACK_KIT")
          .orElseThrow(() -> new NotFoundException(NO_SUCH_DESTINATION + destination));
    }
  };

  public static final String NO_SUCH_SOURCE = "No such source: ";
  public static final String NO_SUCH_DESTINATION = "No such destination: ";
  private static final String INVENTORY_NEGATIVE = "INVENTORY_NEGATIVE";
  private static final String INVENTORY_POSITIVE = "INVENTORY_POSITIVE";
  private static final String INVENTORY = "INVENTORY";
  private static final String NO_SUCH_REASON = "No such reason: ";

  @SuppressWarnings("unused")
  public String getReason(String reasonName, Integer adjustment) {
    return null;
  }

  @SuppressWarnings("unused")
  public UUID getSourceId(UUID programId, String source) {
    return null;
  }

  @SuppressWarnings("unused")
  public UUID getDestinationId(UUID programId, String destination) {
    return null;
  }

  @SuppressWarnings("unused")
  public UUID getAdjustmentReasonId(UUID programId, String reason) {
    return null;
  }

  @SuppressWarnings("unused")
  public UUID getInventoryReasonId(UUID programId, String reason) {
    return null;
  }
}
