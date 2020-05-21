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

package org.openlmis.referencedata.web.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.openlmis.referencedata.web.dummy.DummyTransferObject.MANDATORY_STRING_FIELD;
import static org.openlmis.referencedata.web.dummy.DummyTransferObject.OPTIONAL_NESTED_FIELD;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.model.ModelField;
import org.openlmis.referencedata.web.dummy.DummyTransferObject;

public class ModelClassTest {

  @Test
  public void shouldGetFieldNameMappingsGivenTheHeader() {
    List<String> headers =
        Arrays.asList(MANDATORY_STRING_FIELD, "mandatoryIntField", OPTIONAL_NESTED_FIELD);

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    final String[] mappings = modelClass.getFieldNameMappings(
        headers.toArray(new String[headers.size()]));
    assertThat(mappings[0], is("mandatoryStringField"));
    assertThat(mappings[1], is("mandatoryIntField"));
    assertThat(mappings[2], is("dummyNestedField.code"));
  }

  @Test
  public void shouldFindImportFieldWithName() {
    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    ModelField importFieldWithName = modelClass.findImportFieldWithName(MANDATORY_STRING_FIELD);

    assertEquals(importFieldWithName.getName(), MANDATORY_STRING_FIELD);
    assertEquals(importFieldWithName.getType(), "String");
  }

}