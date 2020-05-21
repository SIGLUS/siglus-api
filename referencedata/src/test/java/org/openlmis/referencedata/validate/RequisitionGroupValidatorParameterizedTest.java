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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.openlmis.referencedata.validate.RequisitionGroupValidator.CODE;
import static org.openlmis.referencedata.validate.RequisitionGroupValidator.NAME;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.RequisitionGroupDto;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.util.messagekeys.RequisitionGroupMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@RunWith(Parameterized.class)
public class RequisitionGroupValidatorParameterizedTest {

  @Mock
  private SupervisoryNodeRepository supervisoryNodes;

  @InjectMocks
  private Validator validator = new RequisitionGroupValidator();

  private String emptyValue;
  private RequisitionGroupDto requisitionGroupDto;
  private Errors errors;

  /**
   * Create new instance of parametrized test.
   *
   * @param value expected value for code or name property of {@link RequisitionGroup}
   */
  public RequisitionGroupValidatorParameterizedTest(String value) {
    emptyValue = value;

    RequisitionGroup requisitionGroup = new RequisitionGroup();
    requisitionGroup.setCode("TestRequisitionGroupCode");
    requisitionGroup.setName("TestRequisitionGroupName");

    requisitionGroupDto = new RequisitionGroupDto();
    requisitionGroup.export(requisitionGroupDto);

    errors = new BeanPropertyBindingResult(requisitionGroupDto, "requisitionGroup");
  }

  /**
   * This method is used to pass arguments from each array into constructor.
   *
   * @return list of array that contains data that will be passed into constructor.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null},
        {""},
        {"  "}
    });

  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(mock(SupervisoryNode.class))
        .when(supervisoryNodes)
        .findByCode("TestSupervisoryNodeCode");
  }

  @Test
  public void shouldRejectIfCodeIsEmpty() throws Exception {
    requisitionGroupDto.setCode(emptyValue);

    validator.validate(requisitionGroupDto, errors);

    assertErrorMessage(errors, CODE, RequisitionGroupMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfNameIsEmpty() throws Exception {
    requisitionGroupDto.setName(emptyValue);

    validator.validate(requisitionGroupDto, errors);

    assertErrorMessage(errors, NAME, RequisitionGroupMessageKeys.ERROR_NAME_REQUIRED);
  }

}
