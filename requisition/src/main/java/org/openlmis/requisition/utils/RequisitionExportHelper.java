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

package org.openlmis.requisition.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.BatchApproveRequisitionLineItemDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequisitionExportHelper {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(RequisitionExportHelper.class);

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  /**
   * Return list of RequisitionLineItemDtos for a given RequisitionLineItem.
   *
   * @param requisitionLineItems List of RequisitionLineItems to be exported to Dto
   * @return list of RequisitionLineItemDtos
   */
  public List<RequisitionLineItemDto> exportToDtos(List<RequisitionLineItem> requisitionLineItems) {
    return exportToDtos(requisitionLineItems, null, null, false);
  }

  /**
   * Return list of RequisitionLineItemDtos for a given RequisitionLineItem.
   *
   * @param requisitionLineItems List of RequisitionLineItems to be exported to Dto
   * @param orderables Map of Orderables by id
   * @return list of RequisitionLineItemDtos
   */
  public List<RequisitionLineItemDto> exportToDtos(List<RequisitionLineItem> requisitionLineItems,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts,
      boolean batch) {
    XLOGGER.entry(requisitionLineItems);
    Profiler profiler = new Profiler("EXPORT_LINE_ITEMS_TO_DTOS");
    profiler.setLogger(XLOGGER);

    Map<VersionIdentityDto, OrderableDto> orderablesForLines;
    if (orderables == null) {
      profiler.start("GET_ORDERABLE_IDS_FROM_LINE_ITEMS");
      Set<VersionEntityReference> orderableIds = new HashSet<>(requisitionLineItems.size());
      for (RequisitionLineItem lineItem : requisitionLineItems) {
        orderableIds.add(lineItem.getOrderable());
      }

      profiler.start("FIND_ORDERABLES_BY_IDS");
      orderablesForLines =
          orderableReferenceDataService
              .findByIdentities(orderableIds)
              .stream()
              .collect(Collectors.toMap(BasicOrderableDto::getIdentity, orderable -> orderable));

    } else {
      orderablesForLines = orderables;
    }

    Map<VersionIdentityDto, ApprovedProductDto> approvedProductsForLines;
    if (approvedProducts == null) {
      profiler.start("GET_APPROVED_PRODUCTS_IDS_FROM_LINE_ITEMS");
      Set<VersionEntityReference> approvedProductsIds = new HashSet<>(requisitionLineItems.size());
      for (RequisitionLineItem lineItem : requisitionLineItems) {
        approvedProductsIds.add(lineItem.getFacilityTypeApprovedProduct());
      }

      profiler.start("FIND_APPROVED_PRODUCTS_BY_IDS");
      approvedProductsForLines =
          facilityTypeApprovedProductReferenceDataService
              .findByIdentities(approvedProductsIds)
              .stream()
              .collect(Collectors.toMap(ApprovedProductDto::getIdentity, product -> product));

    } else {
      approvedProductsForLines = approvedProducts;
    }

    profiler.start("CONVERT_LINE_ITEMS_TO_DTOS");
    List<RequisitionLineItemDto> requisitionLineItemDtos =
        new ArrayList<>(requisitionLineItems.size());
    for (RequisitionLineItem lineItem : requisitionLineItems) {
      requisitionLineItemDtos.add(exportToDto(lineItem, orderablesForLines,
          approvedProductsForLines, batch));
    }

    profiler.stop().log();
    XLOGGER.exit(requisitionLineItemDtos);
    return requisitionLineItemDtos;
  }


  private RequisitionLineItemDto exportToDto(RequisitionLineItem requisitionLineItem,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts,
      boolean batch) {
    XLOGGER.entry(requisitionLineItem, orderables);
    Profiler profiler = new Profiler("EXPORT_LINE_ITEM_TO_DTO");
    profiler.setLogger(XLOGGER);

    profiler.start("GET_LINE_ITEM_ORDERABLE_FROM_ORDERABLES");
    final OrderableDto orderableDto = orderables
        .get(new VersionIdentityDto(requisitionLineItem.getOrderable()));

    profiler.start("GET_LINE_ITEM_APPROVED_PRODUCT_FROM_APPROVED_PRODUCTS");
    final ApprovedProductDto approvedProductDto = approvedProducts
        .get(new VersionIdentityDto(requisitionLineItem.getFacilityTypeApprovedProduct()));

    profiler.start("CONSTRUCT_REQUISITION_LINE_ITEM_DTO");
    RequisitionLineItemDto dto;
    if (batch) {
      dto = new BatchApproveRequisitionLineItemDto();
    } else {
      dto = new RequisitionLineItemDto();
    }

    profiler.start("EXPORT_TO_DTO");
    requisitionLineItem.export(dto, orderableDto, approvedProductDto);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }
}
