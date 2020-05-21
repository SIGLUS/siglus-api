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

package org.openlmis.requisition.service.stockmanagement;

import static org.junit.Assert.assertEquals;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SERVICE_OCCURED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SERVICE_REQUIRED;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.requisition.dto.ReasonCategory;
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.ReasonType;
import org.openlmis.requisition.dto.ValidReasonDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.service.DataRetrievalException;
import org.openlmis.requisition.testutils.ReasonDtoDataBuilder;
import org.openlmis.requisition.testutils.ValidReasonDtoDataBuilder;
import org.springframework.http.HttpStatus;

public class ValidReasonStockManagementServiceTest
    extends BaseStockmanagementServiceTest<ValidReasonDto> {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ValidReasonStockmanagementService service;

  @Override
  protected BaseCommunicationService<ValidReasonDto> getService() {
    return new ValidReasonStockmanagementService();
  }

  @Override
  protected ValidReasonDto generateInstance() {
    ValidReasonDto validReason = new ValidReasonDtoDataBuilder().buildAsDto();
    validReason.setProgramId(UUID.randomUUID());
    validReason.setFacilityTypeId(UUID.randomUUID());

    ReasonDto reason = new ReasonDtoDataBuilder().buildAsDto();
    reason.setReasonCategory(ReasonCategory.ADJUSTMENT);
    reason.setReasonType(ReasonType.CREDIT);
    validReason.setReason(reason);

    return validReason;
  }

  @Before
  public void setUp() {
    super.setUp();
    service = (ValidReasonStockmanagementService) prepareService();
  }

  @Test
  public void shouldGetValidReasons() {
    // given
    ValidReasonDto validReason = generateInstance();

    // when
    mockArrayResponseEntity(validReason);
    List<ValidReasonDto> actual =
        service.search(validReason.getProgramId(), validReason.getFacilityTypeId());

    // then
    assertEquals(1, actual.size());
    assertEquals(validReason, actual.get(0));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl())
        .hasQueryParameter("program", validReason.getProgramId())
        .hasQueryParameter("facilityType", validReason.getFacilityTypeId());
  }

  @Test
  public void shouldThrowExceptionWithProperKeyIfServerCannotBeFound() {
    // when
    thrown.expect(DataRetrievalException.class);
    thrown.expectMessage(ERROR_SERVICE_REQUIRED);

    mockRequestFail(HttpStatus.NOT_FOUND);
    service.search(UUID.randomUUID(), UUID.randomUUID());
  }


  @Test
  public void shouldThrowExceptionWithProperKeyIfOtherErrorOccurred() {
    // when
    thrown.expect(DataRetrievalException.class);
    thrown.expectMessage(ERROR_SERVICE_OCCURED);

    mockRequestFail(HttpStatus.BAD_REQUEST);
    service.search(UUID.randomUUID(), UUID.randomUUID());
  }
}
