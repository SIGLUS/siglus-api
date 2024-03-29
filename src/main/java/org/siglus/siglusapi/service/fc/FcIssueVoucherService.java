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

import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;
import static org.siglus.siglusapi.util.SiglusDateHelper.DATE_MONTH_YEAR;
import static org.siglus.siglusapi.util.SiglusDateHelper.getFormatDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.OrderController;
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
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionStatusProcessor;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.web.OrderDtoBuilder;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.ShipmentsExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.event.fc.issuevoucher.FcIssueVoucherEmitter;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentsExtensionRepository;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusShipmentDraftService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.LocalMachineHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD"})
public class FcIssueVoucherService implements ProcessDataService {

  private static final String SPLIT = ": ";

  private final FcValidate fcDataValidate;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final SiglusUserReferenceDataService userReferenceDataService;
  private final SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionService requisitionService;
  private final ShipmentsExtensionRepository shipmentsExtensionRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final OrderRepository orderRepository;
  private final SiglusOrderService siglusOrderService;
  private final SiglusShipmentDraftService shipmentDraftService;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final SiglusShipmentService siglusShipmentService;
  private final SiglusSimulateUserAuthHelper simulateUser;
  private final RequisitionRepository requisitionRepository;
  private final RequisitionStatusProcessor requisitionStatusProcessor;
  private final OrderDtoBuilder orderDtoBuilder;
  private final OrderFulfillmentService orderFulfillmentService;
  private final SiglusLotService siglusLotService;
  private final FcIssueVoucherEmitter fcIssueVoucherEmitter;
  private final Machine machine;
  private final LocalMachineHelper localMachineHelper;

  private final OrderController orderController;

  private final List<String> issueVoucherErrors = new ArrayList<>();

  private UUID currentSupplierFacilityId;

