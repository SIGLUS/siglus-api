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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.dto.OrderableDto.COMMODITY_TYPE_IDENTIFIER;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramOrderableDtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class OrderableDtoTest extends EqualsContractTest<OrderableDto> {

  private OrderableDto orderableDto;
  private Set<ProgramOrderableDto> products;
  private ProgramDto program;

  @Before
  public void setUp() {
    products = new HashSet<>();
    products.addAll(genereteProducts(10));

    program = new ProgramDtoDataBuilder().withId(UUID.randomUUID()).buildAsDto();
    orderableDto = new OrderableDtoDataBuilder().withId(UUID.randomUUID()).buildAsDto();
  }

  @Override
  protected Class<OrderableDto> getTestClass() {
    return OrderableDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier verifier) {
    verifier.withRedefinedSuperclass();
  }

  @Test
  public void shouldCalculatePacksToOrderWhenPackRoundingThresholdIsSmallerThanRemainder() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withPackRoundingThreshold(4)
        .withRoundToZero(false)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(26);

    assertEquals(3, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenPackRoundingThresholdIsGreaterThanRemainder() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withPackRoundingThreshold(7)
        .withRoundToZero(false)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(26);

    assertEquals(2, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenCanRoundToZero() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withPackRoundingThreshold(7)
        .withRoundToZero(true)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(6);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenCanNotRoundToZero() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withPackRoundingThreshold(4)
        .withRoundToZero(false)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(6);

    assertEquals(1, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPacksToOrderIfNetContentIsZero() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(0)
        .withPackRoundingThreshold(7)
        .withRoundToZero(true)
        .buildAsDto();
    long packsToOrder = productDto.packsToOrder(6);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPacksToOrderIfOrderQuantityIsZero() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withRoundToZero(false)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(0);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPackToOrderIfOrderQuantityIsOneAndRoundToZeroTrueWithNetContentTen() {
    OrderableDto productDto = new OrderableDtoDataBuilder()
        .withNetContent(10)
        .withPackRoundingThreshold(7)
        .withRoundToZero(true)
        .buildAsDto();

    long packsToOrder = productDto.packsToOrder(1);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldNotRoundUpWhenEqualToThreshold() {
    OrderableDto product = new OrderableDtoDataBuilder()
        .withNetContent(100)
        .withPackRoundingThreshold(50)
        .buildAsDto();

    long packsToOrder = product.packsToOrder(250);
    assertEquals(2, packsToOrder);

    packsToOrder = product.packsToOrder(251);
    assertEquals(3, packsToOrder);
  }

  @Test
  public void shouldGetProgramOrderable() {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDtoDataBuilder()
        .withProgramId(program.getId())
        .buildAsDto();

    products.add(programOrderableDto);
    orderableDto.setPrograms(products);

    ProgramOrderableDto programOrderableDtoAfterFind = orderableDto
            .getProgramOrderable(program.getId());

    assertEquals(programOrderableDtoAfterFind, programOrderableDto);
    assertEquals(programOrderableDtoAfterFind.getProgramId(), programOrderableDto.getProgramId());
  }

  @Test
  public void shouldReturnCommodityTypeIdentifier() {
    String value = UUID.randomUUID().toString();
    orderableDto.setIdentifiers(ImmutableMap.of(COMMODITY_TYPE_IDENTIFIER, value));
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(value));
  }

  @Test
  public void shouldReturnNullIfCommodityTypeIdentifierNotExist() {
    // null as map
    orderableDto.setIdentifiers(null);
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(nullValue()));

    // empty map
    Map<String,String> identifiers = Maps.newHashMap();

    orderableDto.setIdentifiers(identifiers);
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(nullValue()));

    // null value
    identifiers.put(COMMODITY_TYPE_IDENTIFIER, null);
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(nullValue()));

    // white spaces value
    identifiers.put(COMMODITY_TYPE_IDENTIFIER, "          ");
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(nullValue()));

    // empty ("") value
    identifiers.put(COMMODITY_TYPE_IDENTIFIER, "");
    assertThat(orderableDto.getCommodityTypeIdentifier(), is(nullValue()));
  }

  private Set<ProgramOrderableDto> genereteProducts(int instances) {
    Set<ProgramOrderableDto> programs = new HashSet<>();
    for (int i = 0; i < instances; i++) {
      ProgramDto program = new ProgramDtoDataBuilder()
          .withId(UUID.randomUUID())
          .buildAsDto();
      ProgramOrderableDto programOrderableDto = new ProgramOrderableDtoDataBuilder()
          .withProgramId(program.getId())
          .buildAsDto();
      programs.add(programOrderableDto);
    }
    return programs;
  }
}
