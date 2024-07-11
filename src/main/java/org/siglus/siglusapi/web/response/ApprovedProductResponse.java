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

package org.siglus.siglusapi.web.response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;

@Data
public class ApprovedProductResponse {
  private UUID id;
  private Long versionNumber;
  private ApprovedOrderableDto orderable;
  private ApprovedProgramDto program;

  public ApprovedProductResponse(ApprovedProductDto dto) {
    id = dto.getId();
    versionNumber = dto.getVersionNumber();
    orderable = new ApprovedOrderableDto(dto.getOrderable());
    program = new ApprovedProgramDto(dto.getProgram());
  }

  @Data
  public static class ApprovedOrderableDto {
    private UUID id;
    private String productCode;
    private String fullProductName;
    private DispensableDto dispensable;
    private List<ApprovedProgramOrderableDto> programs;

    public ApprovedOrderableDto(OrderableDto dto) {
      id = dto.getId();
      productCode = dto.getProductCode();
      fullProductName = dto.getFullProductName();
      dispensable = dto.getDispensable();
      if (dto.getPrograms() != null) {
        programs = dto.getPrograms().stream().map(ApprovedProgramOrderableDto::new).collect(Collectors.toList());
      }
    }
  }

  @Data
  public static class ApprovedProgramDto {
    private UUID id;
    private String code;
    private String name;
    private Boolean active;

    public ApprovedProgramDto(ProgramDto dto) {
      id = dto.getId();
      code = dto.getCode();
      name = dto.getName();
      active = dto.getActive();
    }
  }

  @Data
  public static class ApprovedProgramOrderableDto {
    private String orderableCategoryDisplayName;

    public ApprovedProgramOrderableDto(ProgramOrderableDto dto) {
      orderableCategoryDisplayName = dto.getOrderableCategoryDisplayName();
    }
  }
}
