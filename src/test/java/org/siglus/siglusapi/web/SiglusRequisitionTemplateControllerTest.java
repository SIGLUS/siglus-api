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
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.openlmis.requisition.web.RequisitionTemplateController;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.service.SiglusRequisitionTemplateService;
import org.springframework.validation.BindingResult;


@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionTemplateControllerTest {
  @Mock
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;

  @Mock
  private RequisitionTemplateController requisitionTemplateController;

  @InjectMocks
  private SiglusRequisitionTemplateController siglusRequisitionTemplateController;

  @Mock
  private BindingResult bindingResult;

  @Mock
  private RequisitionTemplateDto updatedDto;

  @Mock
  private SiglusRequisitionTemplateDto requestDto;

  private UUID uuid;

  @Before
  public void prepare() {
    uuid = UUID.randomUUID();
  }

  @Test
  public void shouldCallSiglusServiceWhenSearchRequisitionTemplate() {
    siglusRequisitionTemplateController.searchRequisitionTemplate(uuid);

    verify(siglusRequisitionTemplateService).getTemplate(uuid);
  }

  @Test
  public void shouldCallOpenlmisControllerAndSiglusServiceWhenUpdateRequisitionTemplate() {
    when(requisitionTemplateController.updateRequisitionTemplate(uuid, requestDto, bindingResult))
        .thenReturn(updatedDto);

    siglusRequisitionTemplateController.updateRequisitionTemplate(uuid, requestDto, bindingResult);

    verify(requisitionTemplateController).updateRequisitionTemplate(uuid, requestDto,
        bindingResult);
    verify(siglusRequisitionTemplateService).updateTemplate(updatedDto, requestDto);
  }

  @Test
  public void shouldCallOpenlmisControllerAndSiglusServiceWhenCreateRequisitionTemplate() {
    when(requisitionTemplateController.createRequisitionTemplate(requestDto, bindingResult))
        .thenReturn(updatedDto);

    siglusRequisitionTemplateController.createRequisitionTemplate(requestDto, bindingResult);

    verify(requisitionTemplateController).createRequisitionTemplate(requestDto, bindingResult);
    verify(siglusRequisitionTemplateService).createTemplateExtension(updatedDto, requestDto);
  }

}