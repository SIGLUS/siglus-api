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

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.javers.core.metamodel.annotation.TypeName;
import org.siglus.common.domain.referencedata.VersionIdentity;
import org.siglus.common.domain.referencedata.Versionable;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;

@Entity
@Table(name = "orderables", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(name = "unq_productcode_versionid",
        columnNames = {"code", "versionnumber"}))
@NamedNativeQueries({
    @NamedNativeQuery(name = "OrderableExpirationDate.findExpirationDate",
        query = "SELECT o.id as orderableId, MIN(lots.expirationDate) as expirationDate "
            + " FROM referencedata.orderables o "
            + " INNER JOIN referencedata.orderable_identifiers oi "
            + " ON o.id = oi.orderableid "
            + " INNER JOIN referencedata.lots "
            + " ON oi.value = CAST(lots.tradeitemid AS varchar)"
            + " WHERE lots.expirationDate >= now()"
            + " AND o.id IN :ids"
            + " GROUP BY o.id ",
        resultSetMapping = "OrderableExpirationDate.OrderableExpirationDateDto")
})
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "OrderableExpirationDate.OrderableExpirationDateDto",
        classes = {
            @ConstructorResult(
                targetClass = OrderableExpirationDateDto.class,
                columns = {
                    @ColumnResult(name = "orderableId",
                        type = UUID.class),
                    @ColumnResult(name = "expirationDate",
                        type = LocalDate.class)
                }
            )
        }
    )
})
public class OrderableExpirationDate implements Versionable {

  @EmbeddedId
  private VersionIdentity identity;

  @Override
  public Long getVersionNumber() {
    return identity.getVersionNumber();
  }

  @Override
  public UUID getId() {
    return identity.getId();
  }

}
