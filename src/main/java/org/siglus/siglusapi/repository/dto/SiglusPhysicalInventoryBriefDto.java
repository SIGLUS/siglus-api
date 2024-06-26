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

package org.siglus.siglusapi.repository.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryDto;
import org.siglus.siglusapi.dto.enums.LocationManagementOption;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@NamedNativeQueries({
    @NamedNativeQuery(
        name = "PhysicalInventory.queryForAllProgram",
        query = "SELECT pi.id as id, pi.programid as programId, pi.facilityid as facilityId, "
            + "  pi.occurreddate as occurredDate, pi.signature as signature, pi.documentnumber as documentNumber, "
            + "  pi.isdraft as isDraft, pie.category as category, pie.locationoption as locationOption "
            + "from stockmanagement.physical_inventories pi "
            + "left join siglusintegration.physical_inventories_extension pie "
            + "       on pi.id  = pie.physicalinventoryid "
            + "where pi.facilityid = :facilityId and pi.isdraft = :isDraft and pie.category = 'ALL' "
            + ";",
        resultSetMapping = "PhysicalInventory.SiglusPhysicalInventoryBriefDto"),

    @NamedNativeQuery(
        name = "PhysicalInventory.queryForOneProgram",
        query = "SELECT pi.id as id, pi.programid as programId, pi.facilityid as facilityId,  "
            + "  pi.occurreddate as occurredDate, pi.signature as signature, pi.documentnumber as documentNumber, "
            + "  pi.isdraft as isDraft, pie.category as category, pie.locationoption as locationOption "
            + "from stockmanagement.physical_inventories pi "
            + "left join siglusintegration.physical_inventories_extension pie "
            + "       on pi.id  = pie.physicalinventoryid "
            + "where pi.facilityid = :facilityId and pi.isdraft = :isDraft and pie.category = 'SINGLE' "
            + "      and pi.programid = :programId "
            + ";",
        resultSetMapping = "PhysicalInventory.SiglusPhysicalInventoryBriefDto"),

    @NamedNativeQuery(
        name = "PhysicalInventory.queryAllDraftByFacility",
        query = "SELECT pi.id as id, pi.programid as programId, pi.facilityid as facilityId, "
            + "  pi.occurreddate as occurredDate, pi.signature as signature, pi.documentnumber as documentNumber, "
            + "  pi.isdraft as isDraft, pie.category as category, pie.locationoption as locationOption "
            + "from stockmanagement.physical_inventories pi "
            + "left join siglusintegration.physical_inventories_extension pie "
            + "       on pi.id  = pie.physicalinventoryid "
            + "where pi.facilityid = :facilityId and pi.isdraft = true and pie.category in ('ALL', 'SINGLE') "
            + ";",
        resultSetMapping = "PhysicalInventory.SiglusPhysicalInventoryBriefDto"),

    @NamedNativeQuery(
        name = "PhysicalInventory.findByProgramIdAndFacilityIdAndStartDateAndEndDate",
        query = "SELECT pi.id as id, pi.programid as programId, pi.facilityid as facilityId,  "
            + "  pi.occurreddate as occurredDate, pi.signature as signature, pi.documentnumber as documentNumber, "
            + "  pi.isdraft as isDraft, pie.category as category, pie.locationoption as locationOption "
            + "from stockmanagement.physical_inventories pi "
            + "left join siglusintegration.physical_inventories_extension pie "
            + "       on pi.id  = pie.physicalinventoryid "
            + "where pi.facilityid = :facilityId and pi.programid = :programId "
            + "      and pi.occurreddate >= :startDate and pi.occurreddate <= :endDate "
            + ";",
        resultSetMapping = "PhysicalInventory.SiglusPhysicalInventoryBriefDto")
})

@MappedSuperclass
@SqlResultSetMapping(
    name = "PhysicalInventory.SiglusPhysicalInventoryBriefDto",
    classes = @ConstructorResult(
        targetClass = SiglusPhysicalInventoryBriefDto.class,
        columns = {
            @ColumnResult(name = "id", type = UUID.class),
            @ColumnResult(name = "programId", type = UUID.class),
            @ColumnResult(name = "facilityId", type = UUID.class),
            @ColumnResult(name = "occurredDate", type = LocalDate.class),
            @ColumnResult(name = "signature", type = String.class),
            @ColumnResult(name = "documentNumber", type = String.class),
            @ColumnResult(name = "isDraft", type = Boolean.class),
            @ColumnResult(name = "category", type = String.class),
            @ColumnResult(name = "locationOption", type = String.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiglusPhysicalInventoryBriefDto {
  private UUID id;
  private UUID programId;
  private UUID facilityId;
  @JsonFormat(
      shape = JsonFormat.Shape.STRING
  )
  private LocalDate occurredDate;
  private String signature;
  private String documentNumber;
  private Boolean isDraft;
  private String category;
  private String locationOption;

  public LocationManagementOption getLocationOption() {
    if (locationOption == null) {
      return null;
    }
    return LocationManagementOption.valueOf(locationOption);
  }

  public SiglusPhysicalInventoryDto toSiglusPhysicalInventoryDto() {
    SiglusPhysicalInventoryDto dto = new SiglusPhysicalInventoryDto();
    dto.setId(id);
    dto.setProgramId(programId);
    dto.setFacilityId(facilityId);
    dto.setOccurredDate(occurredDate);
    dto.setSignature(signature);
    dto.setDocumentNumber(documentNumber);
    dto.setIsDraft(isDraft);
    LocationManagementOption locationOption = getLocationOption();
    if (!ObjectUtils.isEmpty(locationOption)) {
      dto.setLocationOption(locationOption.getValue());
    }
    return dto;
  }
}
