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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.validation.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.exception.InvalidReasonException;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.service.SiglusLotLocationService;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.web.request.RemoveLotsRequest;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class SiglusFacilityControllerTest {

  @InjectMocks
  private SiglusFacilityController siglusFacilityController;

  @Mock
  private SiglusLotService siglusLotService;
  @Mock
  private SiglusLotLocationService lotLocationService;
  @Mock
  private SiglusFacilityService siglusFacilityService;

  @Test
  public void shouldGetExpiredLotsGivenParamExpiredIsTrue() {
    UUID facilityId = UUID.randomUUID();

    siglusFacilityController.searchLotStock(facilityId, true, null);

    verify(siglusLotService, Mockito.times(1)).getExpiredLots(facilityId);
  }

  @Test
  public void shouldCallGetLotsGivenParameterOrderableIdIsNotEmpty() {
    UUID facilityId = UUID.randomUUID();
    Set<UUID> orderableIds = new HashSet<>();
    orderableIds.add(UUID.randomUUID());

    siglusFacilityController.searchLotStock(facilityId, null, orderableIds);

    verify(siglusLotService, Mockito.times(1)).getLotStocksByOrderables(facilityId, orderableIds);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionGivenNoParameter() {
    UUID facilityId = UUID.randomUUID();

    siglusFacilityController.searchLotStock(facilityId, null, null);
  }

  @Test
  public void shouldSuccessWhenSearchFacilityRequisitionGroup() {
    UUID facilityId = UUID.randomUUID();
    Set<UUID> programs = Collections.singleton(UUID.randomUUID());

    siglusFacilityController.searchFacilityRequisitionGroup(facilityId, programs);

    verify(siglusFacilityService).searchFacilityRequisitionGroup(facilityId, programs);
  }

  @Test
  public void shouldSuccessWhenSearchLocationStatus() {
    UUID facilityId = UUID.randomUUID();

    siglusFacilityController.searchLocationStatus(facilityId);

    verify(lotLocationService).searchLocationStatus(facilityId);
  }

  @Test
  public void shouldSuccessWhenGetAllClientFacilities() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    siglusFacilityController.getAllClientFacilities(facilityId, programId);

    verify(siglusFacilityService).getAllClientFacilities(facilityId, programId);
  }

  @Test
  public void shouldSuccessWhenRemoveExpiredLots() {
    RemoveLotsRequest request = new RemoveLotsRequest();
    request.setLotType(RemoveLotsRequest.EXPIRED);
    request.setSignature("signature");
    request.setDocumentNumber("documentNumber");
    UUID facilityId = UUID.randomUUID();

    siglusFacilityController.removeExpiredLots(facilityId, request);

    verify(siglusFacilityService).removeExpiredLots(facilityId,
        request.getLots(), request.getSignature(), request.getDocumentNumber());
  }

  @Test(expected = InvalidReasonException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsDoesNotSupport() {
    RemoveLotsRequest request = new RemoveLotsRequest();
    request.setLotType("TEST");
    request.setSignature("signature");
    request.setDocumentNumber("documentNumber");
    UUID facilityId = UUID.randomUUID();

    siglusFacilityController.removeExpiredLots(facilityId, request);
  }
}
