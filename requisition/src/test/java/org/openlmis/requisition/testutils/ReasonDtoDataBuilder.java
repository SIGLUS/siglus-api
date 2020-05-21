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

import org.openlmis.requisition.dto.ReasonCategory;
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.ReasonType;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ReasonDtoDataBuilder implements DtoDataBuilder<ReasonDto> {

  private String name;
  private String description;
  private ReasonType reasonType;
  private ReasonCategory reasonCategory;
  private Boolean isFreeTextAllowed;
  private Boolean hidden;

  /**
   * Builder for {@link ReasonDto}.
   */
  public ReasonDtoDataBuilder() {
    this.name = "name";
    this.description = "description";
    this.reasonType = ReasonType.DEBIT;
    this.reasonCategory = ReasonCategory.TRANSFER;
    this.isFreeTextAllowed = true;
    this.hidden = false;
  }

  @Override
  public ReasonDto buildAsDto() {
    ReasonDto dto = new ReasonDto();
    dto.setName(name);
    dto.setDescription(description);
    dto.setReasonType(reasonType);
    dto.setReasonCategory(reasonCategory);
    dto.setIsFreeTextAllowed(isFreeTextAllowed);
    dto.setHidden(hidden);
    return dto;
  }

  public ReasonDtoDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public ReasonDtoDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  public ReasonDtoDataBuilder withReasonType(ReasonType reasonType) {
    this.reasonType = reasonType;
    return this;
  }

  public ReasonDtoDataBuilder withReasonCategory(ReasonCategory reasonCategory) {
    this.reasonCategory = reasonCategory;
    return this;
  }

  public ReasonDtoDataBuilder withFreeTextAllowed(Boolean freeTextAllowed) {
    isFreeTextAllowed = freeTextAllowed;
    return this;
  }

  public ReasonDtoDataBuilder withHidden(Boolean hidden) {
    this.hidden = hidden;
    return this;
  }
}
