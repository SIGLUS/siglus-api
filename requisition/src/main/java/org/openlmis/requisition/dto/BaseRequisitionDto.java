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

package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.utils.StatusChangeHelper;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class BaseRequisitionDto
    extends BaseDto
    implements Requisition.Importer, Requisition.Exporter {

  @Getter
  @Setter
  private ZonedDateTime createdDate;

  @Getter
  @Setter
  private ZonedDateTime modifiedDate;

  @Getter
  @Setter
  private String draftStatusMessage;

  @Getter
  @Setter
  private RequisitionStatus status;

  @Getter
  @Setter
  private Boolean emergency;
  
  @Getter
  @Setter
  private Boolean reportOnly;

  @Getter
  @Setter
  private UUID supplyingFacility;

  @Getter
  @Setter
  private UUID supervisoryNode;

  @Getter
  @Setter
  private BasicRequisitionTemplateDto template;

  @Getter
  private Map<String, StatusLogEntry> statusChanges = new HashMap<>();
  
  @Getter
  private List<StatusChangeDto> statusHistory = new ArrayList<>();

  @Getter
  @Setter
  private LocalDate datePhysicalStockCountCompleted;

  @Getter
  @Setter
  private List<ReasonDto> stockAdjustmentReasons;

  @Getter
  @Setter
  private Map<String, Object> extraData;

  @Override
  public List<RequisitionLineItem.Importer> getRequisitionLineItems() {
    return new ArrayList<>(
        Optional.ofNullable(getLineItems()).orElse(Collections.emptyList())
    );
  }

  @Override
  public void addStatusChange(StatusChange.Exporter statusChangeExporter) {
    StatusChangeDto statusChangeDto = (StatusChangeDto) statusChangeExporter;
    StatusChangeHelper.addOrUpdate(this.statusChanges, statusChangeDto);
    statusHistory.add(statusChangeDto);
  }

  @Override
  public Optional<Supplier<StatusChange.Exporter>> provideStatusChangeExporter() {
    return Optional.of(StatusChangeDto::new);
  }

  @Override
  @JsonIgnore
  public UUID getFacilityId() {
    return Optional
        .ofNullable(getFacility())
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public UUID getProgramId() {
    return Optional
        .ofNullable(getProgram())
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public UUID getProcessingPeriodId() {
    return Optional
        .ofNullable(getProcessingPeriod())
        .map(BaseDto::getId)
        .orElse(null);
  }

  public abstract BaseDto getFacility();

  public abstract BaseDto getProgram();

  public abstract BaseDto getProcessingPeriod();

  @JsonIgnore
  abstract List<BaseRequisitionLineItemDto> getLineItems();

}
