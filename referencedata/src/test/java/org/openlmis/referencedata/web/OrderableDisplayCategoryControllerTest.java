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

package org.openlmis.referencedata.web;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.referencedata.domain.OrderedDisplayValue;
import org.openlmis.referencedata.dto.OrderableDisplayCategoryDto;
import org.openlmis.referencedata.repository.OrderableDisplayCategoryRepository;
import org.openlmis.referencedata.service.RightService;

@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OrderableDisplayCategoryControllerTest {

  @Mock
  private OrderableDisplayCategoryRepository repository;

  @Mock
  private RightService rightService;

  @InjectMocks
  private OrderableDisplayCategoryController controller;

  private OrderableDisplayCategory categoryA;
  private Code codeA;
  private OrderedDisplayValue displayA;
  private OrderableDisplayCategory categoryB;
  private Code codeB;
  private OrderedDisplayValue displayB;

  /**
   * Constructor for test.
   */
  public OrderableDisplayCategoryControllerTest() {
    initMocks(this);

    codeA = Code.code("A");
    displayA = new OrderedDisplayValue("A-Analgesics", 1);
    categoryA = OrderableDisplayCategory.createNew(codeA, displayA);
    codeB = Code.code("B");
    displayB = new OrderedDisplayValue("B-Bandages", 2);
    categoryB = OrderableDisplayCategory.createNew(codeB, displayB);
  }

  @Before
  public void setup() {
  }

  private void preparePostOrPut() {
    when(repository.findAll()).thenReturn(
        Sets.newHashSet(categoryA, categoryB)
    );
  }

  @Test
  public void shouldGetAllOrderableDisplayCategories() {
    //given
    Set<OrderableDisplayCategoryDto> expected = Sets.newHashSet(
        OrderableDisplayCategoryDto.newInstance(categoryA),
        OrderableDisplayCategoryDto.newInstance(categoryB));
    preparePostOrPut();

    //when
    Iterable<OrderableDisplayCategoryDto> categories =
        controller.getAllOrderableDisplayCategories();

    //then
    assertTrue(expected.equals(categories));
  }
}
