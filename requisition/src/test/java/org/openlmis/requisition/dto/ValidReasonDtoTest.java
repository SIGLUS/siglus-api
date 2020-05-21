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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class ValidReasonDtoTest {

  private ReasonDto reasonDto;
  private ValidReasonDto validReasonDto;

  @Before
  public void setUp() {
    reasonDto = new ReasonDto();
    reasonDto.setId(UUID.randomUUID());
    reasonDto.setReasonCategory(ReasonCategory.ADJUSTMENT);
    reasonDto.setReasonType(ReasonType.BALANCE_ADJUSTMENT);
    reasonDto.setDescription("simple description");
    reasonDto.setIsFreeTextAllowed(false);
    reasonDto.setName("simple name");

    validReasonDto = new ValidReasonDto();
    validReasonDto.setReason(reasonDto);
  }

  @Test
  public void shouldGetReasonWithHiddenTrue() {
    validReasonDto.setHidden(true);

    ReasonDto reasonWithHidden = validReasonDto.getReasonWithHidden();
    assertTrue(reasonWithHidden.getHidden());
  }

  @Test
  public void shouldGetReasonWithHiddenFalse() {
    validReasonDto.setHidden(false);

    ReasonDto reasonWithHidden = validReasonDto.getReasonWithHidden();
    assertFalse(reasonWithHidden.getHidden());
  }

}