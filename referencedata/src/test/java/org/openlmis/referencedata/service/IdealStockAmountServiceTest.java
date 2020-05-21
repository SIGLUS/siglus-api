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

package org.openlmis.referencedata.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.repository.IdealStockAmountRepository;

@RunWith(MockitoJUnitRunner.class)
public class IdealStockAmountServiceTest {

  @Mock
  private IdealStockAmountRepository repository;

  @InjectMocks
  private IdealStockAmountService service;

  @Mock
  private IdealStockAmount isa;

  private UUID isaId = UUID.randomUUID();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(isa.getId()).thenReturn(isaId);
  }

  @Test
  public void shouldCallRepositorySearch() {
    when(repository.search(null))
        .thenReturn(Collections.singletonList(isaId));
    when(repository.findAll(any(Iterable.class)))
        .thenReturn(Collections.singletonList(isa));

    assertEquals(Collections.singletonList(isa), service.search());
    verify(repository).search(null);
    verify(repository).findAll(any(Iterable.class));
  }

  @Test
  public void shouldCallRepositorySearchWithListParameter() {
    when(repository.search(Collections.singletonList(isa)))
        .thenReturn(Collections.singletonList(isaId));
    when(repository.findAll(any(Iterable.class)))
        .thenReturn(Collections.singletonList(isa));

    assertEquals(Collections.singletonList(isa), service.search(Collections.singletonList(isa)));
    verify(repository).search(Collections.singletonList(isa));
    verify(repository).findAll(any(Iterable.class));
  }
}
