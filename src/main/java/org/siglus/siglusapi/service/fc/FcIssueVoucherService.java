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

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.FulfillmentProgramReferenceDataService;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.FcHandlerStatus;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcIssueVoucherService {

  private static final String FC_INTEGRATION = "FC Integration";

  @Value("${reasons.receive}")
  private String receiveReason;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Autowired
  private FcValidate fcDataValidate;

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

  @Autowired
  private PodExtensionRepository  podExtensionRepository;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private FulfillmentProgramReferenceDataService programReferenceDataService;

  @Autowired
  private RequisitionService requisitionService;

  public boolean createIssueVouchers(List<IssueVoucherDto> issueVoucherDtos) {
    boolean successHandler = true;
    for (IssueVoucherDto issueVoucherDto : issueVoucherDtos) {
     PodExtension podExtension = podExtensionRepository
         .findByClientCodeAndIssueVoucherNumber(issueVoucherDto.getClientCode(),
          issueVoucherDto.getIssueVoucherNumber());
      if (podExtension != null) {
        FcHandlerStatus handlerError = createIssueVoucher(issueVoucherDto);
        if (handlerError.equals(FcHandlerStatus.CALL_API_ERROR)) {
          successHandler = false;
          break;
        }
      }
    }
    return successHandler;
  }

  private FcHandlerStatus createIssueVoucher(IssueVoucherDto issueVoucherDto) {
    try {
      RequisitionExtension extension =
          getRequisitionExtension(issueVoucherDto.getRequisitionNumber());
      FacilityDto supplyFacility = getWareHouseFacility(issueVoucherDto);
      UserDto userDto = getWareHouseUserInfo(issueVoucherDto, supplyFacility);
      FacilityDto requestFacility =  getClientFacility(issueVoucherDto);
      RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService
          .searchRequisition(extension.getRequisitionId());
      Map<String, ApprovedProductDto> approveProductDtos = getApprovedProductsMap(userDto,
          requisitionV2Dto);
      List<ProductDto> existProducts = getExistProducts(issueVoucherDto, approveProductDtos);
      if (!CollectionUtils.isEmpty(existProducts)) {
        createStockEvent(userDto, requisitionV2Dto, issueVoucherDto, existProducts,
            approveProductDtos);
        createOrder(issueVoucherDto, requisitionV2Dto, existProducts, supplyFacility, userDto,
            approveProductDtos);
        saveFcPodExtension(issueVoucherDto);
      }
    } catch (FcDataException exception) {
      return getFcDataExceptionHandler(issueVoucherDto, exception);
    } catch (Exception exception) {
      return getFcExceptionHandler(issueVoucherDto, exception);
    }
    return FcHandlerStatus.SUCCESS;
  }

  private RequisitionExtension getRequisitionExtension(String requisitionNumber) {
    fcDataValidate.validateEmptyRequisitionNumber(requisitionNumber);
    RequisitionExtension extension = requisitionExtensionRepository
        .findByRequisitionNumber(requisitionNumber);
    fcDataValidate.validateExistRequisitionNumber(extension);
    return extension;
  }

  private FacilityDto getClientFacility(IssueVoucherDto issueVoucherDto) {
    String clientCode = issueVoucherDto.getClientCode();
    fcDataValidate.validateEmptyFacilityCode(clientCode);
    List<org.siglus.common.dto.referencedata.FacilityDto> clientCodeList =
        siglusFacilityReferenceDataService.getFacilityByCode(clientCode).getContent();
    fcDataValidate.validateExistFacility(clientCodeList);
    return clientCodeList.get(0);
  }

  private FacilityDto getWareHouseFacility(IssueVoucherDto issueVoucherDto) {
    String warehouseCode = issueVoucherDto.getWarehouseCode();
    fcDataValidate.validateEmptyFacilityCode(warehouseCode);
    List<org.siglus.common.dto.referencedata.FacilityDto> facilityDtos =
        siglusFacilityReferenceDataService.getFacilityByCode(warehouseCode).getContent();
    fcDataValidate.validateExistFacility(facilityDtos);
    return facilityDtos.get(0);
  }

  private UserDto getWareHouseUserInfo(IssueVoucherDto issueVoucherDto, FacilityDto facilityDto) {
    List<UserDto> userList = userReferenceDataService.getUserInfo(facilityDto.getId())
        .getContent();
    fcDataValidate.validateExistUser(userList);
    return userList.get(0);
  }

  private Map<String, ApprovedProductDto> getApprovedProductsMap(UserDto userDto, RequisitionV2Dto dto) {
    return approvedProductService
        .getApprovedProducts(userDto.getHomeFacilityId(), dto.getProgramId(), null,
            false)
        .stream()
        .collect(Collectors.toMap(product->product.getOrderable().getProductCode(),
            product -> product));
  }

  private List<ProductDto> getExistProducts(IssueVoucherDto issueVoucherDto,
      Map<String, ApprovedProductDto> approvedProducts) {
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
      Map<String, ApprovedProductDto> orderableDtoMap) {
    Collection<ValidSourceDestinationDto> validSourceDtos = sourceDestinationService
        .getValidSources(requisitionV2Dto.getProgramId(), useDto.getHomeFacilityId());
    ValidSourceDestinationDto fcSource = validSourceDtos.stream()
        .filter(validSourceDto -> validSourceDto.getName().equals(FC_INTEGRATION))
        .findFirst()
        .orElse(null);
    fcDataValidate.validateFcSource(fcSource);
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
        .documentNumber(FC_INTEGRATION)
        .facilityId(useDto.getHomeFacilityId())
        .build();

    stockEventsService.createAndFillLotId(eventDto, true);
    stockEventsService.createStockEvent(eventDto);
  }

  private StockEventLineItemDto getStockEventLineItemDto(RequisitionV2Dto requisitionV2Dto,
      IssueVoucherDto issueVoucherDto, Map<String, ApprovedProductDto> orderableDtoMap, UUID sourceId,
      ProductDto productDto) {
    StockEventLineItemDto lineItemDto = new StockEventLineItemDto();
    ApprovedProductDto dto = orderableDtoMap.get(productDto.getFnmCode());
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

  private void createOrder(IssueVoucherDto issueVoucherDto, RequisitionV2Dto v2Dto,
      List<ProductDto> existProductDtos, FacilityDto supplyFacility, UserDto userDto,
      Map<String, ApprovedProductDto> approveProductDtos) {
    Map<String, List<ProductDto>> productMaps =
        existProductDtos.stream()
            .collect(Collectors.groupingBy(productDto -> productDto.getFnmCode()));
    List<OrderExternal> externals = orderExternalRepository.findByRequisitionId(v2Dto.getId());
    Order order = orderRepository.findByExternalId(v2Dto.getId());
    if (CollectionUtils.isEmpty(externals) && order == null) {
      convertOrder(v2Dto, productMaps, supplyFacility, userDto, approveProductDtos);
    } else {
      updateSubOrder(approveProductDtos, productMaps, externals, order);
    }
  }

  private void updateSubOrder(Map<String, ApprovedProductDto> approveProductDtos,
      Map<String, List<ProductDto>> productMaps, List<OrderExternal> externals, Order order) {
    List<UUID> externalIds = externals.stream().map(orderExternal -> orderExternal.getId())
        .collect(Collectors.toList());
    Order canFulfillOrder = orderRepository.findCanFulfillOrderAndInExternalId(externalIds);
    if (canFulfillOrder == null) {
      Order existOrder = orderRepository.findByExternalId(externalIds.get(0));
      SiglusOrderDto orderDto = siglusOrderService.searchOrderById(order.getId());
      OrderObjectReferenceDto dto = new OrderObjectReferenceDto(orderDto.getOrder().getId());
      BeanUtils.copyProperties(existOrder, dto);
      List<OrderLineItem> items = getOrderLineItems(productMaps, approveProductDtos, order);
      order.setOrderLineItems(items);
      siglusOrderService.createSubOrder(dto, getOrderLineItemsDto(items, approveProductDtos));
    } else {
      updateCanFulfillOrder(approveProductDtos, productMaps, order);
    }
  }

  private void updateCanFulfillOrder(Map<String, ApprovedProductDto> approveProductDtos,
      Map<String, List<ProductDto>> productMaps, Order order) {
    List<OrderLineItem> lineItems = order.getOrderLineItems();
    List<OrderLineItem> items = getOrderLineItems(productMaps, approveProductDtos, order);
    List<OrderLineItemDto> orderLineItemDto = getOrderLineItemsDto(items, approveProductDtos);
    orderLineItemDto.forEach(lineItem -> {
      if (getOrderLineItems(lineItems, lineItem.getOrderable().getId()) == null){
        OrderLineItem orderItem = new OrderLineItem();
        ApprovedProductDto approvedProductDto = approveProductDtos.get(
            lineItem.getOrderable().getProductCode());
        VersionEntityReference orderableVersion= new VersionEntityReference(
            approvedProductDto.getId(), approvedProductDto.getVersionNumber());
        orderItem.setOrderable(orderableVersion);
        orderItem.setOrderedQuantity(lineItem.getOrderedQuantity());
        lineItems.add(orderItem);
      }
    });
    order.setOrderLineItems(lineItems);
    orderRepository.save(order);
  }

  private OrderLineItem getOrderLineItems(List<OrderLineItem> lineItems, UUID productId) {
    return lineItems.stream().filter(orderLineItem ->
        orderLineItem.getOrderable().getId().equals(productId))
        .findFirst()
        .orElse(null);
  }

  private void convertOrder(RequisitionV2Dto v2Dto, Map<String, List<ProductDto>> existProductDtos,
      FacilityDto supplyFacility, UserDto userDto,
      Map<String, ApprovedProductDto> approveProductDtos) {
    ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
    releasableRequisitionDto.setRequisitionId(v2Dto.getId());
    releasableRequisitionDto.setSupplyingDepotId(supplyFacility.getId());
    requisitionService.convertToOrder(Arrays.asList(releasableRequisitionDto),
        convertToRequisitionUserDto(userDto));
    Order order = orderRepository.findByExternalId(v2Dto.getId());
    List<OrderLineItem> items = getOrderLineItems(existProductDtos, approveProductDtos, order);
    order.setOrderLineItems(items);
    orderRepository.save(order);
  }

  private List<OrderLineItem> getOrderLineItems(Map<String, List<ProductDto>> existProductDtos,
      Map<String, ApprovedProductDto> approveProductDtos, Order order) {
   return existProductDtos.entrySet().stream().map(entry -> {
      OrderLineItem orderLineItem = new OrderLineItem();
      ApprovedProductDto approvedProductDto = approveProductDtos.get(entry.getKey());
      VersionEntityReference orderableVersion= new VersionEntityReference(
          approvedProductDto.getId(), approvedProductDto.getVersionNumber());
      orderLineItem.setOrderable(orderableVersion);
      orderLineItem.setOrderedQuantity(approvedProductDto.getOrderable()
          .packsToOrder(getApprovedQuality(entry.getValue())));
      orderLineItem.setOrder(order);
      return orderLineItem;
    }).collect(Collectors.toList());
  }

  private Long getApprovedQuality(List<ProductDto> productDtos) {
    return Long.valueOf(productDtos.stream().mapToInt(productDto->
        productDto.getApprovedQuantity() == null ? 0 : productDto.getApprovedQuantity()).sum());
  }

  private List<OrderLineItemDto> getOrderLineItemsDto(List<OrderLineItem> orderLineItems,
      Map<String, ApprovedProductDto> approveProductDtos) {
   return orderLineItems.stream().map(orderLineItem -> {
      OrderLineItemDto lineItemDto = new OrderLineItemDto();
      ApprovedProductDto approvedProductDto = approveProductDtos
          .get(lineItemDto.getOrderable().getProductCode());
      org.openlmis.requisition.dto.OrderableDto existOrderableDto = approvedProductDto.getOrderable();
      org.openlmis.fulfillment.service.referencedata.OrderableDto orderableDto = new
          org.openlmis.fulfillment.service.referencedata.OrderableDto();
      BeanUtils.copyProperties(existOrderableDto, orderableDto);
      lineItemDto.setOrderable(orderableDto);
      lineItemDto.setOrderedQuantity(orderLineItem.getOrderedQuantity());
      return lineItemDto;
    }).collect(Collectors.toList());
  }

  private org.openlmis.requisition.dto.UserDto convertToRequisitionUserDto(UserDto dto) {
    org.openlmis.requisition.dto.UserDto userDto = new org.openlmis.requisition.dto.UserDto();
    BeanUtils.copyProperties(dto, userDto);
    return userDto;
  }

  private void saveFcPodExtension(IssueVoucherDto dto) {
    PodExtension podExtension = new PodExtension();
    podExtension.setClientCode(dto.getClientCode());
    podExtension.setIssueVoucherNumber(dto.getIssueVoucherNumber());
    podExtensionRepository.save(podExtension);
  }

  private FcHandlerStatus getFcExceptionHandler(
      IssueVoucherDto issueVoucherDto, Exception exception) {
    log.error("[FC] FcIntegrationError: Issue vourch - {} exception -",
        issueVoucherDto.toString(), exception);
    return FcHandlerStatus.CALL_API_ERROR;
  }

  private FcHandlerStatus getFcDataExceptionHandler(
      IssueVoucherDto issueVoucherDto, FcDataException dataException) {
    log.error("[FC] FcIntegrationError: Issue vourch - {} exception - {}", issueVoucherDto,
        dataException.getMessage());
    return FcHandlerStatus.DATA_ERROR;
  }

}
