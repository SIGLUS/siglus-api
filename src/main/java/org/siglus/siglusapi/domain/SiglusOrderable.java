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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.OrderableChild;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.openlmis.referencedata.domain.Versionable;
import org.openlmis.referencedata.domain.measurement.TemperatureMeasurement;
import org.openlmis.referencedata.domain.measurement.VolumeMeasurement;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;


// TODO: duplicate with SiglusOrderable.java in referencedata
//  but can't import OrderableExpirationDateDto
/**
 * Products that are SiglusOrderable by Program. An SiglusOrderable represent any
 * medical commodities that maybe ordered/requisitioned, typically by a {@link Program}.
 */
@Entity
@Table(name = "orderables", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(name = "unq_productcode_versionid",
        columnNames = {"code", "versionnumber"}))
@NamedNativeQueries({
    @NamedNativeQuery(name = "SiglusOrderable.findExpirationDate",
        query = "SELECT o.id as orderableId, MIN(lots.expirationDate) as expirationDate "
            + " FROM referencedata.orderables o "
            + " INNER JOIN referencedata.orderable_identifiers oi "
            + " ON o.id = oi.orderableid "
            + " INNER JOIN referencedata.lots "
            + " ON oi.value = CAST(lots.tradeitemid AS varchar)"
            + " WHERE lots.expirationDate >= now()"
            + " AND o.id IN :ids"
            + " GROUP BY o.id ",
        resultSetMapping = "SiglusOrderable.OrderableExpirationDateDto")
})
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "SiglusOrderable.OrderableExpirationDateDto",
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
@NoArgsConstructor
@Cacheable
@Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(attributeNodes = {
    @NamedAttributeNode("programOrderables"),
    @NamedAttributeNode("children"),
    @NamedAttributeNode("identifiers")
})
public class SiglusOrderable implements Versionable {

  private static final int FETCH_SIZE = 1000;

  public static final String TRADE_ITEM = "tradeItem";
  public static final String VALUE = "value";

  @Embedded
  @Getter
  private Code productCode;

  @Getter
  @Setter
  private String fullProductName;

  @Getter
  @Setter
  private String description;

  @Getter(AccessLevel.PACKAGE)
  private long netContent;

  @Getter(AccessLevel.PACKAGE)
  private long packRoundingThreshold;

  @Getter(AccessLevel.PACKAGE)
  private boolean roundToZero;

  @ElementCollection
  @MapKeyColumn(name = "key")
  @BatchSize(size = FETCH_SIZE)
  @Column(name = VALUE)
  @CollectionTable(
      name = "orderable_identifiers",
      schema = "referencedata",
      joinColumns = {
          @JoinColumn(name = "orderableId", referencedColumnName = "id"),
          @JoinColumn(name = "orderableVersionNumber", referencedColumnName = "versionNumber")})
  @Setter
  private Map<String, String> identifiers;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = FETCH_SIZE)
  @Getter
  @Setter
  @Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
  private List<ProgramOrderable> programOrderables;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = FETCH_SIZE)
  @Setter
  @Getter
  @Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
  private Set<OrderableChild> children;

  @EmbeddedId
  private VersionIdentity identity;

  @Getter
  @Setter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = VALUE, column = @Column(
          name = "minimumTemperatureValue")),
      @AttributeOverride(name = "temperatureMeasurementUnitCode", column = @Column(
          name = "minimumTemperatureCode"))
  })
  private TemperatureMeasurement minimumTemperature;

  @Getter
  @Setter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = VALUE, column = @Column(
          name = "maximumTemperatureValue")),
      @AttributeOverride(name = "temperatureMeasurementUnitCode", column = @Column(
          name = "maximumTemperatureCode"))
  })
  private TemperatureMeasurement maximumTemperature;

  @Getter
  @Setter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = VALUE, column = @Column(
          name = "inBoxCubeDimensionValue")),
      @AttributeOverride(name = "measurementUnitCode", column = @Column(
          name = "inBoxCubeDimensionCode"))
  })
  private VolumeMeasurement inBoxCubeDimension;

  @Override
  public UUID getId() {
    return identity.getId();
  }

  public void setId(UUID id) {
    identity.setId(id);
  }

  @Override
  public Long getVersionNumber() {
    return identity.getVersionNumber();
  }

  public String getTradeItemIdentifier() {
    return identifiers.get(TRADE_ITEM);
  }

  /**
   * Default constructor.
   *
   * @param productCode product code
   * @param netContent net content
   * @param packRoundingThreshold pack rounding threshold
   * @param roundToZero round to zero
   * @param id id
   * @param versionNumber version number
   */
  public SiglusOrderable(Code productCode, long netContent,
      long packRoundingThreshold, boolean roundToZero, UUID id, Long versionNumber) {
    this.productCode = productCode;
    this.netContent = netContent;
    this.packRoundingThreshold = packRoundingThreshold;
    this.roundToZero = roundToZero;
    this.identity = new VersionIdentity(id, versionNumber);
  }

  /**
   * Determines equality based on product codes.
   *
   * @param object another SiglusOrderable, ideally.
   * @return true if the two are semantically equal.  False otherwise.
   */
  @Override
  public final boolean equals(Object object) {
    return null != object
        && object instanceof SiglusOrderable
        && Objects.equals(productCode, ((SiglusOrderable) object).productCode);
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(productCode);
  }

}
