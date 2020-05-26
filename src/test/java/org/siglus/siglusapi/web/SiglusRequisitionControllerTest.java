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

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.web.RequisitionController;

public class SiglusRequisitionControllerTest {

  @Mock
  private RequisitionController requisitionController;

  @InjectMocks
  private SiglusRequisitionController siglusRequisitionController;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  private UUID uuid;

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    uuid = UUID.randomUUID();
  }

  @Test
  public void shouldCallOpenlmisControllerWhenSubmitRequisition() {
    siglusRequisitionController.submitRequisition(uuid, request, response);

    verify(requisitionController).submitRequisition(uuid, request, response);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenAuthorizeRequisition() {
    siglusRequisitionController.authorizeRequisition(uuid, request, response);

    verify(requisitionController).authorizeRequisition(uuid, request, response);
  }

}