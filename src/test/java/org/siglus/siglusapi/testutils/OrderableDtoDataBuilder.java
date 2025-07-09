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

package org.siglus.siglusapi.testutils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.MetaDataDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableChildDto;
import org.siglus.siglusapi.testutils.api.DtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class OrderableDtoDataBuilder implements DtoDataBuilder<OrderableDto> {
  private static int instanceNumber = 0;

  private UUID id;
  private Long versionNumber;
  private ZonedDateTime lastUpdated;
  private String productCode;
  private String fullProductName;
  private long netContent;
  private long packRoundingThreshold;
  private boolean roundToZero;
  private Set<ProgramOrderableDto> programs;
  private DispensableDto dispensable;
  private org.openlmis.stockmanagement.dto.referencedata.DispensableDto dispensable2;
  private Set<OrderableChildDto> children;
  private Map<String, String> identifiers;
  private Map<String, String> extraData;
  private MetaDataDto metaDataDto;

  /**
   * Creates builder for creating new instance of {@link OrderableDtoDataBuilder}.
   */
  public OrderableDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    versionNumber = 1L;
    lastUpdated = ZonedDateTime.now();
    productCode = "P" + instanceNumber;
    fullProductName = "Product " + instanceNumber;
    netContent = 10L;
    children = Collections.emptySet();
    dispensable = new DispensableDto("pack", "Pack");
    dispensable2 = new org.openlmis.stockmanagement.dto.referencedata.DispensableDto("pack", "Pack");
    identifiers = new HashMap<>();
    extraData = null;
    metaDataDto = new MetaDataDto(Long.parseLong("1"),
        ZonedDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.NOON, ZoneOffset.UTC));
    programs = new HashSet<>();
  }

  public OrderableDtoDataBuilder withProgramOrderable(UUID programId, boolean fullSupply) {
    return withProgramOrderable(programId, fullSupply, Money.of(CurrencyUnit.USD, 1));
  }

  public OrderableDtoDataBuilder withProgramOrderable(UUID programId, Money pricePerPack) {
    return withProgramOrderable(programId, true, pricePerPack);
  }

  public OrderableDtoDataBuilder withProgramOrderable(UUID programId, boolean fullSupply,
      Money pricePerPack) {
    return withProgramOrderable(programId, fullSupply, pricePerPack, 1);
  }

  /**
   * Add program orderable with passed properties.
   */
  public OrderableDtoDataBuilder withProgramOrderable(UUID programId, boolean fullSupply,
      Money pricePerPack, Integer orderableCategoryDisplayOrder) {
    this.programs.add(new ProgramOrderableDto(
        programId, null, null, orderableCategoryDisplayOrder, fullSupply, null, pricePerPack.getAmount()
    ));

    return this;
  }

  public OrderableDtoDataBuilder withIdentifier(String key, String value) {
    identifiers.put(key, value);
    return this;
  }

  public OrderableDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public OrderableDtoDataBuilder withVersionNumber(Long versionNumber) {
    this.versionNumber = versionNumber;
    return this;
  }

  public OrderableDtoDataBuilder withNetContent(long netContent) {
    this.netContent = netContent;
    return this;
  }

  public OrderableDtoDataBuilder withFullProductName(String fullProductName) {
    this.fullProductName = fullProductName;
    return this;
  }

  public OrderableDtoDataBuilder withPackRoundingThreshold(Integer packRoundingThreshold) {
    this.packRoundingThreshold = packRoundingThreshold;
    return this;
  }

  public OrderableDtoDataBuilder withRoundToZero(Boolean roundToZero) {
    this.roundToZero = roundToZero;
    return this;
  }

  public org.openlmis.stockmanagement.dto.referencedata.OrderableDto build() {
    return new org.openlmis.stockmanagement.dto.referencedata.OrderableDto(
        id, productCode, fullProductName, netContent, dispensable2, children, identifiers, extraData,
        metaDataDto, false, false, false
    );
  }

  /**
   * Creates new instance of {@link OrderableDto} with properties.
   * @return created orderable.
   */
  @Override
  public OrderableDto buildAsDto() {
    OrderableDto dto = new OrderableDto();
    dto.setId(id);
    dto.setProductCode(productCode);
    dto.setFullProductName(fullProductName);
    dto.setNetContent(netContent);
    dto.setPackRoundingThreshold(packRoundingThreshold);
    dto.setRoundToZero(roundToZero);
    dto.setPrograms(programs);
    dto.setDispensable(dispensable);
    dto.setIdentifiers(identifiers);
    dto.setMeta(new MetadataDto(versionNumber, lastUpdated));

    return dto;
  }

  public OrderableDtoDataBuilder withPrograms(Set<ProgramOrderableDto> programs) {
    this.programs = programs;
    return this;
  }

  public OrderableDtoDataBuilder withProductCode(String productCode) {
    this.productCode = productCode;
    return this;
  }
}
