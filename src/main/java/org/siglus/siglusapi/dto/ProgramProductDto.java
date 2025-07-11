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

import static org.siglus.common.constant.KitConstants.ALL_KITS;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.dto.DispensableDto;
import org.siglus.common.domain.ProgramOrderablesExtension;

@Data
public class ProgramProductDto {
  private UUID id;
  private ApprovedOrderableDto orderable;
  private ApprovedProgramDto program;

  public ProgramProductDto(ProgramOrderable programOrderable, ProgramOrderablesExtension extension) {
    id = programOrderable.getId();
    orderable = new ApprovedOrderableDto(programOrderable, extension);
    program = new ApprovedProgramDto(programOrderable);
  }

  @Data
  public static class ApprovedOrderableDto {
    public static final String EXTRA_DATA_ORDERABLE_CATEGORY_DISPLAY_NAME = "orderableCategoryDisplayName";
    private UUID id;
    private Long versionNumber;
    private String productCode;
    private String fullProductName;
    private DispensableDto dispensable;
    private Boolean isKit;
    private Map<String, String> extraData = new HashMap<>();

    public ApprovedOrderableDto(ProgramOrderable programOrderable, ProgramOrderablesExtension extension) {
      Orderable product = programOrderable.getProduct();
      id = product.getId();
      versionNumber = product.getVersionNumber();
      productCode = product.getProductCode().toString();
      isKit = ALL_KITS.contains(product.getProductCode().toString());
      fullProductName = product.getFullProductName();
      if (extension != null) {
        dispensable = new DispensableDto(extension.getUnit(), "", "", extension.getUnit());
      } else {
        dispensable = new DispensableDto();
        product.getDispensable().export(dispensable);
      }
      extraData.put(EXTRA_DATA_ORDERABLE_CATEGORY_DISPLAY_NAME,
          programOrderable.getOrderableDisplayCategory().getOrderedDisplayValue().getDisplayName());
    }
  }

  @Data
  public static class ApprovedProgramDto {
    private UUID id;
    private String code;
    private String name;
    private Boolean active;

    public ApprovedProgramDto(ProgramOrderable programOrderable) {
      Program program = programOrderable.getProgram();
      id = program.getId();
      code = program.getCode().toString();
      name = program.getName();
      active = program.getActive();
    }
  }
}
