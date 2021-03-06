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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.ORDERABLE_ID;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.MetadataDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionStatusProcessor;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.web.OrderDtoBuilder;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.LotSearchParams;
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
import org.siglus.siglusapi.service.SiglusShipmentDraftService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
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
  private PodExtensionRepository podExtensionRepository;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private SiglusShipmentDraftService shipmentDraftService;

  @Autowired
  private SiglusStockCardSummariesService stockCardSummariesService;

  @Autowired
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Autowired
  private SiglusShipmentService siglusShipmentService;

  @Autowired
  private SiglusSimulateUserAuthHelper simulateUser;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private RequisitionStatusProcessor requisitionStatusProcessor;

  @Autowired
  private OrderDtoBuilder orderDtoBuilder;

  @Autowired
  private OrderFulfillmentService orderFulfillmentService;

  private final List<String> statusErrorIssueVoucherNumber = new ArrayList<>();

  public boolean processIssueVouchers(List<IssueVoucherDto> issueVoucherDtos) {
    if (isEmpty(issueVoucherDtos)) {
      return false;
    }
    boolean successHandler = true;
    statusErrorIssueVoucherNumber.clear();
    for (IssueVoucherDto issueVoucherDto : issueVoucherDtos) {
      PodExtension podExtension = podExtensionRepository
          .findByClientCodeAndIssueVoucherNumber(issueVoucherDto.getClientCode(),
              issueVoucherDto.getIssueVoucherNumber());
      if (podExtension == null) {
        FcHandlerStatus handlerError = createIssueVoucher(issueVoucherDto);
        if (handlerError.equals(FcHandlerStatus.CALL_API_ERROR)) {
          successHandler = false;
        }
      }
    }
    if (!CollectionUtils.isEmpty(statusErrorIssueVoucherNumber)) {
      successHandler = false;
    }
    return successHandler;
  }

  public List<String> getStatusErrorIssueVoucherNumber() {
    return this.statusErrorIssueVoucherNumber;
  }

  private FcHandlerStatus createIssueVoucher(IssueVoucherDto issueVoucherDto) {
    try {
      RequisitionExtension extension =
          getRequisitionExtension(issueVoucherDto.getRequisitionNumber());
      FacilityDto supplyFacility = getWareHouseFacility(issueVoucherDto);
      UserDto userDto = getWareHouseUserInfo(supplyFacility);
      getClientFacility(issueVoucherDto);
      RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService
          .searchRequisition(extension.getRequisitionId());
      if (!requisitionV2Dto.getStatus().isApproved()) {
        statusErrorIssueVoucherNumber.add("requisition status is error :"
            + issueVoucherDto.getIssueVoucherNumber());
        return FcHandlerStatus.DATA_ERROR;
      }
      List<ApprovedProductDto> approvedProductDtos = getApprovedProducts(userDto, requisitionV2Dto);
      Map<String, ApprovedProductDto> approvedProductsMap =
          getApprovedProductsMap(approvedProductDtos);
      List<ProductDto> existProducts = getExistProducts(issueVoucherDto, approvedProductsMap);
      if (!CollectionUtils.isEmpty(existProducts)) {
        simulateUser.simulateUserAuth(userDto.getId());
        createStockEvent(userDto, requisitionV2Dto, existProducts, approvedProductsMap);
        Map<String, List<ProductDto>> productMaps = existProducts.stream()
            .collect(Collectors.groupingBy(ProductDto::getFnmCode));
        UUID orderId = createOrder(requisitionV2Dto, approvedProductDtos, productMaps,
            supplyFacility, userDto, approvedProductsMap, issueVoucherDto);
        if (orderId != null) {
          ShipmentDto shipmentDto = createShipmentDraftAndShipment(orderId, productMaps,
              approvedProductDtos, approvedProductsMap, supplyFacility, requisitionV2Dto,
              issueVoucherDto);
          saveFcPodExtension(issueVoucherDto, shipmentDto);
        }
      } else {
        statusErrorIssueVoucherNumber.add("product not exist error :"
            + issueVoucherDto.getIssueVoucherNumber());
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

  private UserDto getWareHouseUserInfo(FacilityDto facilityDto) {
    List<UserDto> userList = userReferenceDataService.getUserInfo(facilityDto.getId())
        .getContent();
    fcDataValidate.validateExistUser(userList);
    return userList.get(0);
  }

  private Map<String, ApprovedProductDto> getApprovedProductsMap(List<ApprovedProductDto>
      approvedProductDtos) {
    return approvedProductDtos.stream()
        .collect(Collectors.toMap(product -> product.getOrderable().getProductCode(),
            Function.identity()));
  }

  private List<ApprovedProductDto> getApprovedProducts(UserDto userDto, RequisitionV2Dto dto) {
    return approvedProductService
        .getApprovedProducts(userDto.getHomeFacilityId(), dto.getProgramId(), null,
            false);
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
      log.error("[FC] not found products: {} ", notFindProducts);
    }
    return findProducts;
  }

  private void createStockEvent(UserDto useDto, RequisitionV2Dto requisitionV2Dto,
      List<ProductDto> existProductDtos, Map<String, ApprovedProductDto> orderableDtoMap) {
    Collection<ValidSourceDestinationDto> validSourceDtos = sourceDestinationService
        .getValidSources(requisitionV2Dto.getProgramId(), useDto.getHomeFacilityId());
    ValidSourceDestinationDto fcSource = validSourceDtos.stream()
        .filter(validSourceDto -> validSourceDto.getName().equals(FC_INTEGRATION))
        .findFirst()
        .orElse(null);
    fcDataValidate.validateFcSource(fcSource);
    if (fcSource != null) {
      UUID sourceId = fcSource.getNode().getId();
      List<StockEventLineItemDto> eventLineItemDtos = existProductDtos.stream()
          .map(productDto -> getStockEventLineItemDto(requisitionV2Dto,
              orderableDtoMap, sourceId, productDto)).collect(Collectors.toList());
      StockEventDto eventDto = StockEventDto.builder()
          .programId(requisitionV2Dto.getProgramId())
          .facilityId(useDto.getHomeFacilityId())
          .signature(FC_INTEGRATION)
          .userId(useDto.getId())
          .lineItems(eventLineItemDtos)
          .documentNumber(FC_INTEGRATION)
          .facilityId(useDto.getHomeFacilityId())
          .build();
      stockEventsService.createStockEventForOneProgram(eventDto, useDto.getId());
    }
  }

  private StockEventLineItemDto getStockEventLineItemDto(RequisitionV2Dto requisitionV2Dto,
      Map<String, ApprovedProductDto> orderableDtoMap, UUID sourceId, ProductDto productDto) {
    StockEventLineItemDto lineItemDto = new StockEventLineItemDto();
    ApprovedProductDto dto = orderableDtoMap.get(productDto.getFnmCode());
    lineItemDto.setOrderableId(dto.getOrderable().getId());
    lineItemDto.setQuantity(getReceiveQuantity(dto, productDto.getShippedQuantity()));
    LocalDate date = LocalDate.now().minusDays(1);
    lineItemDto.setOccurredDate(date);
    lineItemDto.setReasonId(UUID.fromString(receiveReason));
    lineItemDto.setDocumentationNo(FC_INTEGRATION);
    lineItemDto.setProgramId(requisitionV2Dto.getProgramId());
    lineItemDto.setSourceId(sourceId);
    lineItemDto.setExtraData(ImmutableMap.of("lotCode", productDto.getBatch(),
        "expirationDate", SiglusDateHelper.formatDate(productDto.getExpiryDate())));
    return lineItemDto;
  }

  private Integer getReceiveQuantity(ApprovedProductDto dto, Integer shipQuantity) {
    Long quantity = dto.getOrderable().packsToOrder(shipQuantity)
        * dto.getOrderable().getNetContent();
    return quantity.intValue();
  }

  private UUID createOrder(RequisitionV2Dto v2Dto, List<ApprovedProductDto> approvedProductDtos,
      Map<String, List<ProductDto>> productMaps, FacilityDto supplyFacility, UserDto userDto,
      Map<String, ApprovedProductDto> approvedProductMap, IssueVoucherDto issueVoucherDto) {
    List<OrderExternal> externals = orderExternalRepository.findByRequisitionId(v2Dto.getId());
    Order firstOrder = orderRepository.findByExternalId(v2Dto.getId());
    if (CollectionUtils.isEmpty(externals) && firstOrder == null) {
      return convertOrder(v2Dto, productMaps, supplyFacility, userDto, approvedProductDtos,
          approvedProductMap, issueVoucherDto);
    } else {
      return updateSubOrder(firstOrder, approvedProductMap, supplyFacility, productMaps, externals,
          issueVoucherDto);
    }
  }

  private UUID updateSubOrder(Order firstOrder,
      Map<String, ApprovedProductDto> approveProductDtos,
      FacilityDto supplyFacility,
      Map<String, List<ProductDto>> productMaps, List<OrderExternal> externals,
      IssueVoucherDto issueVoucherDto) {
    List<UUID> externalIds = externals.stream().map(OrderExternal::getId)
        .collect(Collectors.toList());
    Order canFulfillOrder = CollectionUtils.isEmpty(externalIds) ? null :
        orderRepository.findCanFulfillOrderByExternalIdIn(externalIds);
    if (canFulfillOrder == null) {
      Order existOrder = firstOrder != null ? firstOrder :
          orderRepository.findLastOrderByExternalIds(externalIds);
      OrderDto orderDto = siglusOrderService
          .searchOrderByIdForMultiWareHouseSupply(existOrder.getId()).getOrder();
      org.openlmis.fulfillment.service.referencedata.FacilityDto fulfillFacilityDto =
          new org.openlmis.fulfillment.service.referencedata.FacilityDto();
      BeanUtils.copyProperties(supplyFacility, fulfillFacilityDto);
      orderDto.setSupplyingFacility(fulfillFacilityDto);
      OrderObjectReferenceDto dto = new OrderObjectReferenceDto(orderDto.getId());
      BeanUtils.copyProperties(orderDto, dto);
      orderDto.setCreatedDate(issueVoucherDto.getShippingDate());
      Iterable<BasicOrderDto> orderDtos = siglusOrderService
          .createSubOrder(dto, getOrderLineItemsDtoInIssue(productMaps, approveProductDtos));
      if (orderDtos.iterator().hasNext()) {
        return orderDtos.iterator().next().getId();
      }
      return null;
    } else {
      return updateCanFulfillOrder(approveProductDtos, productMaps, canFulfillOrder,
          issueVoucherDto, supplyFacility);
    }
  }

  private UUID updateCanFulfillOrder(Map<String, ApprovedProductDto> approveProductDtos,
      Map<String, List<ProductDto>> productMaps, Order canFulfillOrder,
      IssueVoucherDto issueVoucherDto, FacilityDto supplyFacility) {
    List<OrderLineItem> existLineItems = canFulfillOrder.getOrderLineItems();
    List<OrderLineItemDto> orderLineItemDto =
        getOrderLineItemsDtoInIssue(productMaps, approveProductDtos);
    orderLineItemDto.forEach(lineItem -> {
      if (getOrderLineItems(existLineItems, lineItem.getOrderable().getId()) == null) {
        OrderLineItem orderItem = new OrderLineItem();
        ApprovedProductDto approvedProductDto = approveProductDtos.get(
            lineItem.getOrderable().getProductCode());
        VersionEntityReference orderableVersion = new VersionEntityReference(
            approvedProductDto.getOrderable().getId(),
            approvedProductDto.getOrderable().getVersionNumber());
        orderItem.setOrderable(orderableVersion);
        orderItem.setOrder(canFulfillOrder);
        orderItem.setOrderedQuantity(lineItem.getOrderedQuantity());
        existLineItems.add(orderItem);
      }
    });
    canFulfillOrder.setSupplyingFacilityId(supplyFacility.getId());
    canFulfillOrder.setOrderLineItems(existLineItems);
    canFulfillOrder.setCreatedDate(issueVoucherDto.getShippingDate());
    log.info("[FC] save fc order: {}", canFulfillOrder);
    orderRepository.save(canFulfillOrder);
    return canFulfillOrder.getId();
  }

  private UUID convertOrder(RequisitionV2Dto v2Dto, Map<String, List<ProductDto>> existProductDtos,
      FacilityDto supplyFacility, UserDto userDto, List<ApprovedProductDto> approvedProductDtos,
      Map<String, ApprovedProductDto> approveProductMap, IssueVoucherDto issueVoucherDto) {
    convertRequisitionToOrder(v2Dto, supplyFacility, convertToRequisitionUserDto(userDto),
        existProductDtos, approvedProductDtos, approveProductMap, issueVoucherDto);
    Order order = orderRepository.findByExternalId(v2Dto.getId());
    return order.getId();
  }

  private OrderLineItem getOrderLineItems(List<OrderLineItem> lineItems, UUID productId) {
    return lineItems.stream().filter(orderLineItem ->
        orderLineItem.getOrderable().getId().equals(productId))
        .findFirst()
        .orElse(null);
  }

  private List<OrderLineItem> getOrderLineItems(Map<String, List<ProductDto>> existProductDtos,
      Map<String, ApprovedProductDto> approveProductDtos, Order order) {
    return existProductDtos.entrySet().stream().map(entry -> {
      OrderLineItem orderLineItem = new OrderLineItem();
      ApprovedProductDto approvedProductDto = approveProductDtos.get(entry.getKey());
      VersionEntityReference orderableVersion = new VersionEntityReference(
          approvedProductDto.getOrderable().getId(),
          approvedProductDto.getOrderable().getVersionNumber());
      orderLineItem.setOrderable(orderableVersion);
      orderLineItem.setOrderedQuantity(approvedProductDto.getOrderable()
          .packsToOrder(getApprovedQuality(entry.getValue())));
      orderLineItem.setOrder(order);
      return orderLineItem;
    }).collect(Collectors.toList());
  }

  private List<OrderLineItemDto> getOrderLineItemsDtoInIssue(Map<String, List<ProductDto>>
      existProductDtos, Map<String, ApprovedProductDto> approveProductDtos) {
    return existProductDtos.entrySet().stream().map(entry -> {
      ApprovedProductDto approvedProductDto = approveProductDtos.get(entry.getKey());
      org.openlmis.requisition.dto.OrderableDto existOrderableDto = approvedProductDto
          .getOrderable();
      org.openlmis.fulfillment.service.referencedata.OrderableDto orderableDto = new
          org.openlmis.fulfillment.service.referencedata.OrderableDto();
      BeanUtils.copyProperties(existOrderableDto, orderableDto);
      MetadataDto metadataDto = new MetadataDto();
      BeanUtils.copyProperties(existOrderableDto.getMeta(), metadataDto);
      orderableDto.setMeta(metadataDto);
      OrderLineItemDto itemDto = new OrderLineItemDto();
      itemDto.setOrderable(orderableDto);
      itemDto.setPartialFulfilledQuantity((long) 0);
      itemDto.setOrderedQuantity(approvedProductDto.getOrderable()
          .packsToOrder(getApprovedQuality(entry.getValue())));
      return itemDto;
    }).collect(Collectors.toList());
  }


  private Long getApprovedQuality(List<ProductDto> productDtos) {
    return (long) productDtos.stream().mapToInt(productDto ->
        productDto.getApprovedQuantity() == null ? 0 : productDto.getApprovedQuantity()).sum();
  }

  private org.openlmis.requisition.dto.UserDto convertToRequisitionUserDto(UserDto dto) {
    org.openlmis.requisition.dto.UserDto userDto = new org.openlmis.requisition.dto.UserDto();
    BeanUtils.copyProperties(dto, userDto);
    return userDto;
  }

  private void saveFcPodExtension(IssueVoucherDto dto, ShipmentDto shipmentDto) {
    PodExtension podExtension = new PodExtension();
    podExtension.setClientCode(dto.getClientCode());
    podExtension.setIssueVoucherNumber(dto.getIssueVoucherNumber());
    podExtension.setShipmentId(shipmentDto.getId());
    log.info("save fc pod extension: {}", podExtension);
    podExtensionRepository.save(podExtension);
  }

  private FcHandlerStatus getFcExceptionHandler(
      IssueVoucherDto issueVoucherDto, Exception exception) {
    log.error("[FC] issue voucher: {}, exception: {}", issueVoucherDto, exception);
    return FcHandlerStatus.CALL_API_ERROR;
  }

  private ShipmentDto createShipmentDraftAndShipment(UUID orderId,
      Map<String, List<ProductDto>> productMaps,
      List<ApprovedProductDto> approvedProductDtos,
      Map<String, ApprovedProductDto> approvedProductDtoMaps,
      FacilityDto supplyFacility, RequisitionV2Dto requisitionV2Dto,
      IssueVoucherDto issueVoucherDto) {
    SiglusOrderDto orderDto = siglusOrderService
        .searchOrderByIdForMultiWareHouseSupply(orderId);
    ShipmentDto shipmentDto = new ShipmentDto();
    OrderObjectReferenceDto orderReferenceDto = new OrderObjectReferenceDto(orderId);
    BeanUtils.copyProperties(orderDto.getOrder(), orderReferenceDto);
    orderReferenceDto.setOrderLineItems(orderDto.getOrder().orderLineItems());
    shipmentDto.setOrder(orderReferenceDto);
    List<UUID> productIds = productMaps.keySet().stream()
        .map(key -> approvedProductDtoMaps.get(key).getOrderable().getId())
        .collect(Collectors.toList());
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos =
        searchStockCardSummaries(supplyFacility, requisitionV2Dto, productIds);
    Map<UUID, ApprovedProductDto> approvedProductIdMaps = approvedProductDtos.stream()
        .collect(Collectors.toMap(product -> product.getOrderable().getId(), Function.identity()));
    ShipmentDraftDto draftDto =
        createShipmentDraft(orderDto, stockCardSummaryV2Dtos, approvedProductIdMaps);
    shipmentDto.setId(draftDto.getId());
    shipmentDto.setShippedDate(issueVoucherDto.getShippingDate());
    shipmentDto.setLineItems(getShipmentLineItems(draftDto.lineItems(), issueVoucherDto,
        approvedProductIdMaps));
    return siglusShipmentService.createSubOrderAndShipment(shipmentDto);
  }

  private ShipmentDraftDto createShipmentDraft(SiglusOrderDto orderDto,
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Map<UUID, ApprovedProductDto> approvedProductIdMaps) {
    List<ShipmentLineItemDto> shipmentLineItemDtos =
        getShipmentDraftLineItems(stockCardSummaryV2Dtos, approvedProductIdMaps);
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(
        orderDto.getOrder().getId());
    BeanUtils.copyProperties(orderDto, orderObjectReferenceDto);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(orderObjectReferenceDto);
    draftDto.setLineItems(shipmentLineItemDtos);
    return shipmentDraftService.createShipmentDraft(draftDto);
  }

  private List<ShipmentLineItemDto> getShipmentDraftLineItems(
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Map<UUID, ApprovedProductDto> approvedProductIdMaps) {
    List<ShipmentLineItemDto> shipmentLineItemDtos = new ArrayList<>();
    stockCardSummaryV2Dtos.forEach(stockCardSummaryV2Dto -> {
      OrderableDto orderableDto = approvedProductIdMaps
          .get(stockCardSummaryV2Dto.getOrderable().getId()).getOrderable();
      long netContent = orderableDto.getNetContent();
      stockCardSummaryV2Dto.getCanFulfillForMe().forEach(fulfillForMeDto -> {
        if (fulfillForMeDto.getStockOnHand() > netContent) {
          ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
          VersionObjectReferenceDto orderableReferenceDto =
              new VersionObjectReferenceDto(orderableDto.getId(),
                  null, "orderables", orderableDto.getVersionNumber());
          lineItemDto.setOrderable(orderableReferenceDto);
          lineItemDto.setQuantityShipped((long) 0);
          lineItemDto.setLotId(fulfillForMeDto.getLot().getId());
          shipmentLineItemDtos.add(lineItemDto);
        }
      });
    });
    return shipmentLineItemDtos;
  }

  private List<ShipmentLineItemDto> getShipmentLineItems(List<ShipmentLineItemDto> draftLineItems,
      IssueVoucherDto issueVoucherDto, Map<UUID, ApprovedProductDto> approvedProductIdMaps) {
    List<UUID> lotIds = draftLineItems.stream()
        .map(lineItemDto -> lineItemDto.getLot().getId()).collect(Collectors.toList());
    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setId(lotIds);
    Map<UUID, List<LotDto>> lotDtos = lotReferenceDataService.getLots(lotSearchParams).stream()
        .collect(Collectors.groupingBy(LotDto::getId));
    draftLineItems.forEach(lineItem -> {
      UUID orderableId = lineItem.getOrderable().getId();
      ApprovedProductDto approvedProductDto = approvedProductIdMaps.get(orderableId);
      LotDto lotDto = lotDtos.get(lineItem.getLot().getId()).get(0);
      ProductDto product = getProductDto(approvedProductDto, lotDto, issueVoucherDto.getProducts());
      lineItem.setQuantityShipped(product == null ? 0 :
          approvedProductDto.getOrderable().packsToOrder(product.getShippedQuantity()));
    });
    return draftLineItems;
  }

  private ProductDto getProductDto(ApprovedProductDto productDto, LotDto lotDto,
      List<ProductDto> products) {
    return products.stream().filter(dto ->
        dto.getFnmCode().equals(productDto.getOrderable().getProductCode())
            && dto.getBatch().equals(lotDto.getLotCode()))
        .findFirst()
        .orElse(null);
  }

  private List<StockCardSummaryV2Dto> searchStockCardSummaries(FacilityDto supplyFacility,
      RequisitionV2Dto requisitionV2Dto, List<UUID> productIds) {
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set(PROGRAM_ID, requisitionV2Dto.getProgram().getId().toString());
    parameters.set(FACILITY_ID, supplyFacility.getId().toString());
    parameters.set(RIGHT_NAME, STOCK_CARDS_VIEW);
    productIds.forEach(productId -> parameters.add(ORDERABLE_ID, productId.toString()));
    Pageable page = new PageRequest(DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE);
    return stockCardSummariesService.findSiglusStockCard(parameters, page)
        .getContent();
  }

  private FcHandlerStatus getFcDataExceptionHandler(
      IssueVoucherDto issueVoucherDto, FcDataException dataException) {
    log.error("[FC] issue voucher: {}, exception: {}", issueVoucherDto, dataException.getMessage());
    return FcHandlerStatus.DATA_ERROR;
  }

  private void convertRequisitionToOrder(RequisitionV2Dto v2Dto, FacilityDto supplyFacility,
      org.openlmis.requisition.dto.UserDto user, Map<String, List<ProductDto>> existProductDtos,
      List<ApprovedProductDto> approvedProductDtos,
      Map<String, ApprovedProductDto> approveProductMap, IssueVoucherDto issueVoucherDto) {
    Requisition loadedRequisition = requisitionRepository.findOne(v2Dto.getId());
    loadedRequisition.release(supplyFacility.getId());
    loadedRequisition.setSupplyingFacilityId(supplyFacility.getId());
    log.info("[FC] save requisition: {}", loadedRequisition);
    requisitionRepository.save(loadedRequisition);
    requisitionStatusProcessor.statusChange(loadedRequisition, LocaleContextHolder.getLocale());
    org.openlmis.requisition.dto.OrderDto order = orderDtoBuilder.build(loadedRequisition, user);
    List<OrderLineItem> items = getOrderLineItems(existProductDtos, approveProductMap,
        new Order());
    order.setOrderLineItems(convertToOrderLineItemDto(approvedProductDtos, items));
    order.setCreatedDate(issueVoucherDto.getShippingDate());
    orderFulfillmentService.create(Arrays.asList(order));
  }

  private List<org.openlmis.requisition.dto.OrderLineItemDto> convertToOrderLineItemDto(
      List<ApprovedProductDto> approvedProductDtos, List<OrderLineItem> items) {
    Map<UUID, ApprovedProductDto> approvedProductMap = approvedProductDtos.stream()
        .collect(Collectors.toMap(approvedProductDto ->
            approvedProductDto.getOrderable().getId(), Function.identity()));
    return items.stream().map(item -> {
      org.openlmis.requisition.dto.OrderLineItemDto itemDto = new
          org.openlmis.requisition.dto.OrderLineItemDto();
      itemDto.setOrderable(approvedProductMap.get(item.getOrderable().getId()).getOrderable());
      itemDto.setOrderedQuantity(item.getOrderedQuantity());
      return itemDto;
    }).collect(Collectors.toList());
  }

}