  private final FcCreateIssueVoucherService fcCreateIssueVoucherService;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> issueVouchers, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC issueVoucher] sync count: {}", issueVouchers.size());
    if (issueVouchers.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    int createCounter = 0;
    int ignoreCounter = 0;
    int duplicatedCounter = 0;
    String errorMessage = "";
    List<? extends ResponseBaseDto> issueVoucherList = new ArrayList<>();
    try {
      issueVoucherErrors.clear();
      issueVoucherList = issueVouchers.stream()
          .distinct()
          .filter(this::isRequisitionNumberExisted)
          .collect(Collectors.toList());
      for (ResponseBaseDto item : issueVoucherList) {
        IssueVoucherDto issueVoucherDto = (IssueVoucherDto) item;
        ShipmentsExtension shipmentsExtension = shipmentsExtensionRepository.findByClientCodeAndIssueVoucherNumber(
            issueVoucherDto.getClientCode(), issueVoucherDto.getIssueVoucherNumber());
        if (shipmentsExtension == null) {
          log.info("[FC issueVoucher] create: {}", issueVoucherDto);
          fcCreateIssueVoucherService.createIssueVoucher(issueVoucherDto, issueVoucherErrors);
          createCounter++;
        } else {
          duplicatedCounter++;
        }
      }
      if (!CollectionUtils.isEmpty(issueVoucherErrors)) {
        log.error("[FC issueVoucher] issueVoucherErrors: {}",
            issueVoucherErrors.stream().collect(Collectors.joining(";")));
        finalSuccess = false;
      }
      ignoreCounter = issueVouchers.size() - issueVoucherList.size();
    } catch (Exception e) {
      log.error("[FC issueVoucher] process data error {}", e.toString());
      issueVoucherErrors.add(e.toString());
      e.printStackTrace();
      finalSuccess = false;
    }
    log.info("[FC issueVoucher] process data create: {}, update: {}, same: {}",
        createCounter, 0, issueVouchers.size() - createCounter);
    log.info("[FC issueVoucher] fc integration not exist our system count: {} and duplicated count {}",
        ignoreCounter, duplicatedCounter);
    if (!finalSuccess) {
      errorMessage = issueVoucherErrors.stream().collect(Collectors.joining(";"));
    }
    return buildResult(
        new FcIntegrationResultBuildDto(ISSUE_VOUCHER_API, issueVoucherList, startDate, previousLastUpdatedAt,
            finalSuccess, createCounter, 0, errorMessage, null));
  }

  private boolean isRequisitionNumberExisted(ResponseBaseDto receiptPlanDto) {
    IssueVoucherDto receiptPlan = (IssueVoucherDto) receiptPlanDto;
    String requisitionNumber = receiptPlan.getRequisitionNumber();
    return !StringUtils.isEmpty(requisitionNumber)
        && requisitionExtensionRepository.findByRequisitionNumber(requisitionNumber) != null;
  }

  public List<String> getIssueVoucherErrors() {
    return this.issueVoucherErrors;
  }

  public void createIssueVoucher(IssueVoucherDto issueVoucherDto) {
    try {
      RequisitionExtension extension = requisitionExtensionRepository.findByRequisitionNumber(
          issueVoucherDto.getRequisitionNumber());
      FacilityDto supplyFacility = getWareHouseFacility(issueVoucherDto);
      UserDto userDto = getWareHouseUserInfo(supplyFacility);
      getClientFacility(issueVoucherDto);
      RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService.searchRequisition(
          extension.getRequisitionId());
      if (!requisitionV2Dto.getStatus().isApproved()) {
        issueVoucherErrors.add("requisition status is error" + SPLIT + issueVoucherDto.getIssueVoucherNumber());
        return;
      }
      List<ApprovedProductDto> approvedProductDtos = requisitionService
          .getAllApprovedProducts(userDto.getHomeFacilityId(), requisitionV2Dto.getProgramId());
      Map<String, ApprovedProductDto> approvedProductsMap = getApprovedProductsMap(approvedProductDtos);
      List<ProductDto> existProducts = getExistProducts(issueVoucherDto, approvedProductsMap);
      if (!CollectionUtils.isEmpty(existProducts)) {
        simulateUser.simulateUserAuth(userDto.getId());
        // before shipment, need sufficient products for the warehouse first,
        // so that it can have enough products to fulfill when do shipment
        log.info("FC] sufficient products for AC warehouse");
        Map<String, List<ProductDto>> productMaps = existProducts.stream()
            .collect(Collectors.groupingBy(ProductDto::getFnmCode));
        UUID orderId = createOrder(requisitionV2Dto, approvedProductDtos, productMaps,
            supplyFacility, userDto, approvedProductsMap, issueVoucherDto);
        if (orderId != null) {
          // when do shipment for AC warehouse, it will fulfill products
          log.info("FC] create shipment for AC warehouse");
          ShipmentDto shipmentDto = createShipmentDraftAndShipment(orderId, productMaps, approvedProductDtos,
              approvedProductsMap, issueVoucherDto);
          saveFcShipmentsExtension(issueVoucherDto, shipmentDto);
          boolean isLocalMachine = localMachineHelper.isLocalMachine(requisitionV2Dto.getFacilityId());
          if (machine.isOnlineWeb() && isLocalMachine) {
            fcIssueVoucherEmitter.emit(issueVoucherDto, requisitionV2Dto.getFacilityId(),
                issueVoucherDto.getRequisitionNumber());
          }
        }
      } else {
        issueVoucherErrors.add("product not exist error" + SPLIT + issueVoucherDto.getIssueVoucherNumber());
      }
    } catch (FcDataException e) {
      issueVoucherErrors.add(e.getMessage() + SPLIT + issueVoucherDto.getIssueVoucherNumber());
    } catch (Exception e) {
      log.error("[FC] issue voucher: {}, exception: {}", issueVoucherDto, e.toString());
      throw e;
    }
  }

  private void getClientFacility(IssueVoucherDto issueVoucherDto) {
    String clientCode = issueVoucherDto.getClientCode();
    fcDataValidate.validateEmptyFacilityCode(clientCode);
    List<org.siglus.siglusapi.dto.FacilityDto> clientCodeList =
        siglusFacilityReferenceDataService.getFacilityByCode(clientCode).getContent();
    fcDataValidate.validateExistFacility(clientCodeList);
    currentSupplierFacilityId = clientCodeList.get(0).getId();
  }

  private FacilityDto getWareHouseFacility(IssueVoucherDto issueVoucherDto) {
    String warehouseCode = issueVoucherDto.getWarehouseCode();
    fcDataValidate.validateEmptyFacilityCode(warehouseCode);
    List<org.siglus.siglusapi.dto.FacilityDto> facilityDtos =
        siglusFacilityReferenceDataService.getFacilityByCode(warehouseCode).getContent();
    fcDataValidate.validateExistFacility(facilityDtos);
    return facilityDtos.get(0);
  }

  private UserDto getWareHouseUserInfo(FacilityDto facilityDto) {
    List<UserDto> userList = userReferenceDataService.getUserInfo(facilityDto.getId()).getContent();
    fcDataValidate.validateExistUser(userList);
    return userList.get(0);
  }

  private Map<String, ApprovedProductDto> getApprovedProductsMap(List<ApprovedProductDto> approvedProductDtos) {
    return approvedProductDtos.stream()
        .collect(Collectors.toMap(product -> product.getOrderable().getProductCode(), Function.identity()));
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

  private UUID createOrder(RequisitionV2Dto v2Dto, List<ApprovedProductDto> approvedProductDtos,
      Map<String, List<ProductDto>> productMaps, FacilityDto supplyFacility, UserDto userDto,
      Map<String, ApprovedProductDto> approvedProductMap, IssueVoucherDto issueVoucherDto) {
    List<OrderExternal> externals = orderExternalRepository.findByRequisitionId(v2Dto.getId());
    Order firstOrder = orderRepository.findByExternalId(v2Dto.getId());
    if (CollectionUtils.isEmpty(externals) && firstOrder == null) {
      return convertOrder(v2Dto, productMaps, supplyFacility, userDto, approvedProductDtos,
          approvedProductMap, issueVoucherDto);
    } else {
      return updateSubOrder(firstOrder, approvedProductMap, supplyFacility, productMaps, externals, issueVoucherDto);
    }
  }

  private UUID updateSubOrder(Order firstOrder,
      Map<String, ApprovedProductDto> approveProductDtos,
      FacilityDto supplyFacility,
      Map<String, List<ProductDto>> productMaps, List<OrderExternal> externals,
      IssueVoucherDto issueVoucherDto) {
    List<UUID> externalIds = externals.stream().map(OrderExternal::getId).collect(Collectors.toList());
    Order canFulfillOrder =
        CollectionUtils.isEmpty(externalIds) ? null : orderRepository.findCanFulfillOrderByExternalIdIn(externalIds);
    if (canFulfillOrder == null) {
      Order existOrder = firstOrder != null ? firstOrder : orderRepository.findLastOrderByExternalIds(externalIds);
      OrderDto orderDto = siglusOrderService.searchOrderByIdForMultiWareHouseSupply(existOrder.getId()).getOrder();
      org.openlmis.fulfillment.service.referencedata.FacilityDto fulfillFacilityDto =
          new org.openlmis.fulfillment.service.referencedata.FacilityDto();
      BeanUtils.copyProperties(supplyFacility, fulfillFacilityDto);
      orderDto.setSupplyingFacility(fulfillFacilityDto);
      OrderObjectReferenceDto dto = new OrderObjectReferenceDto(orderDto.getId());
      orderDto.setCreatedDate(issueVoucherDto.getIssueDate());
      BeanUtils.copyProperties(orderDto, dto);
      Iterable<BasicOrderDto> orderDtos = siglusOrderService.createSubOrder(dto,
          getOrderLineItemsDtoInIssue(productMaps, approveProductDtos));
      if (orderDtos.iterator().hasNext()) {
        return orderDtos.iterator().next().getId();
      }
      return null;
    } else {
      return updateCanFulfillOrder(approveProductDtos, productMaps, canFulfillOrder, issueVoucherDto, supplyFacility);
    }
  }

  private UUID updateCanFulfillOrder(Map<String, ApprovedProductDto> approveProductDtos,
      Map<String, List<ProductDto>> productMaps, Order canFulfillOrder,
      IssueVoucherDto issueVoucherDto, FacilityDto supplyFacility) {
    List<OrderLineItem> existLineItems = canFulfillOrder.getOrderLineItems();
    List<OrderLineItemDto> orderLineItemDto = getOrderLineItemsDtoInIssue(productMaps, approveProductDtos);
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
    canFulfillOrder.setCreatedDate(issueVoucherDto.getIssueDate());
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
      org.openlmis.requisition.dto.OrderableDto existOrderableDto = approvedProductDto.getOrderable();
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

  private void saveFcShipmentsExtension(IssueVoucherDto dto, ShipmentDto shipmentDto) {
    ShipmentsExtension shipmentsExtension = new ShipmentsExtension();
    shipmentsExtension.setShipmentId(shipmentDto.getId());
    shipmentsExtension.setClientCode(dto.getClientCode());
    shipmentsExtension.setIssueVoucherNumber(dto.getIssueVoucherNumber());
    log.info("save fc shipments extension: {}", shipmentsExtension);
    shipmentsExtensionRepository.save(shipmentsExtension);
  }

  private ShipmentDto createShipmentDraftAndShipment(UUID orderId,
      Map<String, List<ProductDto>> productMaps,
      List<ApprovedProductDto> approvedProductDtos,
      Map<String, ApprovedProductDto> approvedProductDtoMaps,
      IssueVoucherDto issueVoucherDto) {
    SiglusOrderDto orderDto = siglusOrderService.searchOrderByIdForMultiWareHouseSupply(orderId);
    ShipmentDto shipmentDto = new ShipmentDto();
    OrderObjectReferenceDto orderReferenceDto = new OrderObjectReferenceDto(orderId);
    BeanUtils.copyProperties(orderDto.getOrder(), orderReferenceDto);
    orderReferenceDto.setOrderLineItems(orderDto.getOrder().orderLineItems());
    shipmentDto.setOrder(orderReferenceDto);
    Map<UUID, ApprovedProductDto> approvedProductIdMaps = approvedProductDtos.stream()
        .collect(Collectors.toMap(product -> product.getOrderable().getId(), Function.identity()));
    ShipmentDraftDto draftDto = createShipmentDraft(orderDto, approvedProductDtoMaps, productMaps);
    shipmentDto.setId(draftDto.getId());
    shipmentDto.setShippedDate(issueVoucherDto.getShippingDate());
    shipmentDto.setLineItems(getShipmentLineItems(draftDto.lineItems(), issueVoucherDto, approvedProductIdMaps));
    return siglusShipmentService.createSubOrderAndShipmentForFc(shipmentDto);
  }

  private ShipmentDraftDto createShipmentDraft(SiglusOrderDto orderDto,
      Map<String, ApprovedProductDto> approvedProductDtoMaps,
      Map<String, List<ProductDto>> productMaps) {
    List<ShipmentLineItemDto> shipmentLineItemDtos = getShipmentDraftLineItems(approvedProductDtoMaps, productMaps);
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(orderDto.getOrder().getId());
    BeanUtils.copyProperties(orderDto.getOrder(), orderObjectReferenceDto);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(orderObjectReferenceDto);
    draftDto.setLineItems(shipmentLineItemDtos);
    return shipmentDraftService.createShipmentDraft(draftDto);
  }

  private List<ShipmentLineItemDto> getShipmentDraftLineItems(
      Map<String, ApprovedProductDto> approvedProductDtoMaps,
      Map<String, List<ProductDto>> productMaps) {
    List<ShipmentLineItemDto> shipmentLineItemDtos = new ArrayList<>();
    productMaps.forEach((code, productDtos) -> {
      ApprovedProductDto approvedProductDto = approvedProductDtoMaps.get(code);
      if (approvedProductDto == null) {
        return;
      }
      OrderableDto orderableDto = approvedProductDto.getOrderable();
      productDtos.forEach(productDto -> shipmentLineItemDtos.add(
          buildShipmentLineItemDto(orderableDto.getId(), orderableDto.getVersionNumber(),
              generateLotId(orderableDto.getIdentifiers(), productDto))));
    });
    return shipmentLineItemDtos;
  }

  private ShipmentLineItemDto buildShipmentLineItemDto(UUID orderableId, Long versionNumber, UUID lotId) {
    VersionObjectReferenceDto orderableReferenceDto = new VersionObjectReferenceDto(orderableId, null, "orderables",
        versionNumber);
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setOrderable(orderableReferenceDto);
    lineItemDto.setQuantityShipped((long) 0);
    lineItemDto.setLotId(lotId);
    return lineItemDto;
  }

  private UUID generateLotId(Map<String, String> identifiers, ProductDto productDto) {
    LocalDate expirationDate = LocalDate.parse(SiglusDateHelper.formatDate(productDto.getExpiryDate()));
    String lotCode = formatLotCode(productDto.getBatch(), productDto.getExpiryDate());
    String tradeItemId = identifiers.get(TRADE_ITEM);
    return siglusLotService.createNewLotOrReturnExisted(currentSupplierFacilityId, tradeItemId, lotCode, expirationDate)
        .getId();

  }

  private String formatLotCode(String lotCode, Date expiryDate) {
    LocalDate expirationDate = LocalDate.parse(SiglusDateHelper.formatDate(expiryDate));
    return lotCode + "-" + getFormatDate(expirationDate, DATE_MONTH_YEAR);
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
                && formatLotCode(dto.getBatch(), dto.getExpiryDate()).equals(lotDto.getLotCode()))
        .findFirst()
        .orElse(null);
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
    List<OrderLineItem> items = getOrderLineItems(existProductDtos, approveProductMap, new Order());
    order.setOrderLineItems(convertToOrderLineItemDto(approvedProductDtos, items));
    order.setCreatedDate(issueVoucherDto.getIssueDate());
    orderController.batchCreateOrders(Collections.singletonList(convertOrderDto(order)),
        (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
  }

  private org.openlmis.fulfillment.web.util.OrderDto convertOrderDto(org.openlmis.requisition.dto.OrderDto o) {
    org.openlmis.fulfillment.web.util.OrderDto order = new OrderDto();
    BeanUtils.copyProperties(o, order);
    order.setExternalId(o.getExternalId());
    order.setEmergency(o.getEmergency());
    org.openlmis.fulfillment.service.referencedata.FacilityDto newFacilityDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    BeanUtils.copyProperties(o.getFacility(), newFacilityDto);
    order.setFacility(newFacilityDto);
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto newProcessingPeriodDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    BeanUtils.copyProperties(o.getProcessingPeriod(), newProcessingPeriodDto);
    order.setProcessingPeriod(newProcessingPeriodDto);
    order.setQuotedCost(BigDecimal.ZERO);
    org.openlmis.fulfillment.service.referencedata.FacilityDto newReceivingFacilityDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    BeanUtils.copyProperties(o.getReceivingFacility(), newReceivingFacilityDto);
    order.setReceivingFacility(newReceivingFacilityDto);
    org.openlmis.fulfillment.service.referencedata.FacilityDto newRequestingFacilityDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    BeanUtils.copyProperties(o.getReceivingFacility(), newRequestingFacilityDto);
    order.setRequestingFacility(newRequestingFacilityDto);
    org.openlmis.fulfillment.service.referencedata.FacilityDto newSupplyingFacilityDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    BeanUtils.copyProperties(o.getSupplyingFacility(), newSupplyingFacilityDto);
    order.setSupplyingFacility(newSupplyingFacilityDto);
    org.openlmis.fulfillment.service.referencedata.ProgramDto newProgramDto =
        new org.openlmis.fulfillment.service.referencedata.ProgramDto();
    BeanUtils.copyProperties(o.getProgram(), newProgramDto);
    order.setProgram(newProgramDto);
    List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItems = o.getOrderLineItems()
        .stream()
        .map(lineItem -> {
          org.openlmis.fulfillment.web.util.OrderLineItemDto newLine =
              new org.openlmis.fulfillment.web.util.OrderLineItemDto();
          BeanUtils.copyProperties(lineItem, newLine);
          org.openlmis.fulfillment.service.referencedata.OrderableDto newOrderable =
              new org.openlmis.fulfillment.service.referencedata.OrderableDto();
          BeanUtils.copyProperties(lineItem.getOrderable(), newOrderable);
          org.openlmis.fulfillment.web.util.MetadataDto newMeta =
              new org.openlmis.fulfillment.web.util.MetadataDto();
          BeanUtils.copyProperties(lineItem.getOrderable().getMeta(), newMeta);
          newOrderable.setMeta(newMeta);
          newLine.setOrderable(newOrderable);
          return newLine;
        })
        .collect(Collectors.toList());
    order.setOrderLineItems(orderLineItems);
    order.setStatusChanges(new ArrayList<>());
    order.setStatusMessages(new ArrayList<>());
    order.setUpdaterId(o.getLastUpdater().getId());
    org.openlmis.fulfillment.service.referencedata.UserDto newUser =
        new org.openlmis.fulfillment.service.referencedata.UserDto();
    BeanUtils.copyProperties(o.getCreatedBy(), newUser);
    order.setCreatedBy(newUser);
    return order;
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
