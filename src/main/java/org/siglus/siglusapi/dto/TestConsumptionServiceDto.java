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

package org.siglus.siglusapi.dto;

import static com.google.common.collect.Lists.newArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestConsumptionServiceDto {

  private String service;

  private Map<String, TestConsumptionProjectDto> projects;

  public static List<TestConsumptionServiceDto> from(List<TestConsumptionLineItem> lineItems) {
    List<TestConsumptionServiceDto> serviceDtos = newArrayList();
    Map<String, List<TestConsumptionLineItem>> groupByService =
        lineItems.stream().collect(Collectors.groupingBy(TestConsumptionLineItem::getService));

    groupByService.forEach((serviceKey, serviceLineItems) -> {
      TestConsumptionServiceDto serviceDto = new TestConsumptionServiceDto();
      serviceDto.setService(serviceKey);

      Map<String, TestConsumptionProjectDto> projectMap = new HashMap<>();
      Map<String, List<TestConsumptionLineItem>> groupByProject = serviceLineItems.stream()
          .collect(Collectors.groupingBy(TestConsumptionLineItem::getProject));

      groupByProject.forEach((projectKey, projectLineItems) -> {
        TestConsumptionProjectDto projectDto = new TestConsumptionProjectDto();
        projectDto.setProject(projectKey);
        Map<String, TestConsumptionOutcomeDto> outcomeMap = projectLineItems.stream()
            .collect(Collectors.toMap(TestConsumptionLineItem::getOutcome,
                lineItem -> TestConsumptionOutcomeDto
                    .builder()
                    .testConsumptionLineItemId(lineItem.getId())
                    .outcome(lineItem.getOutcome())
                    .value(lineItem.getValue())
                    .build()));
        projectDto.setOutcomes(outcomeMap);

        projectMap.put(projectKey, projectDto);
      });

      serviceDto.setProjects(projectMap);
      serviceDtos.add(serviceDto);
    });

    return serviceDtos;
  }
}
