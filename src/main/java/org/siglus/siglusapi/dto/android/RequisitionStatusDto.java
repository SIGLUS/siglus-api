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

package org.siglus.siglusapi.dto.android;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;

@Data
public class RequisitionStatusDto {
  @NotBlank
  private String id;
  @NotBlank
  @Pattern(regexp = "(ML)|(TR)|(T)|(VC)|(TB)",
      message = "{org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest.programCode}")
  private String programCode;
  @NotNull
  private String startDate;
  private RequisitionStatus status;

  @JsonIgnore
  public LocalDate getRequisitionStartDate() {
    return LocalDate.parse(startDate, new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter());
  }
}
