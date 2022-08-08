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

package org.siglus.siglusapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.domain.ProofsOfDeliveryExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.ProofsOfDeliveryExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionsRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.PodExtensionRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodServiceTest {

  @InjectMocks
  private SiglusPodService service;

  @Mock
  private SiglusPodFulfillmentService fulfillmentService;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private PodSubDraftRepository podSubDraftRepository;

  @Mock
  private PodLineItemsExtensionRepository podLineItemsExtensionRepository;

  @Mock
  private PodLineItemsRepository podLineItemsRepository;

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private ProofOfDeliveryController podController;

  @Mock
  private SiglusNotificationService notificationService;

  @Mock
  private OrdersRepository ordersRepository;

  @Mock
  private RequisitionsRepository requisitionsRepository;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private StatusChangeRepository requisitionStatusChangeRepository;

  @Mock
  private SiglusRequisitionExtensionService requisitionExtensionService;

  @Mock
  private ProofsOfDeliveryExtensionRepository podExtensionRepository;

  private final UUID externalId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID podId = UUID.randomUUID();
  private final UUID subDraftId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final UUID lineItemId2 = UUID.randomUUID();
  private final UUID orderId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID orderDtoId = UUID.randomUUID();
  private final UUID shipmentDtoId = UUID.randomUUID();
  private final String productCode = "product code";
  private final String productName = "product name";
  private final String serviceUrl = "serviceUrl";
  private final String facilityCode = "facility code";
  private final String lotCode = "lot code";
  private final String requisitionNum = "requisitionNumber";
  private final String preparedBy = "prepared by user1";
  private final String conferredBy = "conferred by user2";
  private final Set<String> defaultExpands = Sets.newHashSet("shipment.order");

  @Test
  public void shouldGetPartialQualityWhenGetProofOfDelivery() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(UUID.randomUUID());
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(UUID.randomUUID());
    orderDto.setOrderLineItems(Arrays.asList(lineItemDto));
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(UUID.randomUUID());
    shipmentDto.setOrder(orderDto);
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setShipment(shipmentDto);
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(dto);
    OrderLineItemDto lineItemDtoExtension = new OrderLineItemDto();
    lineItemDtoExtension.setId(UUID.randomUUID());
    lineItemDtoExtension.setPartialFulfilledQuantity(10L);
    OrderObjectReferenceDto orderExtensionDto = new OrderObjectReferenceDto(UUID.randomUUID());
    orderExtensionDto.setOrderLineItems(Arrays.asList(lineItemDtoExtension));
    when(siglusOrderService.getExtensionOrder(any(OrderObjectReferenceDto.class)))
        .thenReturn(orderExtensionDto);

    // when
    ProofOfDeliveryDto proofOfDeliveryDto = service
        .getExpandedPodDtoById(UUID.randomUUID(), Collections.emptySet());

    //then
    OrderLineItemDto lineItem = proofOfDeliveryDto.getShipment().getOrder().getOrderLineItems()
        .get(0);
    assertEquals(Long.valueOf(10), lineItem.getPartialFulfilledQuantity());
  }

  @Test
  public void shouldSetRequisitionNumberWhenGetProofOfDelivery() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(UUID.randomUUID());
    orderDto.setExternalId(externalId);
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(UUID.randomUUID());
    shipmentDto.setOrder(orderDto);
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setShipment(shipmentDto);
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(dto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId)).thenReturn(requisitionNum);

    // when
    ProofOfDeliveryDto proofOfDeliveryDto = service.getExpandedPodDtoById(UUID.randomUUID(), Collections.emptySet());

    // then
    assertEquals(requisitionNum, proofOfDeliveryDto.getShipment().getOrder().getRequisitionNumber());
  }

  @Test
  public void shouldSetExtensionInfoWhenGetPodExtensionResponse() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId)).thenReturn(requisitionNum);
    mockPodExtensionQuery();

    // when
    PodExtensionResponse podExtensionResponse = service.getPodExtensionResponse(podId, defaultExpands);

    // then
    assertEquals(preparedBy, podExtensionResponse.getPreparedBy());
    assertEquals(conferredBy, podExtensionResponse.getConferredBy());
  }

  @Test
  public void shouldNotPodExtensionInfoWhenGetPodExtensionResponseWithPodExtensionNull() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId)).thenReturn(requisitionNum);
    mockPodExtensionQueryReturnNull();

    // when
    PodExtensionResponse podExtensionResponse = service.getPodExtensionResponse(podId, defaultExpands);

    // then
    assertNull(podExtensionResponse.getPreparedBy());
    assertNull(podExtensionResponse.getConferredBy());
  }

  @Test
  public void shouldSuccessWhenCreateSubDrafts() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(podSubDraftRepository.save(anyList())).thenReturn(buildMockSubDrafts());
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId))).thenReturn(buildMockOrderables());

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
    service.createSubDrafts(podId, request);

    // then
    verify(podSubDraftRepository).save(any(List.class));
    verify(podLineItemsExtensionRepository).save(any(List.class));
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenCreateSubDraftsWithPodIdNotExist() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
    service.createSubDrafts(podId, request);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenCreateSubDraftsWithPodIdLineItemsIsEmpty() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithNoLineItems());

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
    service.createSubDrafts(podId, request);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenCreateSubDraftsWithSubDraftsExisted() {
    // given
    when(podSubDraftRepository.count(any())).thenReturn(1L);

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
    service.createSubDrafts(podId, request);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowWhenCreateSubDraftsWithSplitNumTooLarge() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId))).thenReturn(buildMockOrderables());

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(10);
    service.createSubDrafts(podId, request);
  }

  @Test
  public void shouldReturnWhenGetSubDraftSummary() {
    // given
    List<PodSubDraft> podSubDrafts = buildMockSubDrafts();
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(podSubDrafts);
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    when(authenticationHelper.getUserNameByUserId(any())).thenReturn("CS_role1");

    // when
    PodSubDraftsSummaryResponse actualResponse = service.getSubDraftSummary(podId);

    // then
    PodSubDraftsSummaryResponse expectedResponse = PodSubDraftsSummaryResponse.builder()
        .podId(podId)
        .subDrafts(toSubDraftInfos(podSubDrafts))
        .canMergeOrDeleteDrafts(Boolean.TRUE)
        .canSubmitDrafts(Boolean.FALSE)
        .build();
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void shouldReturnCanSubmitWhenGetSubDraftSummary() {
    // given
    List<PodSubDraft> podSubDrafts = buildMockSubDraftsAllSubmitted();
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(podSubDrafts);
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.FALSE);
    when(authenticationHelper.getUserNameByUserId(any())).thenReturn("CS_role1");

    // when
    PodSubDraftsSummaryResponse actualResponse = service.getSubDraftSummary(podId);

    // then
    PodSubDraftsSummaryResponse expectedResponse = PodSubDraftsSummaryResponse.builder()
        .podId(podId)
        .subDrafts(toSubDraftInfos(podSubDrafts))
        .canMergeOrDeleteDrafts(Boolean.FALSE)
        .canSubmitDrafts(Boolean.TRUE)
        .build();
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenGetSubDraftSummaryWithSubDraftsNotExist() {
    // given
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(Lists.newArrayList());

    // when
    service.getSubDraftSummary(podId);
  }

  @Test
  public void shouldReturnWhenGetSubDraftDetail() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithTwoLineItems());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());

    // when
    ProofOfDeliveryDto subDraftDetail = service.getSubDraftDetail(podId, subDraftId, defaultExpands);

    // then
    assertEquals(subDraftDetail.getLineItems().size(), 1);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenGetSubDraftDetailWithSubDraftNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(null);

    // when
    service.getSubDraftDetail(podId, subDraftId, defaultExpands);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenGetSubDraftDetailWithPodIdAndSubDraftIdNotMatch() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());

    // when
    service.getSubDraftDetail(UUID.randomUUID(), subDraftId, defaultExpands);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenGetSubDraftDetailWithPodIdNotExist() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    service.getSubDraftDetail(podId, subDraftId, defaultExpands);
  }

  @Test
  public void shouldSuccessWhenSaveSubDraft() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(podLineItemsRepository.findAll(anySet())).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);

    // then
    verify(podLineItemsRepository).save(any(List.class));
    verify(podSubDraftRepository).save(any(PodSubDraft.class));
  }

  @Test
  public void shouldSuccessWhenSubmitSubDraft() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(podLineItemsRepository.findAll(anySet())).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithTwoLineItems());
    request.setOperateType(OperateTypeEnum.SUBMIT);
    service.updateSubDraft(request, subDraftId);

    // then
    verify(podLineItemsRepository).save(any(List.class));
    verify(podSubDraftRepository).save(any(PodSubDraft.class));
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenUpdateSubDraftWithSubDraftIdNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(null);

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithTwoLineItems());
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenUpdateSubDraftWithSubDraftSubmitted() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setOperateType(OperateTypeEnum.SUBMIT);
    service.updateSubDraft(request, subDraftId);
  }

  @Test
  public void shouldSuccessWhenDeleteSubDraft() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithTwoLineItems());
    when(podLineItemsRepository.findAll(anySet())).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    service.deleteSubDraft(podId, subDraftId);

    // then
    verify(podLineItemsRepository).save(any(List.class));
    verify(podSubDraftRepository).save(any(PodSubDraft.class));
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenDeleteSubDraftWithSubDraftIdNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(null);

    // when
    service.deleteSubDraft(podId, subDraftId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenDeleteSubDraftWithSubDraftSubmitted() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    // when
    service.deleteSubDraft(podId, subDraftId);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenDeleteSubDraftWithPodIdNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    service.deleteSubDraft(podId, subDraftId);
  }

  @Test
  public void shouldSuccessWhenDeleteSubDrafts() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithTwoLineItems());
    when(podLineItemsExtensionRepository.findAllBySubDraftIds(Lists.newArrayList(subDraftId))).thenReturn(
        buildMockPodLineItemsExtensions());

    // when
    service.deleteSubDrafts(podId);

    // then
    verify(podLineItemsRepository).save(any(List.class));
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowWhenDeleteSubDraftsWithNoPermission() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.FALSE);

    // when
    service.deleteSubDrafts(podId);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenDeleteSubDraftsWithSubDraftsNotExist() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(null);

    // when
    service.deleteSubDrafts(podId);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenDeleteSubDraftsWithPodIdNotExist() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    service.deleteSubDrafts(podId);
  }

  @Test
  public void shouldReturnWhenMergeSubDrafts() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithNoLineItems());
    when(podExtensionRepository.findOne(podId)).thenReturn(buildMockPodExtension());
    ProofOfDeliveryDto expectedResponse = buildMockPodDtoWithNoLineItems();

    // when
    ProofOfDeliveryDto actualResponse = service.mergeSubDrafts(podId, defaultExpands);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowWhenMergeSubDraftsWithNoPermission() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.FALSE);

    // when
    service.mergeSubDrafts(podId, defaultExpands);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenMergeSubDraftsWithSubDraftsNotExist() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(null);

    // when
    service.mergeSubDrafts(podId, defaultExpands);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenMergeSubDraftsWithPodIdNotExist() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    service.mergeSubDrafts(podId, defaultExpands);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenMergeSubDraftsWithNotAllPodSubDraftSubmitted() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDrafts());

    // when
    service.mergeSubDrafts(podId, defaultExpands);
  }

  @Test
  public void shouldReturnWhenSubmitSubDrafts() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    PodExtensionRequest request = buildPodExtensionRequest();
    ProofOfDeliveryDto dto = request.getPodDto();
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    when(podController.updateProofOfDelivery(podId, dto, null)).thenReturn(dto);

    // when
    ProofOfDeliveryDto actualResponse = service.submitSubDrafts(podId, request, null);

    // then
    assertEquals(dto, actualResponse);
    verify(podController).updateProofOfDelivery(any(), any(), any());
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
    verify(notificationService).postConfirmPod(dto);
  }

  @Test
  public void shouldReturnWhenSubmitSubDraftsWithPodNotConfirmed() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    PodExtensionRequest request = buildPodExtensionRequestWithPodTwoLineItems();
    ProofOfDeliveryDto dto = request.getPodDto();
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    when(podController.updateProofOfDelivery(podId, dto, null)).thenReturn(dto);

    // when
    ProofOfDeliveryDto actualResponse = service.submitSubDrafts(podId, request, null);

    // then
    assertEquals(dto, actualResponse);
    verify(podController).updateProofOfDelivery(any(), any(), any());
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
    verify(notificationService, times(0)).postConfirmPod(dto);
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowWhenSubmitSubDraftsWithNoPermission() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.FALSE);

    // when
    service.submitSubDrafts(podId, buildPodExtensionRequest(), null);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenSubmitSubDraftsWithSubDraftsNotExist() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(null);

    // when
    service.submitSubDrafts(podId, buildPodExtensionRequest(), null);
  }

  @Test
  public void shouldReturnWhenGetPintInfo() {
    // given
    OrderDto orderDto = buildMockOrderDto();
    when(ordersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneFacility(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel3());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChanges());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(requisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNotNull(response.getSupplierDistrict());
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithDifferentDbResult() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(ordersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneFacility(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel2());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(requisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNull(response.getSupplierDistrict());
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithZoneNull() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(ordersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneFacility(orderDto.getSupplyingFacilityId())).thenReturn(
        new FacilityDto());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(requisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNull(response.getSupplierDistrict());
    assertNull(response.getSupplierProvince());
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithParentZoneNull() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(ordersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    Order orderForQueryCount = new Order();
    orderForQueryCount.setProcessingPeriodId(orderDto.getProcessingPeriodId());
    orderForQueryCount.setEmergency(orderDto.getEmergency());
    Example<Order> orderExample = Example.of(orderForQueryCount);
    when(ordersRepository.count(orderExample)).thenReturn(1L);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneFacility(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel3AndNoParent());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(requisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNotNull(response.getSupplierDistrict());
    assertNull(response.getSupplierProvince());
  }

  private void mockPodExtensionQuery() {
    Example<ProofsOfDeliveryExtension> podExtensionExample = Example.of(
        ProofsOfDeliveryExtension.builder().podId(podId).build());
    when(podExtensionRepository.findOne(podExtensionExample)).thenReturn(buildMockPodExtension());
  }

  private void mockPodExtensionQueryReturnNull() {
    Example<ProofsOfDeliveryExtension> podExtensionExample = Example.of(
        ProofsOfDeliveryExtension.builder().podId(podId).build());
    when(podExtensionRepository.findOne(podExtensionExample)).thenReturn(null);
  }

  private PodExtensionRequest buildPodExtensionRequest() {
    PodExtensionRequest request = new PodExtensionRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setPreparedBy(preparedBy);
    request.setConferredBy(conferredBy);
    return request;
  }

  private PodExtensionRequest buildPodExtensionRequestWithPodTwoLineItems() {
    PodExtensionRequest request = new PodExtensionRequest();
    request.setPodDto(buildMockPodDtoWithTwoLineItems());
    request.setPreparedBy(preparedBy);
    request.setConferredBy(conferredBy);
    return request;
  }

  private ProofsOfDeliveryExtension buildMockPodExtension() {
    return ProofsOfDeliveryExtension.builder()
        .preparedBy(preparedBy)
        .conferredBy(conferredBy)
        .build();
  }

  private void mockForRequisitionCount() {
    when(requisitionsRepository.findRequisitionIdsByOrderInfo(any(), any(), any(), anyBoolean(), anyList())).thenReturn(
        buildMockRequisitions());
    when(requisitionStatusChangeRepository.findByRequisitionIdIn(anyList())).thenReturn(
        buildMockRequisitionSubmittedStatusChanges());
  }

  private List<String> buildMockRequisitions() {
    return Lists.newArrayList(requisitionId.toString());
  }

  private List<StatusChange> buildMockRequisitionStatusChanges() {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(RequisitionStatus.RELEASED);
    return Lists.newArrayList(statusChange);
  }

  private List<StatusChange> buildMockRequisitionSubmittedStatusChanges() {
    List<StatusChange> statusChanges = Lists.newArrayList();
    ZonedDateTime now = ZonedDateTime.now();
    for (int i = 0; i < 100; i++) {
      StatusChange statusChange = new StatusChange();
      Requisition requisition = new Requisition();
      requisition.setId(UUID.randomUUID());
      statusChange.setRequisition(requisition);
      statusChange.setCreatedDate(now.plusSeconds(i));
      statusChange.setStatus(RequisitionStatus.SUBMITTED);
      statusChanges.add(statusChange);
    }
    StatusChange statusChange = new StatusChange();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    statusChange.setRequisition(requisition);
    statusChange.setCreatedDate(now.plusDays(1));
    statusChange.setStatus(RequisitionStatus.SUBMITTED);
    statusChanges.add(statusChange);
    return statusChanges;
  }

  private List<StatusChange> buildMockRequisitionStatusChangesWithNoReleasedStatus() {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(RequisitionStatus.APPROVED);
    return Lists.newArrayList(statusChange);
  }

  private List<PodLineItemDto> buildMockPodLineItemDtos() {
    PodLineItemDto dto = PodLineItemDto.builder()
        .productCode(productCode)
        .productName(productName)
        .lotId(lotId)
        .lotCode(lotCode)
        .lotExpirationDate(LocalDate.now().minusMonths(1L))
        .requestedQuantity(1L)
        .orderedQuantity(1L)
        .receivedQuantity(1L)
        .build();
    return Lists.newArrayList(dto);
  }

  private FacilityDto buildMockFacilityDtoWithLevel3() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 3));
    GeographicZoneDto parentDto = new GeographicZoneDto();
    parentDto.setName("province");
    zoneDto.setParent(parentDto);

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private FacilityDto buildMockFacilityDtoWithLevel2() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 2));
    GeographicZoneDto parentDto = new GeographicZoneDto();
    parentDto.setName("province");
    zoneDto.setParent(parentDto);

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private FacilityDto buildMockFacilityDtoWithLevel3AndNoParent() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 3));

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private OrderDto buildMockOrderDto() {
    return OrderDto.builder()
        .id(orderId)
        .emergency(Boolean.FALSE)
        .receivingFacilityId(facilityId)
        .supplyingFacilityId(facilityId)
        .receivingFacilityCode(facilityCode)
        .requisitionId(requisitionId)
        .processingPeriodId(processingPeriodId)
        .periodEndDate(Date.from(Instant.now()))
        .build();
  }

  private OrderDto buildMockOrderDtoWithOutRequisitionId() {
    return OrderDto.builder()
        .id(orderId)
        .emergency(Boolean.TRUE)
        .receivingFacilityId(facilityId)
        .supplyingFacilityId(facilityId)
        .receivingFacilityCode(facilityCode)
        .processingPeriodId(processingPeriodId)
        .externalId(requisitionId)
        .periodEndDate(Date.from(Instant.now()))
        .build();
  }

  private List<SubDraftInfo> toSubDraftInfos(List<PodSubDraft> podSubDrafts) {
    return podSubDrafts.stream().map(podSubDraft ->
            SubDraftInfo.builder()
                .subDraftId(podSubDraft.getId())
                .groupNum(podSubDraft.getNumber())
                .saver(authenticationHelper.getUserNameByUserId(podSubDraft.getOperatorId()))
                .status(podSubDraft.getStatus())
                .build())
        .collect(Collectors.toList());
  }

  private PodExtensionResponse buildMockPodExtensionResponse() {
    PodExtensionResponse response = new PodExtensionResponse();
    response.setPodDto(buildMockPodDtoWithNoLineItems());
    response.setPreparedBy(preparedBy);
    response.setConferredBy(conferredBy);
    return response;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithNoLineItems() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    podDto.setShipment(buildMockShipmentDto());
    podDto.setLineItems(Lists.newArrayList());
    return podDto;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithOneLineItem() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    podDto.setShipment(buildMockShipmentDto());

    ProofOfDeliveryLineItemDto lineItemDto = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto.setOrderable(orderableDto);
    lineItemDto.setId(lineItemId1);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    lineItemDtos.add(lineItemDto);

    podDto.setLineItems(lineItemDtos);

    return podDto;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithTwoLineItems() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setShipment(buildMockShipmentDto());

    ProofOfDeliveryLineItemDto lineItemDto1 = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto1.setOrderable(orderableDto);
    lineItemDto1.setId(lineItemId1);

    ProofOfDeliveryLineItemDto lineItemDto2 = new ProofOfDeliveryLineItemDto();
    BeanUtils.copyProperties(lineItemDto1, lineItemDto2);
    lineItemDto2.setId(lineItemId2);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    lineItemDtos.add(lineItemDto1);
    lineItemDtos.add(lineItemDto2);

    podDto.setLineItems(lineItemDtos);

    return podDto;
  }

  private ShipmentObjectReferenceDto buildMockShipmentDto() {
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderDtoId);
    orderDto.setExternalId(externalId);
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(shipmentDtoId);
    shipmentDto.setOrder(orderDto);
    return shipmentDto;
  }

  private List<PodSubDraft> buildMockSubDrafts() {
    return Lists.newArrayList(buildMockSubDraftNotYetStarted());
  }

  private List<PodSubDraft> buildMockSubDraftsAllSubmitted() {
    return Lists.newArrayList(buildMockSubDraftSubmitted());
  }

  private PodSubDraft buildMockSubDraftSubmitted() {
    PodSubDraft podSubDraft = PodSubDraft.builder()
        .podId(podId)
        .number(1)
        .status(PodSubDraftStatusEnum.SUBMITTED)
        .build();
    podSubDraft.setId(podId);
    return podSubDraft;
  }

  private PodSubDraft buildMockSubDraftNotYetStarted() {
    PodSubDraft podSubDraft = PodSubDraft.builder()
        .podId(podId)
        .number(1)
        .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
        .build();
    podSubDraft.setId(podId);
    return podSubDraft;
  }

  private List<Orderable> buildMockOrderables() {
    Orderable orderable = new Orderable(Code.code(productCode), null, 0, 0, Boolean.FALSE, orderableId, 0L);
    return Lists.newArrayList(orderable);
  }

  private List<PodLineItemsExtension> buildMockPodLineItemsExtensions() {
    PodLineItemsExtension lineItemsExtension1 = PodLineItemsExtension.builder()
        .subDraftId(subDraftId)
        .podLineItemId(lineItemId1)
        .build();
    return Lists.newArrayList(lineItemsExtension1);
  }

  private List<ProofOfDeliveryLineItem> buildMockPodLineItems() {
    ProofOfDeliveryLineItem lineItem = new ProofOfDeliveryLineItem(null, UUID.randomUUID(), 10, null, 0,
        UUID.randomUUID(), "test notes");
    lineItem.setId(lineItemId1);
    return Lists.newArrayList(lineItem);
  }
}
