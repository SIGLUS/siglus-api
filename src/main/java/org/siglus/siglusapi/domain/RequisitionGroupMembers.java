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

package org.siglus.siglusapi.domain;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;

@Entity
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RequisitionGroupMembersPrimaryKey.class)
@Table(name = "requisition_group_members", schema = "referencedata")
@NamedNativeQueries({
    @NamedNativeQuery(name = "RequisitionGroupMembers.findRequisitonIdFacilityId",
        query = "select rm.requisitiongroupid as requisitionGroupId,rm.facilityid as facilityId,"
            + "ps.programid as programId "
            + "from referencedata.requisition_group_members rm "
            + "inner join "
            + "referencedata.requisition_groups rg on rm.requisitiongroupid = rg.id "
            + "inner join "
            + "referencedata.requisition_group_program_schedules ps on rg.id = ps.requisitiongroupid "
            + "inner join "
            + "referencedata.supervisory_nodes rs on rs.id=rg.supervisorynodeid "
            + "where rs.facilityid = :facilityId "
            + "and rs.parentid is not null "
            + "and ps.programid in :programIds",
        resultSetMapping = "RequisitionGroupMembers.RequisitionGroupMembersDto")
})
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RequisitionGroupMembers.RequisitionGroupMembersDto",
        classes = {
            @ConstructorResult(
                targetClass = RequisitionGroupMembersDto.class,
                columns = {
                    @ColumnResult(name = "requisitionGroupId",
                        type = UUID.class),
                    @ColumnResult(name = "facilityId",
                        type = UUID.class),
                    @ColumnResult(name = "programId",
                        type = UUID.class)
                }
            )
        }
    )
})
public class RequisitionGroupMembers implements Serializable {
  @Column
  @Id
  private UUID requisitionGroupId;

  @Column
  @Id
  private UUID facilityId;

  @Transient
  private UUID programId;
}
