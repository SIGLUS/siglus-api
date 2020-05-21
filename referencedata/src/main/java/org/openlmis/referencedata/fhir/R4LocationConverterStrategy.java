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

package org.openlmis.referencedata.fhir;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.FhirMessageKeys;

@AllArgsConstructor
class R4LocationConverterStrategy implements LocationConverterStrategy<Location> {

  private final IGenericClient client;
  private final CacheControlDirective cacheControlDirective;
  private final CriterionBuilder criterionBuilder;

  @Override
  public Location initiateResource() {
    return new Location();
  }

  @Override
  public void setName(Location resource, FhirLocation input) {
    resource.setName(input.getName());
  }

  @Override
  public void setPhysicalType(Location resource, FhirLocation input) {
    CodeableConcept physicalType = new CodeableConcept();
    input
        .getPhysicalType()
        .getCoding()
        .stream()
        .map(elem -> {
          Coding coding = new Coding();
          coding.setSystem(elem.getSystem());
          coding.setCode(elem.getCode());
          coding.setDisplay(elem.getDisplay());

          return coding;
        })
        .forEach(physicalType::addCoding);

    resource.setPhysicalType(physicalType);
  }

  @Override
  public void setPartOf(Location resource, FhirLocation input) {
    FhirReference reference = input.getPartOf();

    if (null == reference) {
      return;
    }

    UUID resourceId = reference.getResourceId();

    Bundle bundle = client
        .search()
        .forResource(Location.class)
        .cacheControl(cacheControlDirective)
        .where(criterionBuilder.buildIdentifierCriterion(resourceId))
        .returnBundle(Bundle.class)
        .execute();

    List<BundleEntryComponent> entries = bundle.getEntry();
    String url = CollectionUtils.isEmpty(entries) ? null : entries.get(0).getFullUrl();

    if (StringUtils.isBlank(url)) {
      throw new ValidationMessageException(
          new Message(FhirMessageKeys.ERROR_NOT_FOUND_LOCATION_FOR_RESOURCE, resourceId));
    }

    resource.setPartOf(new Reference(url));
  }

  @Override
  public void setIdentifier(Location resource, FhirLocation input) {
    input
        .getIdentifier()
        .stream()
        .map(elem -> {
          Identifier identifier = new Identifier();
          identifier.setSystem(elem.getSystem());
          identifier.setValue(elem.getValue());

          return identifier;
        })
        .forEach(resource::addIdentifier);
  }

  @Override
  public void addSystemIdentifier(Location resource, String system, UUID value) {
    Identifier identifier = new Identifier();
    identifier.setSystem(system);
    identifier.setValue(value.toString());

    resource.addIdentifier(identifier);
  }

  @Override
  public void setAlias(Location resource, FhirLocation input) {
    input
        .getAlias()
        .forEach(resource::addAlias);
  }

  @Override
  public void setPosition(Location resource, FhirLocation input) {
    Optional
        .ofNullable(input.getPosition())
        .ifPresent(position -> {
          LocationPositionComponent positionComponent = new LocationPositionComponent();
          positionComponent.setLatitude(new BigDecimal(position.getLatitude()));
          positionComponent.setLongitude(new BigDecimal(position.getLongitude()));

          resource.setPosition(positionComponent);
        });
  }

  @Override
  public void setDescription(Location resource, FhirLocation input) {
    resource.setDescription(input.getDescription());
  }

  @Override
  public void setStatus(Location resource, FhirLocation input) {
    Optional
        .ofNullable(input.getStatus())
        .ifPresent(status -> {
          try {
            resource.setStatus(LocationStatus.fromCode(status));
          } catch (FHIRException exp) {
            throw new IllegalStateException(exp);
          }
        });
  }

}
