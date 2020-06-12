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

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_AUTHORIZE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_CREATE;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods"})
public class SiglusRequisitionServiceTest {

  @Captor
  private ArgumentCaptor<RequisitionSearchParams> requisitionSearchParamsArgumentCaptor;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private RequisitionAuthenticationHelper authenticationHelper;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  @Mock
  private PermissionService permissionService;

  @InjectMocks
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private Pageable pageable;

  private UUID facilityId = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID requisitionId = UUID.randomUUID();

  private UUID templateId = UUID.randomUUID();

  private UUID supervisoryNodeId = UUID.randomUUID();

  private UUID productId1 = UUID.randomUUID();
  private Long productVersion1 = new Long(1);
  private UUID productId2 = UUID.randomUUID();
  private Long productVersion2 = new Long(1);
  private VersionObjectReferenceDto productVersionObjectReference1
      = createVersionObjectReferenceDto(productId1, productVersion1);
  private VersionObjectReferenceDto productVersionObjectReference2
      = createVersionObjectReferenceDto(productId2, productVersion2);

  private String profilerName = "GET_REQUISITION_TO_APPROVE";

  private Profiler profiler = new Profiler(profilerName);

  private UserDto userDto = new UserDto();

  private ProgramDto programDto = new ProgramDto();

  private FacilityDto facilityDto = new FacilityDto();

  private RequisitionV2Dto requisitionV2Dto = createRequisitionV2Dto();

  private Requisition requisition = createRequisition();

  @Before
  public void prepare() {
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(requisitionV2Dto);
    when(requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(templateId)).thenReturn(createTemplate());
    when(requisitionController.getProfiler(
        profilerName, requisitionId)).thenReturn(profiler);
    when(requisitionController.findRequisition(requisitionId, profiler)).thenReturn(requisition);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(requisitionController.findProgram(programId, profiler)).thenReturn(programDto);
    when(requisitionController.findFacility(facilityId, profiler)).thenReturn(facilityDto);
    when(requisitionService.getApproveProduct(any(), any(), any()))
        .thenReturn(createApproveProductsAggregator());
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(createSupervisoryNodeDto());
  }

  @Test
  public void shouldActivateArchivedProducts() {
    Requisition requisition = createRequisition();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);

    siglusRequisitionService.activateArchivedProducts(requisitionId, facilityId);

    verify(archiveProductService)
        .activateArchivedProducts(Sets.newHashSet(orderableId), facilityId);
  }

  @Test
  public void shouldSearchRequisitionByIdAndKeepApproverApprovedProductIfInApprovePage() {
    when(requisitionService
        .validateCanApproveRequisition(any(), any())).thenReturn(ValidationResult.success());

    RequisitionV2Dto response = siglusRequisitionService.searchRequisition(requisitionId);

    verify(siglusRequisitionRequisitionService).searchRequisition(requisitionId);
    verify(requisitionTemplateExtensionRepository).findByRequisitionTemplateId(templateId);
    verify(requisitionController).getProfiler(profilerName, requisitionId);
    verify(requisitionController).findRequisition(requisitionId, profiler);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());
    verify(authenticationHelper).getCurrentUser();
    verify(requisitionController).findProgram(programId, profiler);
    verify(requisitionController).findFacility(null, profiler);
    verify(supervisoryNodeService).findOne(null);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());

    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    assertEquals(1, availableProducts.size());
    assertTrue(availableProducts.contains(productVersionObjectReference2));
  }

  @Test
  public void shouldSearchRequisitionIdIfNotInApprovePage() {
    when(requisitionService
        .validateCanApproveRequisition(any(), any()))
        .thenReturn(ValidationResult.noPermission("no approve permission"));

    RequisitionV2Dto response = siglusRequisitionService.searchRequisition(requisitionId);

    verify(siglusRequisitionRequisitionService).searchRequisition(requisitionId);
    verify(requisitionTemplateExtensionRepository).findByRequisitionTemplateId(templateId);
    verify(requisitionController).getProfiler(profilerName, requisitionId);
    verify(requisitionController).findRequisition(requisitionId, profiler);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());

    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    assertEquals(2, availableProducts.size());
    assertTrue(availableProducts.contains(productVersionObjectReference1));
    assertTrue(availableProducts.contains(productVersionObjectReference2));
  }

  @Test
  public void shouldAddRequisitionStatusesDisplayWhenCanAuth() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(ValidationResult.success());
    final HashSet<RequisitionStatus> requisitionStatusesDisplayWhenCanAuth = newHashSet(
        AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER);

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertTrue(requisitionStatuses.size() == requisitionStatusesDisplayWhenCanAuth.size());
    assertTrue(requisitionStatuses.containsAll(requisitionStatusesDisplayWhenCanAuth));
  }

  @Test
  public void shouldAddRequisitionStatusesDisplayWhenOnlyCanCreate() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_AUTHORIZE));
    when(permissionService.canSubmitRequisition(any())).thenReturn(ValidationResult.success());
    final HashSet<RequisitionStatus> requisitionStatusesDisplayWhenCanCreate = newHashSet(
        SUBMITTED, AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER);

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertTrue(requisitionStatuses.size() == requisitionStatusesDisplayWhenCanCreate.size());
    assertTrue(requisitionStatuses.containsAll(requisitionStatusesDisplayWhenCanCreate));
  }

  @Test
  public void shouldNotAddRequisitionStatusesDisplayWhenCannotCreateAndAuth() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_AUTHORIZE));
    when(permissionService.canSubmitRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_CREATE));

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertTrue(CollectionUtils.isEmpty(requisitionStatuses));
  }

  private Requisition createRequisition() {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    requisition.setRequisitionLineItems(Lists.newArrayList(lineItem));
    return requisition;
  }

  private VersionObjectReferenceDto createVersionObjectReferenceDto(UUID id, Long version) {
    return new VersionObjectReferenceDto(id, null, "orderables", version);
  }

  private RequisitionV2Dto createRequisitionV2Dto() {
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);

    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    ObjectReferenceDto facility = new ObjectReferenceDto(facilityId);
    requisitionV2Dto.setFacility(facility);
    ObjectReferenceDto program = new ObjectReferenceDto(programId);
    requisitionV2Dto.setProgram(program);
    requisitionV2Dto.setAvailableProducts(products);
    requisitionV2Dto.setTemplate(createTemplateDto());
    requisitionV2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    requisitionV2Dto.setId(requisitionId);
    return requisitionV2Dto;
  }

  private BasicRequisitionTemplateDto createTemplateDto() {
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(templateId);
    return templateDto;
  }

  private RequisitionTemplateExtension createTemplate() {
    RequisitionTemplateExtension extension = new RequisitionTemplateExtension();
    extension.setRequisitionTemplateId(templateId);
    extension.setEnableConsultationNumber(true);
    extension.setEnableKitUsage(true);
    extension.setEnablePatientLineItem(true);
    extension.setEnableProduct(true);
    extension.setEnableRapidTestConsumption(true);
    extension.setEnableRegimen(true);
    extension.setEnableUsageInformation(true);
    return extension;
  }

  private ApproveProductsAggregator createApproveProductsAggregator() {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(new Long(1));
    OrderableDto orderable = new OrderableDto();
    orderable.setId(productId2);
    orderable.setMeta(meta);
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(orderable);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    ApproveProductsAggregator aggregator = new ApproveProductsAggregator(list, programId);
    return aggregator;
  }

  private SupervisoryNodeDto createSupervisoryNodeDto() {
    SupervisoryNodeDto nodeDto = new SupervisoryNodeDto();
    nodeDto.setId(supervisoryNodeId);
    return nodeDto;
  }
}
