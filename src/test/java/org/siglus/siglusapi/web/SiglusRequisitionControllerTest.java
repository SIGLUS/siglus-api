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
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
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
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private SiglusNotificationService notificationService;

  private UUID uuid;

  private BasicRequisitionDto basicRequisitionDto;

  @Mock
  private Pageable pageable;

  private UUID programId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

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
    // given
    when(requisitionController.submitRequisition(uuid, request, response))
        .thenReturn(basicRequisitionDto);

    // when
    BasicRequisitionDto requisition = siglusRequisitionController
        .submitRequisition(uuid, request, response);

    // then
    verify(requisitionController).submitRequisition(uuid, request, response);
    verify(siglusRequisitionService).activateArchivedProducts(any(), any());
    verify(notificationService).postSubmit(requisition);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenAuthorizeRequisition() {
    // given
    siglusRequisitionController.authorizeRequisition(uuid, request, response);

    // then
    verify(siglusRequisitionService).authorizeRequisition(uuid, request, response);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenSearchRequisitionsForApproval() {
    // when
    siglusRequisitionController.searchRequisitionsForApproval(programId, facilityId, pageable);

    // [SIGLUS change start]
    // [change reason]: #368 add requisition facility search param
    // then
    verify(requisitionController).requisitionsForApproval(programId, facilityId, pageable);
    // [SIGLUS change end]
  }

  @Test
  public void shouldCallSiglusRequisitionServiceWhenSearchRequisitions() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    // when
    siglusRequisitionController.searchRequisitions(queryParams, pageable);

    // then
    verify(siglusRequisitionService).searchRequisitions(queryParams, pageable);
  }

  @Test
  public void shouldCallV3ControllerAndServiceWhenApproveRequisition() {
    // given
    UUID requisitionId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    MinimalFacilityDto mockFacility = new MinimalFacilityDto();
    mockFacility.setId(facilityId);
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setFacility(mockFacility);
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);

    // when
    siglusRequisitionController.approveRequisition(requisitionId, request, response);

    // then
    verify(siglusRequisitionService).approveRequisition(requisitionId, request, response);
  }

  @Test
  public void shouldCallV3ControllerWhenRejectRequisition() {
    // given
    UUID requisitionId = UUID.randomUUID();

    // when
    BasicRequisitionDto requisition = siglusRequisitionController
        .rejectRequisition(requisitionId, request, response);

    // then
    verify(requisitionController).rejectRequisition(requisitionId, request, response);
    verify(notificationService).postReject(requisition);
  }

  @Test
  public void shouldCallServiceWhenCreateRequisitionLineItem() {
    // when
    UUID requisitionId = UUID.randomUUID();
    List<UUID> orderableIds = Collections.emptyList();
    siglusRequisitionController.createRequisitionLineItem(requisitionId, orderableIds);

    // then
    verify(siglusRequisitionService).createRequisitionLineItem(requisitionId, orderableIds);
  }

  @Test
  public void shouldCallServiceWhenSearchProcessingPeriodIds() {
    // when
    UUID programId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    boolean emergency = nextBoolean();
    siglusRequisitionController.searchProcessingPeriodIds(programId, facilityId, emergency);

    // then
    verify(siglusProcessingPeriodService).getPeriods(programId, facilityId, emergency);
  }

  @Test
  public void shouldCallV2ControllerWhenInitiate() {
    // when
    UUID programId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    UUID suggestedPeriod = UUID.randomUUID();
    boolean emergency = nextBoolean();
    String physicalInventoryDateStr = "date_str";
    siglusRequisitionController
        .initiate(programId, facilityId, suggestedPeriod, emergency, physicalInventoryDateStr,
            request, response);

    // then
    verify(siglusRequisitionService)
        .initiate(programId, facilityId, suggestedPeriod, emergency, physicalInventoryDateStr,
            request, response);
  }

  @Test
  public void shouldCallServiceWhenSearchRequisition() {
    // when
    UUID requisitionId = UUID.randomUUID();
    siglusRequisitionController.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionService).searchRequisition(requisitionId);
  }

  @Test
  public void shouldCallServiceWhenUpdateRequisition() {
    // when
    UUID requisitionId = UUID.randomUUID();
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    siglusRequisitionController.updateRequisition(requisitionId, requisitionDto, request, response);

    // then
    verify(siglusRequisitionService)
        .updateRequisition(requisitionId, requisitionDto, request, response);
  }

  @Test
  public void shouldCallServiceWhenDeleteRequisition() {
    // when
    UUID requisitionId = UUID.randomUUID();
    siglusRequisitionController.deleteRequisition(requisitionId);

    // then
    verify(siglusRequisitionService).deleteRequisition(requisitionId);
  }

}
