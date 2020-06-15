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

import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionControllerTest {

  @InjectMocks
  private SiglusRequisitionController siglusRequisitionController;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private RequisitionV2Controller requisitionV2Controller;

  @Mock
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  private UUID uuid;

  private BasicRequisitionDto basicRequisitionDto;

  @Mock
  private Pageable pageable;

  private UUID programId = UUID.randomUUID();

  @Before
  public void prepare() {
    uuid = UUID.randomUUID();
    basicRequisitionDto = new BasicRequisitionDto();
    MinimalFacilityDto minimalFacilityDto = new MinimalFacilityDto();
    minimalFacilityDto.setId(UUID.randomUUID());
    basicRequisitionDto.setFacility(minimalFacilityDto);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenSubmitRequisition() {
    when(requisitionController.submitRequisition(uuid, request, response))
        .thenReturn(basicRequisitionDto);

    siglusRequisitionController.submitRequisition(uuid, request, response);

    verify(requisitionController).submitRequisition(uuid, request, response);
    verify(siglusRequisitionService).activateArchivedProducts(any(), any());
  }

  @Test
  public void shouldCallOpenlmisControllerWhenAuthorizeRequisition() {
    when(requisitionController.authorizeRequisition(uuid, request, response))
        .thenReturn(basicRequisitionDto);

    siglusRequisitionController.authorizeRequisition(uuid, request, response);

    verify(requisitionController).authorizeRequisition(uuid, request, response);
    verify(siglusRequisitionService).activateArchivedProducts(any(), any());
  }

  @Test
  public void shouldCallOpenlmisControllerWhenSearchRequisitionsForApproval() {
    siglusRequisitionController.searchRequisitionsForApproval(programId, pageable);

    verify(requisitionController).requisitionsForApproval(programId, pageable);
  }

  @Test
  public void shouldCallSiglusRequisitionServiceWhenSearchRequisitions() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    siglusRequisitionController.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionService).searchRequisitions(queryParams, pageable);
  }

  @Test
  public void shouldCallV3ControllerAndServiceWhenApproveRequisition() {
    UUID requisitionId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    MinimalFacilityDto mockFacility = new MinimalFacilityDto();
    mockFacility.setId(facilityId);
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setFacility(mockFacility);
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);

    siglusRequisitionController.approveRequisition(requisitionId, request, response);

    verify(requisitionController).approveRequisition(requisitionId, request, response);
    verify(siglusRequisitionService).activateArchivedProducts(requisitionId, facilityId);
  }

  @Test
  public void shouldCallV3ControllerWhenRejectRequisition() {
    UUID requisitionId = UUID.randomUUID();
    siglusRequisitionController.rejectRequisition(requisitionId, request, response);

    verify(requisitionController).rejectRequisition(requisitionId, request, response);
  }

  @Test
  public void shouldCallServiceWhenCreateRequisitionLineItem() {
    UUID requisitionId = UUID.randomUUID();
    List<UUID> orderableIds = Collections.emptyList();
    siglusRequisitionController.createRequisitionLineItem(requisitionId, orderableIds);

    verify(siglusRequisitionService).createRequisitionLineItem(requisitionId, orderableIds);
  }

  @Test
  public void shouldCallServiceWhenSearchProcessingPeriodIds() {
    UUID programId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    boolean emergency = nextBoolean();
    siglusRequisitionController.searchProcessingPeriodIds(programId, facilityId, emergency);

    verify(siglusProcessingPeriodService).getPeriods(programId, facilityId, emergency);
  }

  @Test
  public void shouldCallV2ControllerWhenInitiate() {
    UUID programId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    UUID suggestedPeriod = UUID.randomUUID();
    boolean emergency = nextBoolean();
    String physicalInventoryDateStr = "date_str";
    siglusRequisitionController
        .initiate(programId, facilityId, suggestedPeriod, emergency, physicalInventoryDateStr,
            request, response);

    verify(requisitionV2Controller)
        .initiate(programId, facilityId, suggestedPeriod, emergency, physicalInventoryDateStr,
            request, response);
  }

  @Test
  public void shouldCallServiceWhenSearchRequisition() {
    UUID requisitionId = UUID.randomUUID();
    siglusRequisitionController.searchRequisition(requisitionId);

    verify(siglusRequisitionService).searchRequisition(requisitionId);
  }

  @Test
  public void shouldCallServiceWhenUpdateRequisition() {
    UUID requisitionId = UUID.randomUUID();
    RequisitionV2Dto requisitionDto = new RequisitionV2Dto();
    siglusRequisitionController.updateRequisition(requisitionId, requisitionDto, request, response);

    verify(siglusRequisitionService)
        .updateRequisition(requisitionId, requisitionDto, request, response);
  }

  @Test
  public void shouldCallServiceWhenDeleteRequisition() {
    UUID requisitionId = UUID.randomUUID();
    siglusRequisitionController.deleteRequisition(requisitionId);

    verify(siglusRequisitionService).deleteRequisition(requisitionId);
  }

}