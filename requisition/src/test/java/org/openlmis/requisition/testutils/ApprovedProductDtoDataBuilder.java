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

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ApprovedProductDtoDataBuilder implements DtoDataBuilder<ApprovedProductDto> {
  private UUID id = UUID.randomUUID();
  private Long versionNumber = 1L;
  private ZonedDateTime lastUpdated = ZonedDateTime.now();
  private OrderableDto orderable = new OrderableDtoDataBuilder().buildAsDto();
  private ProgramDto program = new ProgramDtoDataBuilder().buildAsDto();
  private Double maxPeriodsOfStock = 3.0;
  private Double minPeriodsOfStock = 1.5;
  private Double emergencyOrderPoint = 1.0;

  public ApprovedProductDtoDataBuilder withOrderable(OrderableDto orderable) {
    this.orderable = orderable;
    return this;
  }

  public ApprovedProductDtoDataBuilder withProgram(ProgramDto program) {
    this.program = program;
    return this;
  }

  public ApprovedProductDtoDataBuilder withMaxPeriodsOfStock(double maxPeriodsOfStock) {
    this.maxPeriodsOfStock = maxPeriodsOfStock;
    return this;
  }

  public ApprovedProductDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ApprovedProductDtoDataBuilder withVersionNumber(Long versionNumber) {
    this.versionNumber = versionNumber;
    return this;
  }

  /**
   * Creates new instance of {@link ApprovedProductDto} with properties.
   * @return created approved product dto
   */
  public ApprovedProductDto buildAsDto() {
    ApprovedProductDto dto = new ApprovedProductDto();
    dto.setId(id);
    dto.setMaxPeriodsOfStock(maxPeriodsOfStock);
    dto.setMinPeriodsOfStock(minPeriodsOfStock);
    dto.setEmergencyOrderPoint(emergencyOrderPoint);
    dto.setOrderable(orderable);
    dto.setProgram(program);
    dto.setMeta(new MetadataDto(versionNumber, lastUpdated));
    return dto;
  }
}
