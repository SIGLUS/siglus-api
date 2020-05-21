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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.FACILITY_TYPE;
import static org.openlmis.referencedata.web.dummy.DummyTransferObject.MANDATORY_STRING_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.web.csv.model.ImportField;
import org.openlmis.referencedata.web.csv.model.ModelField;
import org.openlmis.referencedata.web.dummy.DummyTransferObject;

public class ModelFieldTest {

  private List<ModelField> result = new ArrayList<>();

  @Before
  public void beforeEach() {
    List<java.lang.reflect.Field> fieldsList =
        Arrays.asList(DummyTransferObject.class.getDeclaredFields());
    for (java.lang.reflect.Field field : fieldsList) {
      if (field.isAnnotationPresent(ImportField.class)) {
        result.add(new ModelField(field, field.getAnnotation(ImportField.class)));
      }
    }
  }

  @Test
  public void shouldCheckIfHasName() {
    assertTrue(result.get(0).hasName(MANDATORY_STRING_FIELD.toLowerCase()));
    assertTrue(result.get(0).hasName(MANDATORY_STRING_FIELD.toUpperCase()));
    assertFalse(result.get(0).hasName(MANDATORY_STRING_FIELD + "a"));
    assertFalse(result.get(0).hasName("a"));
  }

  @Test
  public void shouldCheckIfHasType() {
    assertTrue(result.get(1).hasType(FACILITY_TYPE.toLowerCase()));
    assertTrue(result.get(1).hasType(FACILITY_TYPE.toUpperCase()));
    assertFalse(result.get(1).hasType(FACILITY_TYPE + "a"));
    assertFalse(result.get(1).hasType("a"));
  }
}