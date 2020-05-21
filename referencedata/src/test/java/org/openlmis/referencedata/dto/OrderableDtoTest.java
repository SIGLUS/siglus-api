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

package org.openlmis.referencedata.dto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.repository.OrderableRepository;

public class OrderableDtoTest {

  private OrderableDto orderableDto;

  @Before
  public void setUp() {
    orderableDto = new OrderableDto();
  }
  
  @Test
  public void getVersionNumberShouldGetInitialVersionIfRepositoryIsNotSet() {
    assertEquals(1L, orderableDto.getVersionNumber().longValue());
  }
  
  @Test
  public void getVersionNumberShouldGetLatestVersionFromRepositoryIfSet() {
    //given
    Orderable orderable = mock(Orderable.class);
    OrderableRepository orderableRepository = mock(OrderableRepository.class);
    when(orderableRepository.findFirstByIdentityIdOrderByIdentityVersionNumberDesc(any(UUID.class)))
        .thenReturn(orderable);
    when(orderable.getVersionNumber()).thenReturn(2L);
    orderableDto.setOrderableRepository(orderableRepository);

    //then
    assertEquals(2L, orderableDto.getVersionNumber().longValue());
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(OrderableDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    OrderableDto dto = new OrderableDto();
    ToStringTestUtils.verify(OrderableDto.class, dto);
  }
}
