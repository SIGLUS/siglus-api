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

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.FcIntegrationHandlerStatus;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.validator.FcIntegrationDataValidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusFcIntegrationIssueVoucherService {

  private static final String FC_INTEGRATION = "FC Integration";

  @Value("${reasons.receive}")
  private String receiveReason;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Autowired
  private FcIntegrationDataValidate dataValidate;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Autowired
  private SiglusUserReferenceDataService userReferenceDataService;

  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Autowired
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Autowired
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Autowired
  private SiglusStockEventsService stockEventsService;

  @Autowired
  private ValidSourceDestinationStockManagementService sourceDestinationService;

  public FcIntegrationHandlerStatus createIssueVoucher(IssueVoucherDto issueVoucherDto) {
    try {
      RequisitionExtension extension =
          getRequisitionExtension(issueVoucherDto.getRequisitionNumber());
      UserDto userDto = wareHouseInfo(issueVoucherDto);
      getClientFacility(issueVoucherDto);
      RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService
          .searchRequisition(extension.getRequisitionId());
      Map<String, OrderableDto> approveProductDtos = getApprovedProductsMap(userDto,
          requisitionV2Dto);
      List<ProductDto> productDtos = getExistProducts(issueVoucherDto,
          approveProductDtos);
      if (!CollectionUtils.isEmpty(productDtos)) {
        createStockEvent(userDto, requisitionV2Dto, issueVoucherDto, productDtos,
            approveProductDtos);
      }
    } catch (ValidationMessageException messageException) {
      return getFcIntegrationHandlerStatusForMessageException(issueVoucherDto, messageException);
    } catch (Exception exception) {
      return getFcIntegrationHandlerStatusForRegularException(issueVoucherDto, exception);
    }
    return FcIntegrationHandlerStatus.SUCCESS;
  }

  private RequisitionExtension getRequisitionExtension(String requisitionNumber) {
    dataValidate.validateEmptyRequisitionNumber(requisitionNumber);
    RequisitionExtension extension = requisitionExtensionRepository
        .findByRequisitionNumber(requisitionNumber);
    dataValidate.validateExistRequisitionNumber(extension);
    return extension;
  }

  private FacilityDto getClientFacility(IssueVoucherDto issueVoucherDto) {
    String clientCode = issueVoucherDto.getClientCode();
    dataValidate.validateEmptyFacilityCode(clientCode);
    List<FacilityDto> clientCodeList = siglusFacilityReferenceDataService
        .getFacilityByCode(clientCode).getContent();
    dataValidate.validateExistFacility(clientCodeList);
    return clientCodeList.get(0);
  }

  private UserDto wareHouseInfo(IssueVoucherDto issueVoucherDto) {
    String warehouseCode = issueVoucherDto.getWarehouseCode();
    dataValidate.validateEmptyFacilityCode(warehouseCode);
    List<FacilityDto> facilityDtos = siglusFacilityReferenceDataService
        .getFacilityByCode(warehouseCode).getContent();
    dataValidate.validateExistFacility(facilityDtos);
    FacilityDto facilityDto = facilityDtos.get(0);
    List<UserDto> userList = userReferenceDataService.getUserInfo(facilityDto.getId())
        .getContent();
    dataValidate.validateExistUser(userList);
    return userList.get(0);
  }

  private FcIntegrationHandlerStatus getFcIntegrationHandlerStatusForRegularException(
      IssueVoucherDto issueVoucherDto, Exception exception) {
    log.error("[FC] FcIntegrationError: Issue vourch - {} exception -",
        issueVoucherDto.toString(), exception);
    return FcIntegrationHandlerStatus.CALLAPIERROR;
  }

  private FcIntegrationHandlerStatus getFcIntegrationHandlerStatusForMessageException(
      IssueVoucherDto issueVoucherDto, ValidationMessageException messageException) {
    log.error("[FC] FcIntegrationError: Issue vourch - {} exception - {}", issueVoucherDto,
        messageException.getMessage());
    if (messageException.getMessage().contains(FcIntegrationDataValidate.DATA_ERROR)) {
      return FcIntegrationHandlerStatus.DATAERROR;
    }
    return FcIntegrationHandlerStatus.CALLAPIERROR;
  }

  private Map<String, OrderableDto> getApprovedProductsMap(UserDto userDto, RequisitionV2Dto dto) {
    return approvedProductService
        .getApprovedProducts(userDto.getHomeFacilityId(), dto.getProgramId(), null)
        .getOrderablesPage()
        .getContent()
        .stream()
        .collect(Collectors.toMap(OrderableDto::getProductCode, orderableDto -> orderableDto));
  }

  private List<ProductDto> getExistProducts(IssueVoucherDto issueVoucherDto,
      Map<String, OrderableDto> approvedProducts) {
    List<ProductDto> issueProducts = issueVoucherDto.getProducts();
    List<ProductDto> findProducts = issueProducts.stream()
        .filter(productDto -> approvedProducts.containsKey(productDto.getFnmCode()))
        .collect(Collectors.toList());
    if (findProducts.size() != issueVoucherDto.getProducts().size()) {
      List<ProductDto> notFindProducts = issueProducts.stream()
          .filter(productDto -> !approvedProducts.containsKey(productDto.getFnmCode()))
          .collect(Collectors.toList());
      log.error("[FC] FcIntegrationError: not found products - {} ", notFindProducts.toString());
    }
    return findProducts;
  }

  private void createStockEvent(UserDto useDto, RequisitionV2Dto requisitionV2Dto,
      IssueVoucherDto issueVoucherDto, List<ProductDto> existProductDtos,
      Map<String, OrderableDto> orderableDtoMap) {
    Collection<ValidSourceDestinationDto> validSourceDtos = sourceDestinationService
        .getValidSources(requisitionV2Dto.getProgramId(), useDto.getHomeFacilityId());
    ValidSourceDestinationDto fcSource = validSourceDtos.stream()
        .filter(validSourceDto -> validSourceDto.getName().equals(FC_INTEGRATION))
        .findFirst()
        .orElse(null);
    dataValidate.validateFcSource(fcSource);
    UUID sourceId = fcSource.getNode().getId();

    List<StockEventLineItemDto> eventLineItemDtos = existProductDtos.stream()
        .map(productDto -> getStockEventLineItemDto(requisitionV2Dto,
            issueVoucherDto, orderableDtoMap, sourceId, productDto)).collect(Collectors.toList());
    StockEventDto eventDto = StockEventDto.builder()
        .programId(requisitionV2Dto.getProgramId())
        .facilityId(useDto.getHomeFacilityId())
        .signature(FC_INTEGRATION)
        .userId(useDto.getId())
        .lineItems(eventLineItemDtos)
        .build();

    eventDto.setDocumentNumber(FC_INTEGRATION);
    eventDto.setFacilityId(useDto.getHomeFacilityId());
    stockEventsService.createAndFillLotId(eventDto, true);
    stockEventsService.createStockEvent(eventDto);
  }

  private StockEventLineItemDto getStockEventLineItemDto(RequisitionV2Dto requisitionV2Dto,
      IssueVoucherDto issueVoucherDto, Map<String, OrderableDto> orderableDtoMap, UUID sourceId,
      ProductDto productDto) {
    StockEventLineItemDto lineItemDto = new StockEventLineItemDto();
    OrderableDto dto = orderableDtoMap.get(productDto.getFnmCode());
    lineItemDto.setOrderableId(dto.getId());
    lineItemDto.setQuantity(productDto.getShippedQuantity());
    LocalDate date = issueVoucherDto.getShippingDate().toInstant()
        .atZone(ZoneId.of(timeZoneId)).toLocalDate();
    lineItemDto.setOccurredDate(date);
    lineItemDto.setReasonId(UUID.fromString(receiveReason));
    lineItemDto.setDocumentationNo(FC_INTEGRATION);
    lineItemDto.setProgramId(requisitionV2Dto.getProgramId());
    lineItemDto.setSourceId(sourceId);
    lineItemDto.setExtraData(ImmutableMap.of("lotCode", productDto.getBatch(),
        "expirationDate", SiglusDateHelper.formatDate(productDto.getExpiryDate())));
    return lineItemDto;
  }

}
