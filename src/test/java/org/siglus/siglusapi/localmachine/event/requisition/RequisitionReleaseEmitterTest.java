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

package org.siglus.siglusapi.localmachine.event.requisition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEvent;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class RequisitionReleaseEmitterTest {

  @InjectMocks
  private RequisitionReleaseEmitter requisitionReleaseEmitter;

  @Mock
  private EventPublisher eventPublisher;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private EventCommonService eventCommonService;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  private final UUID requisitionId = UUID.randomUUID();
  private final UUID supplyingDepotId = UUID.randomUUID();

  @Test
  public void shouldGetOrderReleaseEmitSuccessfully() {
    // given
    UUID authorId = UUID.randomUUID();
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId)).thenReturn(new RequisitionExtension());
    when(requisitionRepository.findOne(requisitionId)).thenReturn(new Requisition());
    when(eventCommonService.getGroupId(any())).thenReturn("groupId");
    when(siglusRequisitionExtensionService.formatRequisitionNumber(any(UUID.class)))
        .thenReturn("requisitionNumber");

    // when
    RequisitionReleaseEvent emit = requisitionReleaseEmitter.emit(mockReleasableRequisitionDto(), authorId);

    // then
    assertThat(emit.getAuthorId()).isEqualTo(authorId);
  }

  private ReleasableRequisitionDto mockReleasableRequisitionDto() {
    ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
    releasableRequisitionDto.setRequisitionId(requisitionId);
    releasableRequisitionDto.setSupplyingDepotId(supplyingDepotId);
    return releasableRequisitionDto;
  }

}
