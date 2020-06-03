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

package org.openlmis.fulfillment.web.validator;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.i18n.MessageService;
import org.openlmis.fulfillment.util.Message;
import org.openlmis.fulfillment.web.util.FileColumnDto;
import org.openlmis.fulfillment.web.util.FileTemplateDto;
import org.springframework.validation.Errors;

@RunWith(MockitoJUnitRunner.class)
public class FileTemplateValidatorTest {

  private static final String ORDERABLE_ID = "orderableId";
  private static final String ORDER_CODE = "orderCode";
  private static final String QUANTITY_SHIPPED = "quantityShipped";
  private static final String ORDER_ID = "orderId";

  @Mock
  Errors errors;

  @Mock
  MessageService messageService;

  @InjectMocks
  private FileTemplateValidator validator;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private FileTemplateDto fileTemplate;

  @Before
  public void setup() {
    fileTemplate = new FileTemplateDto();
    fileTemplate.setTemplateType(TemplateType.SHIPMENT);

    Message message = new Message("key");
    Message.LocalizedMessage localizedMessage = message.new LocalizedMessage("Localized");
    when(messageService.localize(any())).thenReturn(localizedMessage);

    doNothing().when(errors).reject(anyString(), any(), anyString());
  }

  @Test
  public void shipmentValidationDoesNotAffectOrderTemplate() {
    fileTemplate.setTemplateType(TemplateType.ORDER);

    validator.validate(fileTemplate, errors);

    verify(errors, never()).reject(any(), any(), any());
  }

  @Test
  public void validationSucceedsWhenAllRequiredFieldsArePresent() {
    fileTemplate.setFileColumns(
        asList(
            createColumn(ORDER_CODE),
            createColumn(ORDERABLE_ID),
            createColumn(QUANTITY_SHIPPED))
    );

    validator.validate(fileTemplate, errors);

    verify(errors, never()).reject(any(), any(), any());
  }

  @Test
  public void validationFailsWhenDuplicateColumnFound() {
    fileTemplate.setFileColumns(
        asList(
            createColumn(ORDER_CODE),
            createColumn(ORDER_ID),
            createColumn(ORDERABLE_ID),
            createColumn(QUANTITY_SHIPPED))
    );

    validator.validate(fileTemplate, errors);

    verify(errors).reject(any(), any(), any());
  }

  @Test
  public void validationFailsWhenOrderableColumnIsNotPresent() {
    fileTemplate.setFileColumns(
        asList(createColumn(ORDER_CODE), createColumn(QUANTITY_SHIPPED)));

    validator.validate(fileTemplate, errors);

    verify(errors).reject(any(), any(), any());
  }

  @Test
  public void validationFailsWhenOrderIdentifierColumnIsNotPresent() {
    fileTemplate.setFileColumns(
        asList(createColumn(ORDERABLE_ID), createColumn(QUANTITY_SHIPPED)));

    validator.validate(fileTemplate, errors);

    verify(errors).reject(any(), any(), any());
  }

  @Test
  public void validationFailsWhenQuantityShippedColumnIsNotPresent() {
    fileTemplate.setFileColumns(
        asList(createColumn(ORDERABLE_ID), createColumn(ORDER_CODE)));

    validator.validate(fileTemplate, errors);

    verify(errors).reject(any(), any(), any());
  }

  private FileColumnDto createColumn(String keyPath) {
    FileColumnDto columnDto = new FileColumnDto();
    columnDto.setKeyPath(keyPath);
    return columnDto;
  }
}