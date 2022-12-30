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

package org.siglus.siglusapi.localmachine.event.requisition.reject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.localmachine.event.requisition.web.approve.RequisitionInternalApproveReplayer;
import org.siglus.siglusapi.localmachine.event.requisition.web.reject.RequisitionRejectEvent;
import org.siglus.siglusapi.localmachine.event.requisition.web.reject.RequisitionRejectReplayer;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.web.android.FileBasedTest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class RequisitionRejectReplayerTest extends FileBasedTest {

  @InjectMocks
  private RequisitionRejectReplayer requisitionRejectReplayer;
  @Mock
  private RequisitionRepository requisitionRepository;
  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;
  @Mock
  private EventCommonService eventCommonService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;
  @Mock
  private SiglusRequisitionService siglusRequisitionService;
  @Mock
  private RequisitionInternalApproveReplayer requisitionInternalApproveReplayer;

  private final UUID userId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccess() {
    // given
    final RequisitionRejectEvent event = new RequisitionRejectEvent();
    event.setUserId(userId);
    event.setRequisitionNumber("RNR-001");
    Requisition requisition = RequisitionBuilder.newRequisition(UUID.randomUUID(), UUID.randomUUID(), true);
    requisition.setId(UUID.randomUUID());
    requisition.setTemplate(new RequisitionTemplate());
    requisition.setRequisitionLineItems(new ArrayList<>());
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(new RequisitionExtension());
    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(requisition);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(any(UUID.class), any(UUID.class)))
        .thenReturn(new SupervisoryNodeDto());

    // when
    requisitionRejectReplayer.replay(event);
  }
}
