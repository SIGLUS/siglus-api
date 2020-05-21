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

package org.openlmis.referencedata.domain;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.domain.BaseEntity.BaseExporter;
import org.openlmis.referencedata.domain.BaseEntity.BaseImporter;
import org.openlmis.referencedata.domain.VersionIdentity.VersionExporter;
import org.openlmis.referencedata.domain.VersionIdentity.VersionImporter;

@Entity
@Table(name = "facility_type_approved_products", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(name = "unq_ftap",
        columnNames = {"orderableId", "programId", "facilityTypeId"}))
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@TypeName("FacilityTypeApprovedProduct")
@ToString
public class FacilityTypeApprovedProduct implements Versionable {

  @EmbeddedId
  private VersionIdentity identity;

  @Column(nullable = false)
  @Getter
  @Setter
  private UUID orderableId;

  @ManyToOne
  @JoinColumn(name = "programId", nullable = false)
  @Getter
  @Setter
  private Program program;

  @ManyToOne
  @JoinColumn(name = "facilityTypeId", nullable = false)
  @Getter
  @Setter
  private FacilityType facilityType;

  @Column(nullable = false)
  @Getter
  @Setter
  private Double maxPeriodsOfStock;

  @Getter
  @Setter
  private Double minPeriodsOfStock;

  @Getter
  @Setter
  private Double emergencyOrderPoint;

  @Column(nullable = false, columnDefinition = "boolean DEFAULT true")
  @Getter
  @Setter
  private Boolean active;

  @Getter
  @Setter
  private ZonedDateTime lastUpdated;

  /**
   * A minimal constructor with only required fields.
   *
   * @param id - the resource's UUID. The random UUID will be generated if the null value was
   *             passed.
   * @param versionNumber - the resource's version number. The 1 value will be used if the null
   *                        value was passed.
   * @param orderableId - the {@link Orderable} UUID.
   * @param program - the {@link Program} instance.
   * @param facilityType - The {@link FacilityType} instance.
   * @param maxPeriodsOfStock - Maximum periods of stock for this resource.
   * @param active - true if this resource should be active; otherwise false. The true value will be
   *                 used if the null value was passed.
   */
  public FacilityTypeApprovedProduct(UUID id, Long versionNumber, UUID orderableId,
      Program program, FacilityType facilityType, Double maxPeriodsOfStock, Boolean active) {
    this.identity = new VersionIdentity(id, versionNumber);
    this.orderableId = orderableId;
    this.program = program;
    this.facilityType = facilityType;
    this.maxPeriodsOfStock = maxPeriodsOfStock;
    this.active = toBooleanDefaultIfNull(active, true);
  }

  /**
   * Creates new FacilityTypeApprovedProduct based on data from {@link
   * Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of FacilityTypeApprovedProduct.
   */
  public static FacilityTypeApprovedProduct newFacilityTypeApprovedProduct(Importer importer) {
    FacilityTypeApprovedProduct ftap = new FacilityTypeApprovedProduct(
        importer.getId(), importer.getVersionNumber(), importer.getOrderableId(),
        null, null, importer.getMaxPeriodsOfStock(), importer.getActive()
    );
    ftap.setMinPeriodsOfStock(importer.getMinPeriodsOfStock());
    ftap.setEmergencyOrderPoint(importer.getEmergencyOrderPoint());

    return ftap;
  }

  @Override
  public UUID getId() {
    return identity.getId();
  }

  @Override
  public Long getVersionNumber() {
    return identity.getVersionNumber();
  }

  @PrePersist
  @PreUpdate
  public void updateLastUpdatedDate() {
    lastUpdated = ZonedDateTime.now();
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(identity.getId());
    exporter.setMaxPeriodsOfStock(maxPeriodsOfStock);
    exporter.setMinPeriodsOfStock(minPeriodsOfStock);
    exporter.setEmergencyOrderPoint(emergencyOrderPoint);
    exporter.setProgram(program);
    exporter.setFacilityType(facilityType);
    exporter.setActive(active);
    exporter.setVersionNumber(identity.getVersionNumber());
    exporter.setLastUpdated(lastUpdated);
  }

  public interface Exporter extends BaseExporter, VersionExporter {

    void setMaxPeriodsOfStock(Double maxPeriodsOfStock);

    void setMinPeriodsOfStock(Double minPeriodsOfStock);

    void setEmergencyOrderPoint(Double emergencyOrderPoint);

    void setProgram(Program program);

    void setFacilityType(FacilityType facilityType);

    void setActive(Boolean active);

    void setLastUpdated(ZonedDateTime lastUpdated);
  }

  public interface Importer extends BaseImporter, VersionImporter {

    Double getMaxPeriodsOfStock();

    Double getMinPeriodsOfStock();

    Double getEmergencyOrderPoint();

    UUID getOrderableId();

    Program.Importer getProgram();

    FacilityType.Importer getFacilityType();

    Boolean getActive();
  }
}
