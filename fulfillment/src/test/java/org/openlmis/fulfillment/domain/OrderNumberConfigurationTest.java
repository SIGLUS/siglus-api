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

package org.openlmis.fulfillment.domain;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;

public class OrderNumberConfigurationTest {

  private static final String UUID_STRING = "5625602e-6f5d-11e6-8b77-86f30ca893d3";
  private static final String PROGRAM_CODE = "code";
  private static final String PREFIX = "prefix";
  private static final String EMERGENCY = "E";
  private static final String NOT_EMERGENCY = "R";


  private Order order;
  private ProgramDto program;

  @Before
  public void setUp() {
    order = new Order();
    order.setExternalId(UUID.fromString(UUID_STRING));
    order.setEmergency(true);

    program = new ProgramDto();
    program.setCode(PROGRAM_CODE);
  }

  @Test
  public void shouldGenerateOrderNumber() {

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration(PREFIX, true, true, true);
    String generatedNumber =
        orderNumberConfiguration.formatOrderNumber(order, program, UUID_STRING);

    assertEquals(PREFIX + PROGRAM_CODE + UUID_STRING + EMERGENCY, generatedNumber);
  }

  @Test
  public void shouldGenerateOrderNumberWithOnlyRequiredData() {

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration(PREFIX, false, false, false);
    String generatedNumber =
        orderNumberConfiguration.formatOrderNumber(order, null, UUID_STRING);

    assertEquals(UUID_STRING, generatedNumber);
  }

  @Test
  public void shouldCorrectlyHandleNullPrefix() {

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration(null, true, false, false);
    String generatedNumber =
        orderNumberConfiguration.formatOrderNumber(order, program, UUID_STRING);

    assertEquals(UUID_STRING, generatedNumber);
  }

  @Test
  public void shouldGenerateCorrectSuffixForNotEmergencyOrder() {

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration(null, true, false, true);
    order.setEmergency(false);
    String generatedNumber =
        orderNumberConfiguration.formatOrderNumber(order, program, UUID_STRING);

    String expectedResult = UUID_STRING + NOT_EMERGENCY;

    assertEquals(expectedResult, generatedNumber);
  }

  @Test(expected = OrderNumberException.class)
  public void shouldThrowExceptionWhenGeneratingNumberFromNullOrder() {

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration(PREFIX, true, false, false);

    orderNumberConfiguration.formatOrderNumber(null, program, UUID_STRING);
  }

  @Test
  public void equalsContract() throws Exception {
    EqualsVerifier.forClass(OrderNumberConfiguration.class).withRedefinedSuperclass().verify();
  }
}
