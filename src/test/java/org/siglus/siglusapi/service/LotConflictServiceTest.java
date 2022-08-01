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

package org.siglus.siglusapi.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.LotConflict;
import org.siglus.siglusapi.repository.LotConflictRepository;

@RunWith(MockitoJUnitRunner.class)
public class LotConflictServiceTest extends TestCase {

  @InjectMocks
  private LotConflictService lotConflictService;

  @Mock
  private LotConflictRepository repo;

  private final UUID facilityId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final String lotCode = "lotCode";

  @Test
  public void shouldSuccessWhenHandleConflict() {
    // given
    when(repo.findOneByFacilityIdAndLotIdAndLotCodeAndExpirationDate(any(), any(), any(), any())).thenReturn(null);
    when(repo.save(any(LotConflict.class))).thenReturn(new LotConflict());

    // when
    lotConflictService.handleLotConflict(facilityId, lotCode, lotId, LocalDate.now(), LocalDate.now().plusDays(1));

    // then
    verify(repo).save(any(LotConflict.class));
  }

  @Test
  public void shouldDoNothingWhenHandleConflictWithSameExpirationDate() {
    // given
    // when
    LocalDate localDate = LocalDate.now();
    lotConflictService.handleLotConflict(facilityId, lotCode, lotId, localDate, localDate);
  }

  @Test
  public void shouldDoNotSaveWhenHandleConflictWithExistedConflict() {
    // given
    when(repo.findOneByFacilityIdAndLotIdAndLotCodeAndExpirationDate(any(), any(), any(), any())).thenReturn(
        new LotConflict());

    // when
    lotConflictService.handleLotConflict(facilityId, lotCode, lotId, LocalDate.now(), LocalDate.now().plusDays(1));

    // then
    verify(repo, times(0)).save(any(LotConflict.class));
  }
}