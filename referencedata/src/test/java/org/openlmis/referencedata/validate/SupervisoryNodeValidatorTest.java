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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.testbuilder.RequisitionGroupDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

@RunWith(MockitoJUnitRunner.class)
public class SupervisoryNodeValidatorTest {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DC1 = "DC1";
  private static final String REQUISITION_GROUP = "requisitionGroup";

  @Mock
  private SupervisoryNodeRepository repository;

  @InjectMocks
  private Validator validator = new SupervisoryNodeValidator();

  private SupervisoryNodeDto dto;
  private BeanPropertyBindingResult errors;
  private RequisitionGroup requisitionGroup = new RequisitionGroupDataBuilder().build();

  @Before
  public void setUp() {
    dto = new SupervisoryNodeDto();
    errors = new BeanPropertyBindingResult(dto, "supervisoryNodeDto");
  }

  @Test
  public void shouldNotFindErrorsIfSupervisoryNodeIsValid() {
    new SupervisoryNodeDataBuilder()
        .withRequisitionGroup(requisitionGroup)
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectIfCodeIsNull() {
    new SupervisoryNodeDataBuilder()
        .withoutCode()
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertErrorMessage(errors, CODE, SupervisoryNodeMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfCodeIsDuplicated() {
    SupervisoryNode existing = new SupervisoryNodeDataBuilder()
        .withCode(DC1)
        .build();

    when(repository.findByCode(DC1)).thenReturn(existing);

    new SupervisoryNodeDataBuilder()
        .withCode(DC1)
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertErrorMessage(errors, CODE, SupervisoryNodeMessageKeys.ERROR_CODE_MUST_BE_UNIQUE);
  }

  @Test
  public void shouldAcceptIfRequisitionGroupIsMissingOnUpdateAndInSupervisoryNode() {
    SupervisoryNodeDataBuilder builder = new SupervisoryNodeDataBuilder()
            .withoutRequisitionGroup()
            .withCode(DC1);
    SupervisoryNode existing = builder.build();

    when(repository.findOne(existing.getId())).thenReturn(existing);
    when(repository.findByCode(DC1)).thenReturn(existing);

    builder
            .withCode("Updated Code")
            .withoutRequisitionGroup()
            .build()
            .export(dto);

    validator.validate(dto, errors);

    assertNull(existing.getRequisitionGroup());
    assertNull(dto.getRequisitionGroup());
    assertEquals(existing.getId(), dto.getId());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectIfRequisitionGroupIsMissingOnUpdateOnly() {
    SupervisoryNodeDataBuilder builder = new SupervisoryNodeDataBuilder()
            .withRequisitionGroup(requisitionGroup)
            .withCode(DC1);
    SupervisoryNode existing = builder.build();

    when(repository.findOne(existing.getId())).thenReturn(existing);
    when(repository.findByCode(DC1)).thenReturn(existing);

    builder
            .withCode("Updated Code")
            .withoutRequisitionGroup()
            .build()
            .export(dto);

    validator.validate(dto, errors);

    assertNotNull(existing.getRequisitionGroup());
    assertNull(dto.getRequisitionGroup());
    assertEquals(existing.getId(), dto.getId());
    assertErrorMessage(errors, REQUISITION_GROUP,
            SupervisoryNodeMessageKeys.ERROR_REQUISITION_GROUP_REQUIRED);
  }

  @Test
  public void shouldNotRejectIfRequisitionGroupIsMissingOnCreate() {
    new SupervisoryNodeDataBuilder()
        .withoutRequisitionGroup()
        .withoutId()
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotRejectIfUpdatingSupervisoryNode() {
    SupervisoryNodeDataBuilder builder = new SupervisoryNodeDataBuilder()
        .withRequisitionGroup(requisitionGroup)
        .withCode(DC1);
    SupervisoryNode existing = builder.build();

    when(repository.findOne(existing.getId())).thenReturn(existing);
    when(repository.findByCode(DC1)).thenReturn(existing);

    builder
        .withCode("Updated Code")
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectIfUpdatingRequisitionGroupInSupervisoryNode() {
    SupervisoryNodeDataBuilder builder = new SupervisoryNodeDataBuilder()
        .withRequisitionGroup(requisitionGroup);
    SupervisoryNode existing = builder.build();

    when(repository.findOne(existing.getId())).thenReturn(existing);

    builder
        .withRequisitionGroup(new RequisitionGroupDataBuilder().build())
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertEquals(existing.getId(), dto.getId());
    assertErrorMessage(errors, REQUISITION_GROUP,
        SupervisoryNodeMessageKeys.ERROR_UPDATING_REQUISITION_GROUP_SAVE_FAILED);
  }

  @Test
  public void shouldRejectIfNameIsNull() {
    new SupervisoryNodeDataBuilder()
        .withoutName()
        .build()
        .export(dto);

    validator.validate(dto, errors);

    assertErrorMessage(errors, NAME, SupervisoryNodeMessageKeys.ERROR_NAME_REQUIRED);
  }
}