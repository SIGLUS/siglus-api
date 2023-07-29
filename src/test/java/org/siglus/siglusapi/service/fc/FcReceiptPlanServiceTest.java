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

package org.siglus.siglusapi.service.fc;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SKIPPED;
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;
import static org.siglus.siglusapi.service.fc.FcVariables.START_DATE;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateColumnDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.siglusapi.domain.ReceiptPlan;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.event.fc.receiptplan.FcReceiptPlanEmitter;
import org.siglus.siglusapi.repository.ReceiptPlanRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.LocalMachineHelper;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.data.domain.Page;

@RunWith(MockitoJUnitRunner.class)
public class FcReceiptPlanServiceTest {

  @InjectMocks
  private FcReceiptPlanService fcReceiptPlanService;

  @Mock
  private ReceiptPlanRepository receiptPlanRepository;

  @Mock
  private SiglusSimulateUserAuthHelper siglusSimulateUserAuthHelper;

  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;

  @Mock
  private SiglusUserReferenceDataService userReferenceDataService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private OperatePermissionService operatePermissionService;

  @Mock
  private FcValidate fcDataValidate;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private SiglusStatusChangeRepository siglusStatusChangeRepository;

  @Mock
  private LocalMachineHelper localMachineHelper;

  @Mock
  private Machine machine;

  @Mock
  private FcReceiptPlanEmitter fcReceiptPlanEmitter;

  private final ZonedDateTime date = ZonedDateTime.now();

  private final String fnmCode = "productCode";

  private final List<ReceiptPlanDto> receiptPlanDtos = new ArrayList<>();

  private ReceiptPlanDto receiptPlanDto;

  private ReceiptPlan receiptPlan;

  @SuppressWarnings("unchecked")
  private final Page<UserDto> page = (Page<UserDto>) mock(Page.class);

  @Before
  public void prepare() {
    String receiptPlanNumber = "receiptPlanNumber";
    String clientName = "facilityName";
    String clientCode = "facilityCode";
    String productDescription = "productName";
    Integer approvedQuantity = 2;

    ProductDto productDto = ProductDto.builder()
        .fnmCode(fnmCode)
        .productDescription(productDescription)
        .approvedQuantity(approvedQuantity)
        .build();
    List<ProductDto> productDtos = newArrayList(productDto);

    String requisitionNumber = "requisitionNumber";
    receiptPlanDto = ReceiptPlanDto.builder()
        .receiptPlanNumber(receiptPlanNumber)
        .requisitionNumber(requisitionNumber)
        .clientCode(clientCode)
        .clientName(clientName)
        .date(date)
        .products(productDtos)
        .build();

    receiptPlan = ReceiptPlan.builder()
        .receiptPlanNumber(receiptPlanNumber)
        .build();

    receiptPlanDtos.add(receiptPlanDto);
    doNothing().when(fcDataValidate).validateFacility(any());
    doNothing().when(fcDataValidate).validateExistUser(any());
  }

