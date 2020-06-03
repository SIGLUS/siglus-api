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

package org.openlmis.fulfillment.testutils;

import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class TestDtoDataBuilder {

  private static final String RESOURCE = "someResource";
  private static final String SERVICE_URL = "someService";
  private static final UUID ID = UUID.randomUUID();

  private UUID uuidProperty;
  private ExpandedObjectReferenceDto objRefDto = new ExpandedObjectReferenceDto();
  private String sampleProperty;

  public TestDtoDataBuilder withUuidProperty(UUID uuidProperty) {
    this.uuidProperty = uuidProperty;
    return this;
  }

  public TestDtoDataBuilder withObjRefDto(ExpandedObjectReferenceDto objRefDto) {
    this.objRefDto = objRefDto;
    return this;
  }

  public TestDtoDataBuilder withSampleProperty(String sampleProperty) {
    this.sampleProperty = sampleProperty;
    return this;
  }

  public TestDtoDataBuilder withHrefPropertyRemoved() {
    ReflectionTestUtils.setField(objRefDto, "href", null);
    return this;
  }

  /**
   * Builds TestDto with properties set and an object reference without expanded fields set.
   */
  public TestDto buildDtoWithObjectReferenceNotExpanded() {
    return this.withUuidProperty(UUID.randomUUID())
        .withObjRefDto(new ExpandedObjectReferenceDto(ID, SERVICE_URL, RESOURCE))
        .withSampleProperty("sampleProperty")
        .build();
  }

  /**
   * Builds TestDto with properties set and an object reference without any fields set.
   */
  public TestDto buildDtoWithEmptyObjectReference() {
    return this.withUuidProperty(UUID.randomUUID())
        .withObjRefDto(new ExpandedObjectReferenceDto(null, null, null))
        .withHrefPropertyRemoved()
        .withSampleProperty("sampleProperty")
        .build();
  }

  public TestDto build() {
    return new TestDto(uuidProperty, objRefDto, sampleProperty);
  }

}
