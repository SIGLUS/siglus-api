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

package org.openlmis.referencedata.validate;

import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_INVALID;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_MISSING;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_MISSING_MANDATORY_COLUMNS;
import static org.openlmis.referencedata.web.dummy.DummyTransferObject.MANDATORY_STRING_FIELD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.dummy.DummyTransferObject;

@RunWith(MockitoJUnitRunner.class)
public class CsvHeaderValidatorTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @InjectMocks
  private CsvHeaderValidator csvHeaderValidator;

  @Test
  public void shouldNotThrowExceptionWhileValidatingHeadersWithMismatchCase() {
    List<String> headers = Arrays.asList("MANDAtory String Field", "mandatoryIntFIELD");

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    csvHeaderValidator.validateHeaders(headers, modelClass, false);
  }

  @Test
  public void shouldThrowExceptionIfHeaderDoesNotHaveCorrespondingFieldInModelWhenNotAcceptExtra() {
    List<String> headers =
        Arrays.asList("not existing field", MANDATORY_STRING_FIELD, "mandatoryIntField");

    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_UPLOAD_HEADER_INVALID, "[not existing field]").toString());

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    csvHeaderValidator.validateHeaders(headers, modelClass, false);
  }

  @Test
  public void shouldNotThrowExceptionIfHeaderDoesNotHaveCorrespondingFieldInModelWhenAcceptExtra() {
    List<String> headers =
        Arrays.asList("not existing field", MANDATORY_STRING_FIELD, "mandatoryIntField");

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    csvHeaderValidator.validateHeaders(headers, modelClass, true);
  }

  @Test
  public void shouldThrowExceptionIfHeaderIsNull() {
    List<String> headers =
        Arrays.asList(MANDATORY_STRING_FIELD, null, "mandatoryIntField");

    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_UPLOAD_HEADER_MISSING, "2").toString());

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    csvHeaderValidator.validateHeaders(headers, modelClass, false);
  }


  @Test
  public void shouldThrowExceptionIfMissingMandatoryHeaders() {
    List<String> headers = Collections.singletonList("optionalStringField");

    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_UPLOAD_MISSING_MANDATORY_COLUMNS,
            "[Mandatory String Field, mandatoryIntField]").toString());

    ModelClass modelClass = new ModelClass(DummyTransferObject.class);
    csvHeaderValidator.validateHeaders(headers, modelClass, false);
  }

}
