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

import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SKIPPED;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.siglusapi.domain.ReceiptPlan;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.ReceiptPlanRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcReceiptPlanService implements ProcessDataService {

  private final ReceiptPlanRepository receiptPlanRepository;
  private final SiglusFacilityRepository siglusFacilityRepository;
  private final SiglusSimulateUserAuthHelper siglusSimulateUserAuthHelper;
  private final RequisitionRepository requisitionRepository;
  private final SiglusStatusChangeRepository siglusStatusChangeRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final SiglusUserReferenceDataService userReferenceDataService;
  private final RequisitionService requisitionService;
  private final SiglusRequisitionService siglusRequisitionService;
  private final OperatePermissionService operatePermissionService;
  private final FcValidate fcDataValidate;
  private final HttpServletRequest request;
  private final HttpServletResponse response;

  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  private final List<String> receiptPlanErrors = new ArrayList<>();

  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> receiptPlans, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC receiptPlan] sync count: {}", receiptPlans.size());
    if (receiptPlans.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    int createCounter = 0;
    int ignoreCounter = 0;
    int duplicatedCounter = 0;
    try {
      receiptPlanErrors.clear();
      List<? extends ResponseBaseDto> receiptPlanDtoList = receiptPlans.stream()
          .distinct()
          .filter(this::isRequisitionNumberExisted)
          .collect(Collectors.toList());
      List<? extends ResponseBaseDto> needCreateReceiptPlans = getNeedCreateReceiptPlans(receiptPlanDtoList);
      createCounter = needCreateReceiptPlans.size();
      if (!isEmpty(needCreateReceiptPlans)) {
        needCreateReceiptPlans.forEach(receiptPlanDto -> {
          ReceiptPlan receiptPlan = ReceiptPlan.from((ReceiptPlanDto) receiptPlanDto);
          log.info("[FC receiptPlan] create: {}", receiptPlanDto);
          receiptPlanRepository.save(receiptPlan);
        });
      }
      UserDto userDto = getFcUserInfo();
      siglusSimulateUserAuthHelper.simulateUserAuth(userDto.getId());
      for (ResponseBaseDto receiptPlan : receiptPlanDtoList) {
        updateRequisition((ReceiptPlanDto) receiptPlan, userDto);
      }
      if (!CollectionUtils.isEmpty(receiptPlanErrors)) {
        finalSuccess = false;
      }
      ignoreCounter = receiptPlans.size() - receiptPlanDtoList.size();
      duplicatedCounter = receiptPlanDtoList.size() - needCreateReceiptPlans.size();
    } catch (Exception e) {
      log.error("[FC receiptPlan] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC receiptPlan] process data create: {}, update: {}, same: {}",
        createCounter, 0, receiptPlans.size() - createCounter);
    String errorMessage = String.format(
        "fc integration not exist our system count: %d and duplicated count %d",
        ignoreCounter, duplicatedCounter);
    return buildResult(
        new FcIntegrationResultBuildDto(RECEIPT_PLAN_API, receiptPlans, startDate,
            previousLastUpdatedAt, finalSuccess, createCounter, 0, errorMessage, null));
  }

  private boolean isRequisitionNumberExisted(ResponseBaseDto receiptPlanDto) {
    ReceiptPlanDto receiptPlan = (ReceiptPlanDto) receiptPlanDto;
    String requisitionNumber = receiptPlan.getRequisitionNumber();
    return !StringUtils.isEmpty(requisitionNumber)
        && requisitionExtensionRepository.findByRequisitionNumber(requisitionNumber) != null;
  }

  private void updateRequisition(ReceiptPlanDto receiptPlanDto, UserDto userDto) {
    try {
      RequisitionExtension extension = requisitionExtensionRepository.findByRequisitionNumber(
          receiptPlanDto.getRequisitionNumber());
      UUID requisitionId = extension.getRequisitionId();
      SiglusRequisitionDto requisitionDto = siglusRequisitionService.searchRequisition(requisitionId);
      if (operatePermissionService.isEditable(requisitionDto)) {
        List<RequisitionLineItemV2Dto> requisitionLineItems =
            requisitionDto
                .getLineItems()
                .stream()
                .map(RequisitionLineItemV2Dto.class::cast)
                .collect(Collectors.toList());
        Map<String, OrderableDto> approveProductDtos = getApprovedProductsMap(userDto, requisitionDto);
        List<ProductDto> productDtos = getExistProducts(receiptPlanDto, approveProductDtos);
        boolean displaySkipped = requisitionDto.getTemplate().getColumnsMap()
            .get(SKIPPED.toString().toLowerCase()).getIsDisplayed();
        List<RequisitionLineItemV2Dto> lineItems = updateRequisitionLineItems(
            requisitionLineItems, productDtos, approveProductDtos, requisitionId, displaySkipped);
        requisitionDto.setRequisitionLineItems(lineItems);
        siglusRequisitionService.updateRequisition(requisitionId, requisitionDto, request, response);
        siglusRequisitionService.approveRequisition(requisitionId, request, response);
        updateRequisitionChangeDate(requisitionId, receiptPlanDto);
        log.info("[FC] update receipt plan {}", receiptPlanDto);
      } else {
        receiptPlanErrors.add("requisition is not editable: " + receiptPlanDto.getReceiptPlanNumber());
      }
    } catch (FcDataException e) {
      receiptPlanErrors.add(e.getMessage() + ": " + receiptPlanDto.getReceiptPlanNumber());
    } catch (Exception e) {
      log.error("[FC] receipt plan: {}, exception: {}", receiptPlanDto, e);
      throw e;
    }
  }

  private UserDto getFcUserInfo() {
    Facility facility = siglusFacilityRepository.findFirstByTypeId(fcFacilityTypeId);
    fcDataValidate.validateFacility(facility);
    List<UserDto> userList = userReferenceDataService.getUserInfo(facility.getId()).getContent();
    fcDataValidate.validateExistUser(userList);
    return userList.get(0);
  }

  private Map<String, OrderableDto> getApprovedProductsMap(UserDto userDto, RequisitionV2Dto dto) {
    return requisitionService.getApprovedProducts(userDto.getHomeFacilityId(), dto.getProgramId())
        .stream()
        .map(ApprovedProductDto::getOrderable)
        .collect(Collectors.toMap(OrderableDto::getProductCode, orderableDto -> orderableDto));
  }

  private List<ProductDto> getExistProducts(ReceiptPlanDto receiptPlanDto, Map<String, OrderableDto> approvedProducts) {
    List<ProductDto> receiptPlanProducts = receiptPlanDto.getProducts();
    List<ProductDto> findProducts = receiptPlanProducts.stream()
        .filter(productDto -> approvedProducts.containsKey(productDto.getFnmCode()))
        .collect(Collectors.toList());
    if (findProducts.size() != receiptPlanDto.getProducts().size()) {
      List<ProductDto> notFindProducts = receiptPlanProducts.stream()
          .filter(productDto -> !approvedProducts.containsKey(productDto.getFnmCode()))
          .collect(Collectors.toList());
      if (!notFindProducts.isEmpty()) {
        receiptPlanErrors.add("receipt plan not found products: " + notFindProducts);
      }
    }
    fcDataValidate.validateFcProduct(findProducts);
    return findProducts;
  }

  private List<RequisitionLineItemV2Dto> updateRequisitionLineItems(
      List<RequisitionLineItemV2Dto> requisitionLineItems, List<ProductDto> existProductDtos,
      Map<String, OrderableDto> orderableDtoMap, UUID requisitionId, boolean displaySkipped) {
    List<UUID> addedOrderableIds = new ArrayList<>();
    List<RequisitionLineItemV2Dto> approvedLineItems = new ArrayList<>();
    updateExistLineItems(requisitionLineItems, existProductDtos, orderableDtoMap, addedOrderableIds, approvedLineItems);
    updateSkippedLineItems(requisitionLineItems, displaySkipped, approvedLineItems);
    addNewLineItems(existProductDtos, orderableDtoMap, requisitionId, addedOrderableIds, approvedLineItems);
    return approvedLineItems;
  }

  private void addNewLineItems(List<ProductDto> existProductDtos, Map<String,
      OrderableDto> orderableDtoMap, UUID requisitionId, List<UUID> addedOrderableIds,
      List<RequisitionLineItemV2Dto> approvedLineItems) {
    if (!isEmpty(addedOrderableIds)) {
      List<SiglusRequisitionLineItemDto> addLineItems = siglusRequisitionService
          .createRequisitionLineItem(requisitionId, addedOrderableIds);
      addLineItems.forEach(addLineItem -> {
        RequisitionLineItemV2Dto lineItem = addLineItem.getLineItem();
        ProductDto productDto = existProductDtos
            .stream()
            .filter(
                product -> orderableDtoMap.get(product.getFnmCode()).getId().equals(lineItem.getOrderable().getId()))
            .findFirst()
            .orElse(null);
        if (null != productDto) {
          lineItem.setApprovedQuantity(productDto.getApprovedQuantity());
          approvedLineItems.add(lineItem);
        }
      });
    }
  }

  private void updateSkippedLineItems(List<RequisitionLineItemV2Dto> requisitionLineItems,
      boolean displaySkipped, List<RequisitionLineItemV2Dto> approvedLineItems) {
    for (RequisitionLineItemV2Dto requisitionLineItem : requisitionLineItems) {
      RequisitionLineItemV2Dto approvedLineItem = approvedLineItems
          .stream()
          .filter(lineItemV2Dto -> lineItemV2Dto.getId().equals(requisitionLineItem.getId()))
          .findFirst()
          .orElse(null);
      if (null == approvedLineItem) {
        if (displaySkipped) {
          requisitionLineItem.setSkipped(true);
        } else {
          requisitionLineItem.setApprovedQuantity(0);
        }
        approvedLineItems.add(requisitionLineItem);
      }
    }
  }

  private void updateExistLineItems(List<RequisitionLineItemV2Dto> requisitionLineItems,
      List<ProductDto> existProductDtos, Map<String, OrderableDto> orderableDtoMap,
      List<UUID> addedOrderableIds, List<RequisitionLineItemV2Dto> approvedLineItems) {
    for (ProductDto productDto : existProductDtos) {
      RequisitionLineItemV2Dto requisitionLineItem = requisitionLineItems
          .stream()
          .filter(
              lineItem -> lineItem.getOrderable().getId().equals(orderableDtoMap.get(productDto.getFnmCode()).getId()))
          .findFirst()
          .orElse(null);
      if (null == requisitionLineItem) {
        addedOrderableIds.add(orderableDtoMap.get(productDto.getFnmCode()).getId());
      } else {
        requisitionLineItem.setApprovedQuantity(productDto.getApprovedQuantity());
        approvedLineItems.add(requisitionLineItem);
      }
    }
    requisitionLineItems.removeAll(approvedLineItems);
  }

  private List<? extends ResponseBaseDto> getNeedCreateReceiptPlans(List<? extends ResponseBaseDto> receiptPlanDtos) {
    List<String> receiptNumbers = receiptPlanDtos.stream()
        .map(ReceiptPlanDto.class::cast)
        .map(ReceiptPlanDto::getReceiptPlanNumber).collect(Collectors.toList());
    List<String> existReceiptNumbers = receiptPlanRepository
        .findByReceiptPlanNumberIn(receiptNumbers)
        .stream().map(ReceiptPlan::getReceiptPlanNumber)
        .collect(Collectors.toList());
    return receiptPlanDtos.stream()
        .map(ReceiptPlanDto.class::cast)
        .filter(receiptPlanDto -> !existReceiptNumbers.contains(receiptPlanDto.getReceiptPlanNumber()))
        .collect(Collectors.toList());
  }

  private void updateRequisitionChangeDate(UUID requisitionId, ReceiptPlanDto receiptPlanDto) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    requisition.setModifiedDate(receiptPlanDto.getLastUpdatedAt());
    log.info("[FC] update requisition: {}", requisition);
    requisitionRepository.save(requisition);
    StatusChange statusChange = siglusStatusChangeRepository.findByRequisitionIdAndStatus(requisitionId,
        APPROVED.toString());
    statusChange.setCreatedDate(receiptPlanDto.getDate());
  }

}
