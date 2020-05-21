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

package org.openlmis.requisition.service;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.RequisitionTemplate.ORDER_RELATED_COLUMNS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_DEFINED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_FOUND;

import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionTemplateServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @InjectMocks
  private RequisitionTemplateService requisitionTemplateService;

  private UUID programId = UUID.randomUUID();
  private UUID facilityTypeId = UUID.randomUUID();
  private RequisitionTemplate template = new RequisitionTemplateDataBuilder()
      .withAllColumns()
      .withAdditionalQuantityRequiredColumnDisplayed()
      .build();

  @Test
  public void findTemplateShouldReturnTemplate() {
    when(requisitionTemplateRepository.findTemplate(programId, facilityTypeId))
        .thenReturn(template);

    RequisitionTemplate found = requisitionTemplateService.findTemplate(
        programId, facilityTypeId, false
    );

    assertThat(found, is(template));
  }
  
  @Test
  public void findTemplateShouldReturnModifiedTemplateWhenReportOnlyIsTrue() {
    //given
    when(requisitionTemplateRepository.findTemplate(programId, facilityTypeId))
        .thenReturn(template);

    //when
    RequisitionTemplate found = requisitionTemplateService.findTemplate(
        programId, facilityTypeId, true
    );

    //then
    for (String columnName : ORDER_RELATED_COLUMNS) {
      try {
        assertNotEquals(
            found.findColumn(columnName).getIsDisplayed(),
            template.findColumn(columnName).getIsDisplayed());
      } catch (ValidationMessageException vme) {
        assertTrue(vme.getMessage().contains("requisition.error.columnNotInTemplate"));
      }
    }
  }

  @Test
  public void shouldThrowExceptionIfTemplateDoesNotExist() {
    exception.expect(ContentNotFoundMessageException.class);
    exception.expectMessage(containsString(ERROR_REQUISITION_TEMPLATE_NOT_FOUND));

    when(requisitionTemplateRepository.findTemplate(programId, facilityTypeId))
        .thenReturn(null);

    requisitionTemplateService.findTemplate(programId, facilityTypeId, false);
  }

  @Test
  public void shouldThrowExceptionIfTemplateNotDefined() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_REQUISITION_TEMPLATE_NOT_DEFINED));

    when(requisitionTemplateRepository.findTemplate(programId, facilityTypeId))
        .thenReturn(new RequisitionTemplateDataBuilder().build());

    requisitionTemplateService.findTemplate(programId, facilityTypeId, false);
  }
}
