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

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQueries({
    @NamedNativeQuery(name = "HistoricalDateService.getFacilityLatestRequisitionDate",
        query = "select\n"
            + "  distinct\n"
            + "  f.facilityid,\n"
            + "  first_value(hdpd.enddate ) over "
            + "  (partition by f.facilityid order by hdpd.enddate desc) as lastrequisitiondate\n"
            + "from\n"
            + "  (\n"
            + "  select\n"
            + "    r.facilityid\n"
            + "  from\n"
            + "    requisition.requisitions r\n"
            + "  join referencedata.processing_periods pp on\n"
            + "    r.processingperiodid = pp.id\n"
            + ") f\n"
            + "left join dashboard.historical_data_persistent_data hdpd on\n"
            + "  hdpd.facilityid = f.facilityid\n",
        resultSetMapping = "HistoricalDateService.FacilityLastRequisitionTimeDto"),
    @NamedNativeQuery(name = "HistoricalDateService.getFacilityLatestRequisitionDateByFacilitys",
        query = "select\n"
            + "  distinct\n"
            + "  f.facilityid,\n"
            + "  first_value(hdpd.enddate ) over "
            + "  (partition by f.facilityid order by hdpd.enddate desc) as lastrequisitiondate\n"
            + "from\n"
            + "  (\n"
            + "  select\n"
            + "    r.facilityid\n"
            + "  from\n"
            + "    requisition.requisitions r\n"
            + "  join referencedata.processing_periods pp on\n"
            + "    r.processingperiodid = pp.id\n"
            + ") f\n"
            + "left join dashboard.historical_data_persistent_data hdpd on\n"
            + "  hdpd.facilityid = f.facilityid\n"
            + "where f.facilityid in :facilityIds",
        resultSetMapping = "HistoricalDateService.FacilityLastRequisitionTimeDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "HistoricalDateService.FacilityLastRequisitionTimeDto",
        classes = {
            @ConstructorResult(
                targetClass = FacilityLastRequisitionTimeDto.class,
                columns = {
                    @ColumnResult(name = "facilityId", type = UUID.class),
                    @ColumnResult(name = "lastRequisitionDate", type = LocalDate.class)
                }
            )
        }
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacilityLastRequisitionTimeDto {

  UUID facilityId;
  LocalDate lastRequisitionDate;
}
