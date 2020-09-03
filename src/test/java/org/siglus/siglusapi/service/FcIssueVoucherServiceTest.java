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

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.utils.Pagination;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.domain.FcIntegrationHandlerStatus;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.service.fc.FcIssueVoucherService;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
public class SiglusFcIntegrationIssueVoucherServiceTest {

  @InjectMocks
  private FcIssueVoucherService service;

  @Mock
  private FcValidate dataValidate;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private SiglusUserReferenceDataService userReferenceDataService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusStockEventsService stockEventsService;

  @Mock
  private ValidSourceDestinationStockManagementService sourceDestinationService;

  @Before
  public void prepare() {
    dataValidate = new FcValidate();
  }

  @Test
  public void ShouldReturnSuccess() {
    IssueVoucherDto issueVoucherDto = getIssueVoucherDto();
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionId(UUID.randomUUID());
    when(requisitionExtensionRepository
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber()))
        .thenReturn(requisitionExtension);
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(UUID.randomUUID());
    when(siglusFacilityReferenceDataService
        .getFacilityByCode(issueVoucherDto.getWarehouseCode()))
        .thenReturn(Pagination
            .getPage(Arrays.asList(facilityDto), new PageRequest(0, 10) ,0));
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setHomeFacilityId(facilityDto.getId());
    when(userReferenceDataService
        .getUserInfo(facilityDto.getId()))
        .thenReturn(Pagination
            .getPage(Arrays.asList(userDto), new PageRequest(0, 10) ,0));
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    ObjectReferenceDto program = new ObjectReferenceDto();
    program.setId(randomUUID());
    requisitionV2Dto.setProgram(program);
    requisitionV2Dto.setId(requisitionExtension.getRequisitionId());
    when(siglusRequisitionRequisitionService
        .searchRequisition(requisitionExtension.getRequisitionId()))
        .thenReturn(requisitionV2Dto);
    when(approvedProductService.getApprovedProducts(userDto.getHomeFacilityId(),
        requisitionV2Dto.getProgramId(), null, false));
    assertEquals(FcIntegrationHandlerStatus.SUCCESS , service.createIssueVoucher(issueVoucherDto));
  }

  private IssueVoucherDto getIssueVoucherDto() {
    IssueVoucherDto issueVoucherDto = new IssueVoucherDto();
    issueVoucherDto.setWarehouseCode("04030101");
    issueVoucherDto.setClientCode("04030101");
    issueVoucherDto.setRequisitionNumber("RNR-NO010906120000192");
    issueVoucherDto.setShippingDate(new Date());
    ProductDto productDto = new ProductDto();
    productDto.setApprovedQuantity(100);
    productDto.setShippedQuantity(50);
    productDto.setBatch("M18361");
    productDto.setFnmCode("02E01");
    productDto.setExpiryDate(new Date());
    issueVoucherDto.setProducts(Arrays.asList(productDto));
    return issueVoucherDto;
  }
}
