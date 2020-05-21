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
import java.util.ArrayList;
import java.util.List;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.dto.RequisitionReportDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionReportDtoDataBuilder implements DtoDataBuilder<RequisitionReportDto> {

  private RequisitionDto requisition;
  private List<RequisitionLineItemDto> fullSupply;
  private List<RequisitionLineItemDto> nonFullSupply;
  private Money fullSupplyTotalCost;
  private Money nonFullSupplyTotalCost;
  private Money totalCost;
  private UserDto initiatedBy;
  private ZonedDateTime initiatedDate;
  private UserDto submittedBy;
  private ZonedDateTime submittedDate;
  private UserDto authorizedBy;
  private ZonedDateTime authorizedDate;

  /**
   * Builder for {@link RequisitionReportDtoDataBuilder}.
   */
  public RequisitionReportDtoDataBuilder() {
    requisition = new RequisitionDataBuilder().buildAsDto();
    fullSupply = new ArrayList<>();
    nonFullSupply = new ArrayList<>();
    fullSupplyTotalCost = Money.of(CurrencyUnit.EUR, 5);
    nonFullSupplyTotalCost = Money.of(CurrencyUnit.EUR, 1);
    totalCost = Money.of(CurrencyUnit.EUR, 6);
    initiatedBy = new UserDtoDataBuilder().buildAsDto();
    initiatedDate = ZonedDateTime.now().minusMonths(1);
    submittedBy = new UserDtoDataBuilder().buildAsDto();
    submittedDate = ZonedDateTime.now().minusDays(2);
    authorizedBy = new UserDtoDataBuilder().buildAsDto();
    authorizedDate = ZonedDateTime.now().minusDays(1);
  }

  /**
   * Builds {@link RequisitionReportDto} test data instance.
   * @return RequisitionReportDto
   */
  @Override
  public RequisitionReportDto buildAsDto() {
    return new RequisitionReportDto(requisition, fullSupply, nonFullSupply, fullSupplyTotalCost,
        nonFullSupplyTotalCost, totalCost, initiatedBy, initiatedDate, submittedBy, submittedDate,
        authorizedBy, authorizedDate);
  }

  public RequisitionReportDtoDataBuilder withRequisition(RequisitionDto requisition) {
    this.requisition = requisition;
    return this;
  }

  public RequisitionReportDtoDataBuilder withFullSupply(List<RequisitionLineItemDto> fullSupply) {
    this.fullSupply = fullSupply;
    return this;
  }

  public RequisitionReportDtoDataBuilder withNonFullSupply(
      List<RequisitionLineItemDto> nonFullSupply) {
    this.nonFullSupply = nonFullSupply;
    return this;
  }

  public RequisitionReportDtoDataBuilder withFullSupplyTotalCost(Money fullSupplyTotalCost) {
    this.fullSupplyTotalCost = fullSupplyTotalCost;
    return this;
  }

  /**
   * Sets non full supply total cost.
   */
  public RequisitionReportDtoDataBuilder withNonFullSupplyTotalCost(Money nonFullSupplyTotalCost) {
    this.nonFullSupplyTotalCost = nonFullSupplyTotalCost;
    return this;

  }

  public RequisitionReportDtoDataBuilder withTotalCost(Money totalCost) {
    this.totalCost = totalCost;
    return this;
  }

  public RequisitionReportDtoDataBuilder withInitiatedBy(UserDto initiatedBy) {
    this.initiatedBy = initiatedBy;
    return this;
  }

  public RequisitionReportDtoDataBuilder withInitiatedDate(ZonedDateTime initiatedDate) {
    this.initiatedDate = initiatedDate;
    return this;
  }

  public RequisitionReportDtoDataBuilder withSubmittedBy(UserDto submittedBy) {
    this.submittedBy = submittedBy;
    return this;
  }

  public RequisitionReportDtoDataBuilder withSubmittedDate(ZonedDateTime submittedDate) {
    this.submittedDate = submittedDate;
    return this;
  }

  public RequisitionReportDtoDataBuilder withAuthorizedBy(UserDto authorizedBy) {
    this.authorizedBy = authorizedBy;
    return this;
  }

  public RequisitionReportDtoDataBuilder withAuthorizedDate(ZonedDateTime authorizedDate) {
    this.authorizedDate = authorizedDate;
    return this;
  }
}
