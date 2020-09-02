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
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.Cmm;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcIntegrationCmmService {

  @Autowired
  private CmmRepository cmmRepository;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Autowired
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  public boolean dealCmmData(List<CmmDto> dtos) {
    try {
      List<Cmm> cmms = Cmm.from(dtos);
      cmms.forEach(cmm -> {
        Cmm existCmm = cmmRepository
            .findCmmByFacilityCodeAndProductCodeAndPeriodAndYear(cmm.getFacilityCode(),
                cmm.getProductCode(), cmm.getPeriod(), cmm.getYear());
        if (null != existCmm) {
          cmm.setId(existCmm.getId());
        }
        cmmRepository.save(cmm);
      });
      log.info("save cmm successfully, size: {}", cmms.size());
      return true;
    } catch (Exception e) {
      log.error("deal cmm data error", e);
      return false;
    }
  }

  public void initiateSuggestedQuantity(List<BaseRequisitionLineItemDto> lineItems, UUID facilityId,
      UUID processingPeriodId) {
    if (CollectionUtils.isEmpty(lineItems)) {
      return;
    }
    Set<UUID> orderableIds = lineItems.stream()
        .map(lineItem -> {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          return lineItemV2Dto.getOrderable().getId();
        })
        .collect(Collectors.toSet());
    Map<UUID, String> orderableIdCodeMap = orderableReferenceDataService.findByIds(orderableIds)
        .stream().collect(toMap(OrderableDto::getId, OrderableDto::getProductCode));
    FacilityDto requestingFacility = facilityReferenceDataService
        .findOne(facilityId);
    ProcessingPeriodDto processingPeriodDto = processingPeriodReferenceDataService
        .findOne(processingPeriodId);
    LocalDate endDate = processingPeriodDto.getEndDate();
    Map<String, Cmm> productCodeCmmMap = cmmRepository
        .findAllByFacilityCodeAndProductCodeInAndPeriodAndYear(requestingFacility.getCode(),
            orderableIdCodeMap.values(), "M" + endDate.getMonthValue(), endDate.getYear())
        .stream().collect(toMap(Cmm::getProductCode, Function.identity()));
    List<RequisitionLineItemExtension> extensions = newArrayList();
    lineItems.forEach(lineItem -> {
      RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
      String productCode = orderableIdCodeMap.get(lineItemV2Dto.getOrderable().getId());
      Cmm cmm = productCodeCmmMap.get(productCode);
      int suggestedQuantity = 0;
      if (null != cmm) {
        suggestedQuantity = cmm.getCmm() * cmm.getMax() - lineItem.getStockOnHand();
        suggestedQuantity = Math.max(suggestedQuantity, 0);
      }
      lineItem.setSuggestedQuantity(suggestedQuantity);
      if (null != lineItem.getId()) {
        RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
        extension.setRequisitionLineItemId(lineItem.getId());
        extension.setAuthorizedQuantity(lineItem.getAuthorizedQuantity());
        extension.setSuggestedQuantity(suggestedQuantity);
        extensions.add(extension);
      }
    });
    if (CollectionUtils.isNotEmpty(extensions)) {
      log.info("save line item extension, size: {}", extensions.size());
      lineItemExtensionRepository.save(extensions);
    }
  }
}
