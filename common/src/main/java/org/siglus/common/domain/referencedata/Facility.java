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

package org.siglus.common.domain.referencedata;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.dto.referencedata.NamedResource;

@Entity
@NoArgsConstructor
@TypeName("Facility")
@Table(name = "facilities", schema = "referencedata")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NamedNativeQueries({
    @NamedNativeQuery(name = "Facility.findSupervisionFacilitiesByUser",
        query = "SELECT DISTINCT f.id" 
            + "   , f.name"
            + " FROM referencedata.facilities f"
            + "   JOIN referencedata.right_assignments ra ON ra.facilityid = f.id"
            + " WHERE ra.programid IS NOT NULL"
            + "   AND ra.userid = :userId",
        resultSetMapping = "Facility.namedResource")
    })
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "Facility.namedResource",
        classes = {
            @ConstructorResult(
                targetClass = NamedResource.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "name", type = String.class)
                }
            )
        }
    )
    })
public class Facility extends BaseEntity implements FhirLocation {

  public static final String TEXT = "text";

  @Column(nullable = false, unique = true, columnDefinition = TEXT)
  @Getter
  @Setter
  private String code;

  @Column(columnDefinition = TEXT)
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = TEXT)
  @Getter
  @Setter
  private String description;

  @ManyToOne
  @JoinColumn(name = "geographiczoneid", nullable = false)
  @Getter
  @Setter
  private GeographicZone geographicZone;

  @ManyToOne
  @JoinColumn(name = "typeid", nullable = false)
  @Getter
  @Setter
  private FacilityType type;

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean active;

  @Getter
  @Setter
  private LocalDate goLiveDate;

  @Getter
  @Setter
  private LocalDate goDownDate;

  @Column(columnDefinition = TEXT)
  @Getter
  @Setter
  private String comment;

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean enabled;

  @Getter
  @Setter
  private Boolean openLmisAccessible;

  @OneToMany(mappedBy = "facilityProgram.facility", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.LAZY)
  @DiffIgnore
  @Getter
  @Setter
  private Set<SupportedProgram> supportedPrograms = new HashSet<>();

  @Column(columnDefinition = "Geometry")
  @DiffIgnore
  @Getter
  @Setter
  private Point location;

  @Embedded
  private ExtraDataEntity extraData = new ExtraDataEntity();

  /**
   * Equal by a Facility's code.
   *
   * @param other the other Facility
   * @return true if the two Facilities' {@link Code} are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Facility)) {
      return false;
    }

    Facility facility = (Facility) other;
    return Objects.equals(code, facility.getCode());
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    if (null != location) {
      out.writeObject(JTSGeometryJavaTypeDescriptor.INSTANCE.toString(location));
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    String locationAsString = (String) in.readObject();
    if (null != locationAsString) {
      Geometry geometry = JTSGeometryJavaTypeDescriptor.INSTANCE.fromString(locationAsString);
      location = JTSGeometryJavaTypeDescriptor.INSTANCE.unwrap(geometry, Point.class, null);
    }
  }

  /**
   * Check to see if this facility supports the specified program.
   */
  public boolean supports(Program program) {
    return supportedPrograms
        .stream()
        .anyMatch(supported -> supported.isActiveFor(program));
  }

  public void setExtraData(Map<String, Object> extraData) {
    this.extraData = ExtraDataEntity.defaultEntity(this.extraData);
    this.extraData.updateFrom(extraData);
  }

  @Override
  public Map<String, Object> getExtraData() {
    return ExtraDataEntity.defaultEntity(extraData).getExtraData();
  }

  public interface Importer extends FhirLocation {

    String getCode();

    String getName();

    String getDescription();

    GeographicZone.Importer getGeographicZone();

    FacilityType.Importer getType();

    Boolean getActive();

    LocalDate getGoLiveDate();

    LocalDate getGoDownDate();

    String getComment();

    Boolean getEnabled();

    Boolean getOpenLmisAccessible();
    
    Point getLocation();
    
  }
}
