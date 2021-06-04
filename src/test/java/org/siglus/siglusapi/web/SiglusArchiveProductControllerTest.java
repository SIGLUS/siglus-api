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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.SiglusArchiveProductService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusArchiveProductControllerTest {

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @InjectMocks
  private SiglusArchiveProductController siglusArchiveProductController;

  private UUID facilityId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldCallSiglusArchiveProductServiceWhenArchiveProduct() {
    siglusArchiveProductController.archiveProduct(orderableId);

    verify(archiveProductService).archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldCallSiglusArchiveProductServiceWhenActivateProduct() {
    siglusArchiveProductController.activateProduct(orderableId);

    verify(archiveProductService).activateProduct(facilityId, orderableId);
  }

  @Test
  public void shouldCallSiglusArchiveProductServiceWhenSearchArchivedProducts() {
    siglusArchiveProductController.searchArchivedProducts(facilityId);

    verify(archiveProductService).searchArchivedProductsByFacilityId(facilityId);
  }
}
