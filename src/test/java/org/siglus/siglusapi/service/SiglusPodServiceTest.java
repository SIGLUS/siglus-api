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

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

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
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.fulfillment.web.util.StockEventBuilder;
import org.openlmis.fulfillment.web.util.UserObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.domain.PodSubDraftLineItemsByLocation;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.PodLineItemWithLocationDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftLineItemDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftWithLocationDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEmitter;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEvent;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftLineItemRepository;
import org.siglus.siglusapi.repository.PodSubDraftLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.testutils.StockEventLineItemDtoDataBuilder;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.PodExtensionRequest;
import org.siglus.siglusapi.web.request.PodWithLocationRequest;
import org.siglus.siglusapi.web.request.SubmitPodSubDraftsRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsMergedResponse;
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
  private SiglusOrdersRepository siglusOrdersRepository;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private StatusChangeRepository requisitionStatusChangeRepository;

  @Mock
  private PodExtensionRepository podExtensionRepository;

  @Mock
  private PodLineItemsByLocationRepository podLineItemsByLocationRepository;

  @Mock
  private PodSubDraftLineItemsByLocationRepository podSubDraftLineItemsByLocationRepository;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Mock
  private StockEventBuilder stockEventBuilder;

  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;

  @Mock
  private StockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private ProofOfDeliveryEmitter proofOfDeliveryEmitter;

  @Mock
  private SiglusStockEventsService stockEventsService;

  @Mock
  private PodSubDraftLineItemRepository podSubDraftLineItemRepository;

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
  private final UUID userId = UUID.randomUUID();
  private final Set<String> defaultExpands = Sets.newHashSet("shipment.order");
  private final String locationCode = "ABC";
  private final String area = "DEF";
  private final String resourceName = "resourceName";
  private final String notes = "test notes";
  private final UUID stockCardId = UUID.randomUUID();
  private final UUID stockCardLineItemId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final String programCode = "program code";

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

  @Test(expected = BusinessDataException.class)
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
    ProofOfDeliverySubDraftDto subDraftDetail = service.getSubDraftDetail(podId, subDraftId, defaultExpands);

    // then
    assertEquals(1, subDraftDetail.getLineItems().size());
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
    request.setPodDto(buildPodDraftDto(3));
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
    request.setPodDto(buildPodDraftDto(2));
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
    request.setPodDto(buildPodDraftDto(1));
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenUpdateSubDraftWithSubDraftSubmitted() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildPodDraftDto(10));
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
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(podExtensionRepository.findOne(
        Example.of(PodExtension.builder().podId(podId).build()))).thenReturn(
        new PodExtension());
    // when
    PodSubDraftsMergedResponse actualResponse = service.mergeSubDrafts(podId, defaultExpands);

    assertEquals(podId, actualResponse.getPodDto().getId());
    assertEquals(1, actualResponse.getPodDto().getLineItems().size());
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
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    ProofOfDeliveryDto dto = request.getPodDto().to();
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    when(podController.updateProofOfDelivery(podId, dto, null, false)).thenReturn(dto);
    when(proofOfDeliveryEmitter.emit(podId)).thenReturn(new ProofOfDeliveryEvent());
    when(proofOfDeliveryRepository.findOne(podId)).thenReturn(buildMockProofOfDelivery());

    // when
    service.submitSubDrafts(podId, request, null, false);

    // then
    verify(podController).updateProofOfDelivery(any(), any(), any(), any());
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
    verify(notificationService).postConfirmPod(dto);
  }

  @Test
  public void shouldReturnWhenSubmitSubDraftsWithPodExtensionNull() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    ProofOfDeliveryDto dto = request.getPodDto().to();
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    when(podController.updateProofOfDelivery(podId, dto, null, false)).thenReturn(dto);
    when(proofOfDeliveryRepository.findOne(podId)).thenReturn(buildMockProofOfDelivery());
    mockPodExtensionQuery();
    when(proofOfDeliveryEmitter.emit(podId)).thenReturn(new ProofOfDeliveryEvent());

    // when
    service.submitSubDrafts(podId, request, null, false);

    // then
    verify(podController).updateProofOfDelivery(any(), any(), any(), any());
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
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    ProofOfDeliveryDto dto = request.getPodDto().to();
    dto.setStatus(ProofOfDeliveryStatus.INITIATED);
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    when(podController.updateProofOfDelivery(any(), any(), any(), any())).thenReturn(dto);
    when(proofOfDeliveryRepository.findOne(podId)).thenReturn(buildMockProofOfDelivery());
    when(proofOfDeliveryEmitter.emit(podId)).thenReturn(new ProofOfDeliveryEvent());

    // when
    service.submitSubDrafts(podId, request, null, false);

    // then
    verify(podController).updateProofOfDelivery(any(), any(), any(), any());
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
    verify(notificationService, times(0)).postConfirmPod(any());
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowWhenSubmitSubDraftsWithNoPermission() {
    // given
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.FALSE);

    // when
    service.submitSubDrafts(podId, request, null, false);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenSubmitSubDraftsWithSubDraftsNotExist() {
    // given
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(null);

    // when
    service.submitSubDrafts(podId, request, null, false);
  }

  @Test
  public void shouldReturnWhenGetPintInfo() {
    // given
    OrderDto orderDto = buildMockOrderDto();
    when(siglusOrdersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel3());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChanges());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);
    mockPodExtensionQuery();

    // when
    PodPrintInfoResponse response = service.getPrintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNotNull(response.getSupplierDistrict());
  }

  @Test
  public void shouldReturnWithSubOrderNumberWhenGetSubOrderPintInfo() {
    // given
    OrderDto orderDto = buildMockSubOrderDto();
    when(siglusOrdersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel3());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChanges());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);
    mockPodExtensionQuery();

    // when
    PodPrintInfoResponse response = service.getPrintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNotNull(response.getSupplierDistrict());
    assertTrue(response.getFileName().endsWith("01"));
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithDifferentDbResult() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(siglusOrdersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel2());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPrintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNull(response.getSupplierDistrict());
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithZoneNull() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(siglusOrdersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId())).thenReturn(
        new FacilityDto());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPrintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNull(response.getSupplierDistrict());
    assertNull(response.getSupplierProvince());
  }

  @Test
  public void shouldReturnWhenGetPintInfoWithParentZoneNull() {
    // given
    OrderDto orderDto = buildMockOrderDtoWithOutRequisitionId();
    when(siglusOrdersRepository.findOrderDtoById(orderId)).thenReturn(orderDto);
    mockForRequisitionCount();
    when(siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId())).thenReturn(
        buildMockFacilityDtoWithLevel3AndNoParent());
    when(requisitionStatusChangeRepository.findByRequisitionId(requisitionId)).thenReturn(
        buildMockRequisitionStatusChangesWithNoReleasedStatus());
    when(podLineItemsRepository.lineItemDtos(podId, orderId, requisitionId)).thenReturn(buildMockPodLineItemDtos());
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn(requisitionNum);

    // when
    PodPrintInfoResponse response = service.getPrintInfo(orderId, podId);

    // then
    assertNotNull(response);
    assertNotNull(response.getSupplierDistrict());
    assertNull(response.getSupplierProvince());
  }

  @Test
  public void shouldGetSubDraftWithLocation() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(buildMockPodDtoWithOneLineItem());
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId)).thenReturn(requisitionNum);
    mockPodExtensionQuery();
    when(podLineItemsByLocationRepository.findByPodLineItemIdIn(Lists.newArrayList(lineItemId1)))
        .thenReturn(Lists.newArrayList(buildMockPodLineItemsByLocation()));
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(podSubDraftLineItemsByLocationRepository.findByPodLineItemIdIn(Lists.newArrayList(lineItemId1)))
        .thenReturn(Lists.newArrayList(buildMockPodSubDraftLineItemsByLocation()));
    // when
    ProofOfDeliverySubDraftWithLocationDto podSubDraftWithLocation =
        service.getPodSubDraftWithLocation(podId, subDraftId);

    // then
    assertEquals(locationCode, podSubDraftWithLocation.getPodLineItemLocation().get(0).getLocationCode());
    assertEquals(area, podSubDraftWithLocation.getPodLineItemLocation().get(0).getArea());
  }

  @Test
  public void shouldUpdateSubDraftWithLocation() {
    // given
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildPodDraftDto(10));
    request.setOperateType(OperateTypeEnum.SUBMIT);
    request.setPodLineItemLocation(Lists.newArrayList(buildMockPodLineItemWithLocationDto()));
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    when(podLineItemsExtensionRepository.findAll()).thenReturn(null);
    when(podLineItemsRepository.findAll(Sets.newHashSet(lineItemId1))).thenReturn(null);
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    service.updateSubDraftWithLocation(request, subDraftId);

    // then
    verify(podSubDraftLineItemsByLocationRepository, times(1))
        .save(anyCollection());
    verify(podSubDraftLineItemsByLocationRepository, times(0))
        .deleteByPodLineItemIdIn(Lists.newArrayList());
  }

  @Test
  public void shouldSuccessDeleteSubDraftWithLocation() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithTwoLineItems());
    when(podLineItemsRepository.findAll(anySet())).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    service.deleteSubDraftWithLocation(podId, subDraftId);

    // then
    verify(podLineItemsRepository, times(1)).save(any(List.class));
    verify(podSubDraftRepository, times(1)).save(any(PodSubDraft.class));
    verify(podSubDraftLineItemsByLocationRepository, times(1))
        .deleteByPodLineItemIdIn(any(Set.class));
  }

  @Test
  public void shouldSuccessDeleteSubDraftsWithLocation() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithTwoLineItems());
    when(podLineItemsExtensionRepository.findAllBySubDraftIds(Lists.newArrayList(subDraftId))).thenReturn(
        buildMockPodLineItemsExtensions());

    // when
    service.deleteSubDraftsWithLocation(podId);

    // then
    verify(podLineItemsRepository).save(any(List.class));
    verify(podSubDraftRepository).deleteAllByIds(any(List.class));
    verify(podLineItemsExtensionRepository).deleteAllBySubDraftIds(any(List.class));
    verify(podSubDraftLineItemsByLocationRepository).deleteByPodLineItemIdIn(any(List.class));
  }

  @Test
  public void shouldGetMergedSubDraftWithLocation() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDtoWithOneLineItemInitial());
    when(podSubDraftLineItemsByLocationRepository.findByPodLineItemIdIn(Lists.newArrayList(lineItemId1)))
        .thenReturn(Lists.newArrayList(buildMockPodSubDraftLineItemsByLocation()));

    // when
    PodSubDraftsMergedResponse mergedSubDraftWithLocation = service.getMergedSubDraftWithLocation(podId);

    // then
    assertEquals(mergedSubDraftWithLocation.getPodLineItemLocation().get(0).getLocationCode(), locationCode);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrewIllegalArgumentExceptionWhenPodIdMismatch() {
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();

    service.submitSubDrafts(podId, request, null, true);
  }

  @Test
  public void shouldSuccessSubmitSubDraftsWithLocation() {
    // given
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(Boolean.TRUE);
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    when(podSubDraftRepository.findAll(example)).thenReturn(buildMockSubDraftsAllSubmitted());
    SubmitPodSubDraftsRequest request = buildSubmitPodSubDraftRequest();
    request.getPodDto().setId(podId);
    ProofOfDeliveryDto dto = request.getPodDto().to();
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(dto);
    ProofOfDelivery proofOfDelivery = buildMockProofOfDelivery();
    when(proofOfDeliveryRepository.findOne(podId)).thenReturn(proofOfDelivery);
    StockEventDto stockEventDto = buildMockStockEventDto();
    when(stockEventBuilder.fromProofOfDelivery(any())).thenReturn(stockEventDto);
    when(podController.updateProofOfDelivery(any(), any(), any(), any())).thenReturn(dto);
    when(siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(any(), any()))
        .thenReturn(Lists.newArrayList(buildStockCard()));
    when(stockCardLineItemRepository.findLatestByStockCardIds(any()))
        .thenReturn(com.google.common.collect.Lists.newArrayList(buildStockCardLineItem()));
    doNothing().when(stockEventsService).processStockEvent(buildStockManagementStockEvent(), false);


    // when
    service.submitSubDrafts(podId, request, null, true);

    // then
    verify(podLineItemsByLocationRepository, times(1)).save(any(List.class));
  }

  @Test
  public void shouldCreatePodSubDraftLineItemSuccess() {
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    when(podLineItemsExtensionRepository.findByPodLineItemId(lineItemId1))
        .thenReturn(PodLineItemsExtension.builder().podLineItemId(lineItemId1)
            .subDraftId(subDraftId).build());
    when(podLineItemsRepository.findOne(lineItemId1)).thenReturn(buildMockPodLineItems().get(0));

    service.createPodSubDraftLineItem(podId, subDraftId, lineItemId1);

    verify(podSubDraftLineItemRepository, times(1)).save(any(PodSubDraftLineItem.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenCreatePodSubDraftLineItemGivenSubDraftIdAndLineItemIdMismatch() {
    UUID podLineItemId = UUID.randomUUID();
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    when(podLineItemsExtensionRepository.findByPodLineItemId(podLineItemId))
        .thenReturn(PodLineItemsExtension.builder().podLineItemId(podLineItemId)
            .subDraftId(UUID.randomUUID()).build());

    service.createPodSubDraftLineItem(podId, subDraftId, podLineItemId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenCreatePodSubDraftLineItemGivenSubDraftIdAndPodIdMismatch() {
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());

    service.createPodSubDraftLineItem(UUID.randomUUID(), subDraftId, UUID.randomUUID());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenCreatePodSubDraftLineItemGivenSubDraftStatusIsSubmitted() {
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    service.deletePodSubDraftLineItem(UUID.randomUUID(), subDraftId, UUID.randomUUID());
  }

  @Test
  public void shouldDeletePodSubDraftLineItemSuccess() {
    UUID subLineItemId = UUID.randomUUID();
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    doNothing().when(podSubDraftLineItemRepository).delete(subLineItemId);

    service.deletePodSubDraftLineItem(podId, subDraftId, subLineItemId);

    verify(podSubDraftLineItemRepository, times(1)).delete(subLineItemId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenDeletePodSubDraftLineItemGivenSubDraftIdAndPodIdMismatch() {
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());

    service.deletePodSubDraftLineItem(UUID.randomUUID(), subDraftId, UUID.randomUUID());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenDeletePodSubDraftLineItemGivenSubDraftStatusIsSubmitted() {
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    service.deletePodSubDraftLineItem(UUID.randomUUID(), subDraftId, UUID.randomUUID());
  }

  private void mockPodExtensionQuery() {
    Example<PodExtension> podExtensionExample = Example.of(
        PodExtension.builder().podId(podId).build());
    when(podExtensionRepository.findOne(podExtensionExample)).thenReturn(buildMockPodExtension());
  }

  private void mockPodExtensionQueryReturnNull() {
    Example<PodExtension> podExtensionExample = Example.of(
        PodExtension.builder().podId(podId).build());
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

  private PodExtension buildMockPodExtension() {
    return PodExtension.builder()
        .preparedBy(preparedBy)
        .conferredBy(conferredBy)
        .build();
  }

  private void mockForRequisitionCount() {
    when(siglusRequisitionRepository.findRequisitionIdsByOrderInfo(any(), any(), any(), anyBoolean(), anyList()))
        .thenReturn(buildMockRequisitions());
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
        .programId(programId)
        .programCode(programCode)
        .orderCode("ORDEM-1ASJKT54R")
        .emergency(Boolean.FALSE)
        .receivingFacilityId(facilityId)
        .supplyingFacilityId(facilityId)
        .receivingFacilityCode(facilityCode)
        .requisitionId(requisitionId)
        .processingPeriodId(processingPeriodId)
        .periodEndDate(Date.from(Instant.now()))
        .build();
  }

  private OrderDto buildMockSubOrderDto() {
    return OrderDto.builder()
        .id(orderId)
        .programId(programId)
        .programCode(programCode)
        .orderCode("ORDEM-1ASJKT54R-1")
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
        .programId(programId)
        .programCode(programCode)
        .orderCode("ORDEM-1ASJKT54R")
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
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, resourceName), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), notes);
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

  private ProofOfDeliveryDto buildMockPodDtoWithOneLineItemInitial() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setStatus(ProofOfDeliveryStatus.INITIATED);
    podDto.setShipment(buildMockShipmentDto());

    ProofOfDeliveryLineItemDto lineItemDto = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, resourceName), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), notes);
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
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, resourceName), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), notes);
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
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    shipmentLineItemDto.setLotId(lotId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    shipmentLineItemDto.setOrderable(orderableDto);
    shipmentDto.setLineItems(Lists.newArrayList(shipmentLineItemDto));
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
        UUID.randomUUID(), notes);
    lineItem.setId(lineItemId1);
    return Lists.newArrayList(lineItem);
  }

  private PodLineItemsByLocation buildMockPodLineItemsByLocation() {
    PodLineItemsByLocation podLineItemsByLocation = new PodLineItemsByLocation();
    podLineItemsByLocation.setPodLineItemId(lineItemId1);
    podLineItemsByLocation.setLocationCode(locationCode);
    podLineItemsByLocation.setArea(area);
    podLineItemsByLocation.setQuantityAccepted(10);
    return podLineItemsByLocation;
  }

  private PodSubDraftLineItemsByLocation buildMockPodSubDraftLineItemsByLocation() {
    PodSubDraftLineItemsByLocation podSubDraftLineItemsByLocation = new PodSubDraftLineItemsByLocation();
    podSubDraftLineItemsByLocation.setPodLineItemId(lineItemId1);
    podSubDraftLineItemsByLocation.setLocationCode(locationCode);
    podSubDraftLineItemsByLocation.setArea(area);
    podSubDraftLineItemsByLocation.setQuantityAccepted(10);
    return podSubDraftLineItemsByLocation;
  }

  private SubmitPodSubDraftsRequest buildSubmitPodSubDraftRequest() {
    SubmitPodSubDraftsRequest request = new SubmitPodSubDraftsRequest();
    request.setPodDto(buildPodDraftDto(3));
    request.setPodLineItemLocation(Lists.newArrayList(buildMockPodLineItemWithLocationDto()));
    return request;
  }

  private ProofOfDeliverySubDraftDto buildPodDraftDto(Integer quantityAccpeted) {
    ProofOfDeliverySubDraftDto podDto = new ProofOfDeliverySubDraftDto();
    OrderObjectReferenceDto order = new OrderObjectReferenceDto();
    UserObjectReferenceDto shippedBy = UserObjectReferenceDto.create(userId, serviceUrl);
    ShipmentObjectReferenceDto shipment = new ShipmentObjectReferenceDto(order, shippedBy, ZonedDateTime.now(),
        notes, Lists.newArrayList(buildShipmentLineItemDto()), null);
    List<ProofOfDeliverySubDraftLineItemDto> podLineItems =
        Lists.newArrayList(buildPodDraftLineItemDto(quantityAccpeted));
    podDto.setShipment(shipment);
    podDto.setLineItems(podLineItems);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    return podDto;
  }

  private ProofOfDeliverySubDraftLineItemDto buildPodDraftLineItemDto(Integer quantityAccpeted) {
    ProofOfDeliverySubDraftLineItemDto draftLineItemDto = new ProofOfDeliverySubDraftLineItemDto();
    draftLineItemDto.setId(lineItemId1);
    draftLineItemDto.setOrderable(new VersionObjectReferenceDto(orderableId, serviceUrl, resourceName, 1L));
    draftLineItemDto.setLot(new ObjectReferenceDto(lotId, serviceUrl, resourceName));
    draftLineItemDto.setQuantityAccepted(quantityAccpeted);
    draftLineItemDto.setUseVvm(false);
    draftLineItemDto.setVvmStatus(null);
    draftLineItemDto.setQuantityRejected(0);
    draftLineItemDto.setRejectionReasonId(UUID.randomUUID());
    draftLineItemDto.setNotes(notes);
    return draftLineItemDto;
  }

  private ProofOfDeliveryDto buildPodDto(Integer quantityAccpeted) {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    OrderObjectReferenceDto order = new OrderObjectReferenceDto();
    UserObjectReferenceDto shippedBy = UserObjectReferenceDto.create(userId, serviceUrl);
    ShipmentObjectReferenceDto shipment = new ShipmentObjectReferenceDto(order, shippedBy, ZonedDateTime.now(),
        notes, Lists.newArrayList(buildShipmentLineItemDto()), null);
    List<ProofOfDeliveryLineItemDto> podLineItems = Lists.newArrayList(buildPodLineItemDto(quantityAccpeted));
    podDto.setShipment(shipment);
    podDto.setLineItems(podLineItems);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    return podDto;
  }

  private ProofOfDeliveryLineItemDto buildPodLineItemDto(Integer quantityAccpeted) {
    ProofOfDeliveryLineItemDto proofOfDeliveryLineItemDto = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(orderableId, serviceUrl, resourceName, 1L),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, resourceName), quantityAccpeted,
        Boolean.TRUE, null, 0,
        UUID.randomUUID(), notes);
    proofOfDeliveryLineItemDto.setLotId(lotId);
    proofOfDeliveryLineItemDto.setId(lineItemId1);
    return proofOfDeliveryLineItemDto;
  }

  private ShipmentLineItemDto buildShipmentLineItemDto() {
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    shipmentLineItemDto.setOrderable(orderableDto);
    shipmentLineItemDto.setLotId(lotId);
    shipmentLineItemDto.setId(lineItemId1);
    shipmentLineItemDto.setQuantityShipped(10L);
    return shipmentLineItemDto;
  }

  private PodLineItemWithLocationDto buildMockPodLineItemWithLocationDto() {
    PodLineItemWithLocationDto podLineItemWithLocationDto = new PodLineItemWithLocationDto();
    podLineItemWithLocationDto.setPodLineItemId(lineItemId1);
    podLineItemWithLocationDto.setLocationCode(locationCode);
    podLineItemWithLocationDto.setArea(area);
    podLineItemWithLocationDto.setQuantityAccepted(10);
    return podLineItemWithLocationDto;
  }

  private PodWithLocationRequest buildMockPodWithLocationRequest() {
    PodWithLocationRequest request = new PodWithLocationRequest();
    request.setPodDto(buildPodDto(10));
    request.setPodLineItemLocation(Lists.newArrayList(buildMockPodLineItemWithLocationDto()));
    return request;
  }

  private ProofOfDelivery buildMockProofOfDelivery() {
    VersionEntityReference orderable = new VersionEntityReference();
    orderable.setId(orderableId);
    orderable.setVersionNumber(1L);
    ShipmentLineItem shipmentLineItem = new ShipmentLineItem(orderable, 10L, null);
    Shipment shipment = new Shipment(new Order(), null, notes,
        Lists.newArrayList(shipmentLineItem), null);
    ProofOfDeliveryLineItem proofOfDeliveryLineItem = new ProofOfDeliveryLineItem(orderable, lotId, 10,
        null, 0, null, notes);
    proofOfDeliveryLineItem.setId(lineItemId1);
    return new ProofOfDelivery(shipment, ProofOfDeliveryStatus.CONFIRMED, Lists.newArrayList(proofOfDeliveryLineItem),
        "test", "test", LocalDate.now());
  }

  private StockEventDto buildMockStockEventDto() {
    StockEventDto stockEventDto = new StockEventDto();
    stockEventDto.setFacilityId(facilityId);
    stockEventDto.setUserId(userId);
    StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDto();
    stockEventLineItemDto.setId(lineItemId1);
    stockEventLineItemDto.setLotId(lotId);
    stockEventLineItemDto.setQuantityAccepted(10);
    stockEventLineItemDto.setQuantity(10);
    stockEventLineItemDto.setOrderableId(orderableId);
    stockEventDto.setLineItems(Lists.newArrayList(stockEventLineItemDto));
    return stockEventDto;
  }

  private StockCard buildStockCard() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setOrderableId(orderableId);
    stockCard.setLotId(lotId);
    return stockCard;
  }

  private StockCardLineItem buildStockCardLineItem() {
    StockCardLineItem stockCardLineItem = new StockCardLineItem();
    stockCardLineItem.setStockCard(buildStockCard());
    stockCardLineItem.setId(stockCardLineItemId);
    return stockCardLineItem;
  }

  private org.openlmis.stockmanagement.dto.StockEventDto buildStockManagementStockEvent() {
    org.openlmis.stockmanagement.dto.StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    lineItemDto1.setOrderableId(orderableId);
    return org.openlmis.stockmanagement.dto.StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
  }
}
