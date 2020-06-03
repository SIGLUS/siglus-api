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

package org.openlmis.fulfillment.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;

@Entity
@Table(name = "transfer_properties",
    uniqueConstraints = @UniqueConstraint(columnNames = {"facilityId", "transferType"}))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("abstract")
@NoArgsConstructor
public abstract class TransferProperties extends BaseEntity implements Storable {

  @Column(nullable = false)
  @Getter
  @Setter
  protected UUID facilityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter
  @Setter
  protected TransferType transferType = TransferType.ORDER;

  public interface BaseExporter {

    void setId(UUID id);

    void setFacility(FacilityDto facility);

    void setTransferType(TransferType transferType);

  }

  public interface BaseImporter {

    UUID getId();

    FacilityDto getFacility();

    TransferType getTransferType();

  }

}
