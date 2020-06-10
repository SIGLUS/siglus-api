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

package org.siglus.siglusapi.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.utils.Pagination;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.domain.AvailableUsageColumnSection;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.dto.AvailableUsageColumnDto;
import org.siglus.siglusapi.repository.AvailableUsageColumnRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusAvailableUsageColumnControllerTest {
  @Mock
  private Pageable pageable;

  @Mock
  AvailableUsageColumnRepository repository;

  @InjectMocks
  SiglusAvailableUsageColumnController siglusAvailableUsageColumnController;

  @Test
  public void shouldGetRightColumnValue() {
    AvailableUsageColumn column = new AvailableUsageColumn();
    column.setSources("STOCK_CARDS|USER_INPUT");
    AvailableUsageColumnSection section = new AvailableUsageColumnSection();
    section.setLabel("sectionName");
    section.setCategory(UsageCategory.CONSULTATIONNUMBER);
    column.setAvailableUsageColumnSection(section);

    Page<AvailableUsageColumn> columnPage = Pagination.getPage(Arrays.asList(column));
    when(repository.findAll(pageable)).thenReturn(columnPage);
    Page<AvailableUsageColumnDto> availableUsageColumnDtos =
        siglusAvailableUsageColumnController.getAllColumns(pageable);
    AvailableUsageColumnDto dto = availableUsageColumnDtos.getContent().get(0);
    assertEquals(Arrays.asList("STOCK_CARDS","USER_INPUT"), dto.getSources());
    assertEquals("sectionName", dto.getSection().getLabel());
  }

}
