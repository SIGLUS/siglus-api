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

package org.openlmis.requisition.testutils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.custom.DefaultRequisitionSearchParams;
import org.openlmis.requisition.testutils.api.DataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class DefaultRequisitionSearchParamsDataBuilder implements
    DataBuilder<DefaultRequisitionSearchParams> {

  private UUID facility;
  private UUID program;
  private UUID processingPeriod;
  private UUID supervisoryNode;

  private Boolean emergency;

  private LocalDate initiatedDateFrom;
  private LocalDate initiatedDateTo;

  private ZonedDateTime modifiedDateFrom;
  private ZonedDateTime modifiedDateTo;

  private Set<RequisitionStatus> requisitionStatuses;

  /**
   * Create new instance of {@link DefaultRequisitionSearchParams}.
   */
  @Override
  public DefaultRequisitionSearchParams build() {
    return new DefaultRequisitionSearchParams(
        facility,
        program,
        processingPeriod,
        supervisoryNode,
        emergency,
        initiatedDateFrom,
        initiatedDateTo,
        modifiedDateFrom,
        modifiedDateTo,
        requisitionStatuses
    );
  }

  public DefaultRequisitionSearchParamsDataBuilder withFacility(UUID facility) {
    this.facility = facility;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withProgram(UUID program) {
    this.program = program;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withProcessingPeriod(UUID processingPeriod) {
    this.processingPeriod = processingPeriod;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withSupervisoryNode(UUID supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withEmergency(Boolean emergency) {
    this.emergency = emergency;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withInitiatedDateFrom(
      LocalDate initiatedDateFrom) {
    this.initiatedDateFrom = initiatedDateFrom;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withInitiatedDateTo(LocalDate initiatedDateTo) {
    this.initiatedDateTo = initiatedDateTo;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withModifiedDateFrom(
      ZonedDateTime modifiedDateFrom) {
    this.modifiedDateFrom = modifiedDateFrom;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withModifiedDateTo(
      ZonedDateTime modifiedDateTo) {
    this.modifiedDateTo = modifiedDateTo;
    return this;
  }

  public DefaultRequisitionSearchParamsDataBuilder withRequisitionStatuses(
      Set<RequisitionStatus> requisitionStatuses) {
    this.requisitionStatuses = requisitionStatuses;
    return this;
  }
}
