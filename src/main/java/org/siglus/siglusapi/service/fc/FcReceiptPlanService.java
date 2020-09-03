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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.ReceiptPlan;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.repository.ReceiptPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@Slf4j
public class FcReceiptPlanService {

  @Autowired
  private ReceiptPlanRepository receiptPlanRepository;

  public boolean saveReceiptPlan(List<ReceiptPlanDto> receiptPlanDtos) {
    if (isEmpty(receiptPlanDtos)) {
      return true;
    }
    List<ReceiptPlanDto> nonexistentReceiptPlans = getNonexistentReceiptPlan(receiptPlanDtos);
    if (isEmpty(nonexistentReceiptPlans)) {
      return true;
    }
    nonexistentReceiptPlans.forEach(receiptPlanDto -> {
      ReceiptPlan receiptPlan = ReceiptPlan.from(receiptPlanDto);
      receiptPlanRepository.save(receiptPlan);
    });
    return true;
  }

  private List<ReceiptPlanDto> getNonexistentReceiptPlan(List<ReceiptPlanDto> receiptPlanDtos) {
    Set<String> receiptNumbers = receiptPlanRepository.findAllReceiptPlanNumbers();
    return receiptPlanDtos.stream().filter(receiptPlanDto ->
        !receiptNumbers.contains(receiptPlanDto.getReceiptPlanNumber()))
        .collect(Collectors.toList());
  }
}