  @Test
  public void shouldReturnFalseGivenEmptyReceiptPlan() {

    // when
    FcIntegrationResultDto result = fcReceiptPlanService.processData(emptyList(), START_DATE, LAST_UPDATED_AT);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnFalseIfCatchExceptionWhenReceiptPlan() {
    // given
    Facility facility = new Facility();
    UserDto userDto = new UserDto();
    UUID userId = UUID.randomUUID();
    userDto.setId(userId);
    List<UserDto> userDtoList = newArrayList(userDto);
    when(receiptPlanRepository.findByReceiptPlanNumberIn(any())).thenReturn(newArrayList(receiptPlan));
    when(siglusFacilityRepository.findFirstByTypeId(any())).thenReturn(facility);
    when(userReferenceDataService.getUserInfo(any())).thenReturn(page);
    when(page.getContent()).thenReturn(userDtoList);
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcReceiptPlanService.processData(receiptPlanDtos, START_DATE, LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertFalse(result.getFinalSuccess());
  }

  @Test
  public void shouldNotSaveReceiptPlanIfCRequisitionNumberNotExisted() {
    // given
    Facility facility = new Facility();
    UserDto userDto = new UserDto();
    UUID userId = UUID.randomUUID();
    userDto.setId(userId);
    List<UserDto> userDtoList = newArrayList(userDto);
    when(receiptPlanRepository.findByReceiptPlanNumberIn(any())).thenReturn(newArrayList(receiptPlan));
    when(siglusFacilityRepository.findFirstByTypeId(any())).thenReturn(facility);
    when(userReferenceDataService.getUserInfo(any())).thenReturn(page);
    when(page.getContent()).thenReturn(userDtoList);
    receiptPlanDto.setRequisitionNumber(null);
    List<ReceiptPlanDto> inValidReceiptPlan = newArrayList(receiptPlanDto);

    // when
    FcIntegrationResultDto result = fcReceiptPlanService.processData(inValidReceiptPlan, START_DATE, LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertTrue(result.getFinalSuccess());
    verify(receiptPlanRepository, times(0)).save(any(ReceiptPlan.class));
  }

  @Test
  public void shouldReturnTrueGivenNotEditableRequisition() {
    // given
    UserDto userDto = new UserDto();
    UUID userId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    userDto.setId(userId);
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    RequisitionLineItemV2Dto requisitionLineItem = new RequisitionLineItemV2Dto();
    VersionObjectReferenceDto versionObjectReferenceDto = new VersionObjectReferenceDto();
    versionObjectReferenceDto.setId(orderableId);
    requisitionLineItem.setOrderable(versionObjectReferenceDto);
    List<RequisitionLineItemV2Dto> requisitionLineItems = newArrayList(requisitionLineItem);
    UUID requisitionId = UUID.randomUUID();
    requisitionV2Dto.setId(requisitionId);
    requisitionV2Dto.setRequisitionLineItems(requisitionLineItems);

    BasicRequisitionTemplateColumnDto columnDto = mock(BasicRequisitionTemplateColumnDto.class);
    Map<String, BasicRequisitionTemplateColumnDto> columnDtoMap = new HashMap<>();
    columnDtoMap.put(SKIPPED.toString().toLowerCase(), columnDto);
    when(columnDto.getIsDisplayed()).thenReturn(true);

    BasicRequisitionTemplateDto template = mock(BasicRequisitionTemplateDto.class);
    when(template.getColumnsMap()).thenReturn(columnDtoMap);

    SiglusRequisitionDto requisitionDto = mock(SiglusRequisitionDto.class);
    when(requisitionDto.getTemplate()).thenReturn(template);
    when(requisitionDto.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);
    UUID facilityId = UUID.randomUUID();
    when(requisitionDto.getFacilityId()).thenReturn(facilityId);

    SiglusRequisitionLineItemDto lineItem = new SiglusRequisitionLineItemDto(requisitionLineItem, null);
    List<SiglusRequisitionLineItemDto> lineItems = newArrayList(lineItem);
    when(siglusRequisitionService.createRequisitionLineItem(any(), any())).thenReturn(lineItems);
    when(receiptPlanRepository.findByReceiptPlanNumberIn(any())).thenReturn(emptyList());
    Facility facility = new Facility();
    when(siglusFacilityRepository.findFirstByTypeId(any())).thenReturn(facility);
    when(userReferenceDataService.getUserInfo(any())).thenReturn(page);
    List<UserDto> userDtoList = newArrayList(userDto);
    when(page.getContent()).thenReturn(userDtoList);
    RequisitionExtension requisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionId)
        .requisitionNumber(121).build();
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(requisitionExtension);
    when(siglusRequisitionService.searchRequisitionForFc(any())).thenReturn(requisitionDto);
    when(operatePermissionService.isEditable(any())).thenReturn(true);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode(fnmCode);
    orderableDto.setId(orderableId);
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    approvedProduct.setOrderable(orderableDto);
    when(requisitionService.getAllApprovedProducts(any(), any())).thenReturn(singletonList(approvedProduct));
    Requisition requisition = new Requisition();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    StatusChange statusChange = new StatusChange();
    when(siglusStatusChangeRepository.findByRequisitionIdAndStatus(any(), any())).thenReturn(statusChange);
    when(machine.isOnlineWeb()).thenReturn(true);
    when(localMachineHelper.isLocalMachine(facilityId)).thenReturn(true);

    // when
    FcIntegrationResultDto result = fcReceiptPlanService.processData(receiptPlanDtos, START_DATE, LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertTrue(result.getFinalSuccess());
    verify(receiptPlanRepository).save(any(ReceiptPlan.class));
    ArgumentCaptor<UUID> captorUserId = ArgumentCaptor.forClass(UUID.class);
    verify(siglusSimulateUserAuthHelper).simulateUserAuth(captorUserId.capture());
    assertEquals(userId, captorUserId.getValue());
    verify(fcReceiptPlanEmitter, times(1)).emit(any(ReceiptPlanDto.class), eq(facilityId), any());
  }

  @Test
  public void shouldReturnFalseIfCatchFcDataExceptionWhenProductsNotExist() {
    // given
    UserDto userDto = new UserDto();
    UUID userId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    userDto.setId(userId);
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    RequisitionLineItemV2Dto requisitionLineItem = new RequisitionLineItemV2Dto();
    VersionObjectReferenceDto versionObjectReferenceDto = new VersionObjectReferenceDto();
    versionObjectReferenceDto.setId(orderableId);
    requisitionLineItem.setOrderable(versionObjectReferenceDto);
    List<RequisitionLineItemV2Dto> requisitionLineItems = newArrayList(requisitionLineItem);
    UUID requisitionId = UUID.randomUUID();
    requisitionV2Dto.setId(requisitionId);
    requisitionV2Dto.setRequisitionLineItems(requisitionLineItems);
    BasicRequisitionTemplateColumnDto columnDto = mock(BasicRequisitionTemplateColumnDto.class);
    Map<String, BasicRequisitionTemplateColumnDto> columnDtoMap = new HashMap<>();
    columnDtoMap.put(SKIPPED.toString().toLowerCase(), columnDto);
    when(columnDto.getIsDisplayed()).thenReturn(true);

    BasicRequisitionTemplateDto template = mock(BasicRequisitionTemplateDto.class);
    when(template.getColumnsMap()).thenReturn(columnDtoMap);

    SiglusRequisitionDto requisitionDto = mock(SiglusRequisitionDto.class);
    when(requisitionDto.getTemplate()).thenReturn(template);
    when(receiptPlanRepository.findByReceiptPlanNumberIn(any())).thenReturn(newArrayList(receiptPlan));
    Facility facility = new Facility();
    when(siglusFacilityRepository.findFirstByTypeId(any())).thenReturn(facility);
    when(userReferenceDataService.getUserInfo(any())).thenReturn(page);
    List<UserDto> userDtoList = newArrayList(userDto);
    when(page.getContent()).thenReturn(userDtoList);
    RequisitionExtension requisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionId)
        .requisitionNumber(121).build();
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(requisitionExtension);
    when(siglusRequisitionService.searchRequisitionForFc(any())).thenReturn(requisitionDto);
    when(operatePermissionService.isEditable(any())).thenReturn(true);
    OrderableDto orderableDto = new OrderableDto();
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    approvedProduct.setOrderable(orderableDto);
    when(requisitionService.getAllApprovedProducts(any(), any())).thenReturn(singletonList(approvedProduct));
    SiglusRequisitionLineItemDto lineItem = new SiglusRequisitionLineItemDto(requisitionLineItem, null);
    List<SiglusRequisitionLineItemDto> lineItems = newArrayList(lineItem);
    when(siglusRequisitionService.createRequisitionLineItem(any(), any())).thenReturn(lineItems);
    Requisition requisition = new Requisition();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    StatusChange statusChange = new StatusChange();
    when(siglusStatusChangeRepository.findByRequisitionIdAndStatus(any(), any())).thenReturn(statusChange);

    // when
    FcIntegrationResultDto result = fcReceiptPlanService.processData(receiptPlanDtos, START_DATE, LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertFalse(result.getFinalSuccess());
  }

}
