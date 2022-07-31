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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
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

  private final UUID externalId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID podId = UUID.randomUUID();
  private final UUID subDraftId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final UUID lineItemId2 = UUID.randomUUID();
  private final String productCode = "product code";

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
        .getPodDto(UUID.randomUUID(), Collections.emptySet());

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
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId))
        .thenReturn("requisitionNumber");

    // when
    ProofOfDeliveryDto proofOfDeliveryDto = service.getPodDto(UUID.randomUUID(),
        Collections.emptySet());

    // then
    assertEquals("requisitionNumber",
        proofOfDeliveryDto.getShipment().getOrder().getRequisitionNumber());
  }

  @Test
  public void shouldSuccessWhenCreateSubDrafts() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(buildMockPodDto());
    when(podSubDraftRepository.save(anyList())).thenReturn(buildMockSubDrafts());
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId))).thenReturn(buildMockOrderables());

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
    service.createSubDrafts(podId, request);

    // then
    verify(podSubDraftRepository, times(1)).save(any(List.class));
    verify(podLineItemsExtensionRepository, times(1)).save(any(List.class));
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

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenCreateSubDraftsWithSubDraftsExisted() {
    // given
    when(podSubDraftRepository.count(any())).thenReturn(1L);

    // when
    CreatePodSubDraftRequest request = new CreatePodSubDraftRequest();
    request.setSplitNum(1);
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
    ProofOfDeliveryDto subDraftDetail = service.getSubDraftDetail(podId, subDraftId);

    // then
    assertEquals(subDraftDetail.getLineItems().size(), 1);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenGetSubDraftDetailWithSubDraftNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(null);

    // when
    service.getSubDraftDetail(podId, subDraftId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenGetSubDraftDetailWithPodIdAndSubDraftIdNotMatch() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());

    // when
    service.getSubDraftDetail(UUID.randomUUID(), subDraftId);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenGetSubDraftDetailWithPodIdNotExist() {
    // given
    when(fulfillmentService.searchProofOfDelivery(any(), any())).thenReturn(null);

    // when
    service.getSubDraftDetail(podId, subDraftId);
  }

  @Test
  public void shouldSuccessWhenSaveSubDraft() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(podLineItemsRepository.findAll(Lists.newArrayList(lineItemId1))).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDto());
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);

    // then
    verify(podLineItemsRepository, times(1)).save(any(List.class));
    verify(podSubDraftRepository, times(1)).save(any(PodSubDraft.class));
  }

  @Test
  public void shouldSuccessWhenSubmitSubDraft() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftNotYetStarted());
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    when(podLineItemsExtensionRepository.findAll(example)).thenReturn(buildMockPodLineItemsExtensions());
    when(podLineItemsRepository.findAll(Lists.newArrayList(lineItemId1))).thenReturn(buildMockPodLineItems());
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDto());
    request.setOperateType(OperateTypeEnum.SUBMIT);
    service.updateSubDraft(request, subDraftId);

    // then
    verify(podLineItemsRepository, times(1)).save(any(List.class));
    verify(podSubDraftRepository, times(1)).save(any(PodSubDraft.class));
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowWhenUpdateSubDraftWithSubDraftIdNotExist() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(null);

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDto());
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenUpdateSubDraftWithSubDraftSubmitted() {
    // given
    when(podSubDraftRepository.findOne(subDraftId)).thenReturn(buildMockSubDraftSubmitted());

    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDto());
    request.setOperateType(OperateTypeEnum.SUBMIT);
    service.updateSubDraft(request, subDraftId);
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

  private ProofOfDeliveryDto buildMockPodDto() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    ProofOfDeliveryLineItemDto lineItemDto = new ProofOfDeliveryLineItemDto("serviceUrl",
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), "serviceUrl", "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto.setOrderable(orderableDto);
    lineItemDtos.add(lineItemDto);

    podDto.setLineItems(lineItemDtos);

    return podDto;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithTwoLineItems() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    ProofOfDeliveryLineItemDto lineItemDto1 = new ProofOfDeliveryLineItemDto("serviceUrl",
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), "serviceUrl", "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto1.setOrderable(orderableDto);
    lineItemDto1.setId(lineItemId1);

    ProofOfDeliveryLineItemDto lineItemDto2 = new ProofOfDeliveryLineItemDto();
    BeanUtils.copyProperties(lineItemDto1, lineItemDto2);
    lineItemDto2.setId(lineItemId2);

    lineItemDtos.add(lineItemDto1);
    lineItemDtos.add(lineItemDto2);

    podDto.setLineItems(lineItemDtos);

    return podDto;
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
