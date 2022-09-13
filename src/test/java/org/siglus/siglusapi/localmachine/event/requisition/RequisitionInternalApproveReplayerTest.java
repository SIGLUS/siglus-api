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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.requisition.web.RequisitionInternalApproveReplayer;
import org.siglus.siglusapi.localmachine.event.requisition.web.RequisitionInternalApprovedEvent;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class RequisitionInternalApproveReplayerTest {
  @InjectMocks
  private RequisitionInternalApproveReplayer requisitionInternalApproveReplayer;
  @Mock
  private RequisitionRepository requisitionRepository;
  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;
  @Mock
  private RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  @Mock
  private AgeGroupLineItemRepository ageGroupLineItemRepository;
  @Mock
  private ConsultationNumberLineItemRepository consultationNumberLineItemRepository;
  @Mock
  private UsageInformationLineItemRepository usageInformationLineItemRepository;
  @Mock
  private PatientLineItemRepository patientLineItemRepository;
  @Mock
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;
  @Mock
  private RegimenLineItemRepository regimenLineItemRepository;
  @Mock
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  @Mock
  private KitUsageLineItemRepository kitUsageRepository;
  @Mock
  private EventPublisher eventPublisher;
  @Mock
  private RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  @Mock
  private RequisitionService requisitionService;
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccess() {
    // given
    final RequisitionInternalApprovedEvent event = new RequisitionInternalApprovedEvent();
    final List<RequisitionLineItem> lineItems = new ArrayList<>();
    final RequisitionLineItem lineItem1 = new RequisitionLineItem();
    lineItem1.setId(UUID.randomUUID());
    lineItem1.setTotalCost(Money.of(CurrencyUnit.USD, 0.0));
    final VersionEntityReference orderable1 = new VersionEntityReference();
    orderable1.setId(UUID.randomUUID());
    orderable1.setVersionNumber(1L);
    lineItem1.setOrderable(orderable1);
    final RequisitionLineItem lineItem2 = new RequisitionLineItem();
    lineItem2.setId(UUID.randomUUID());
    lineItem2.setTotalCost(Money.of(CurrencyUnit.USD, 100.0));
    final VersionEntityReference orderable2 = new VersionEntityReference();
    orderable2.setId(UUID.randomUUID());
    orderable2.setVersionNumber(1L);
    lineItem2.setOrderable(orderable1);
    lineItems.add(lineItem1);
    lineItems.add(lineItem2);
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setDraftStatusMessage("");
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(programId);
    requisition.setOriginalRequisitionId(null);
    requisition.setRequisitionLineItems(lineItems);
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    requisition.setEmergency(false);
    requisition.setReportOnly(false);

    event.setRequisition(requisition);
    event.setRequisitionExtension(new RequisitionExtension());
    event.setLineItemExtensions(new ArrayList<>());
    final ApproveProductsAggregator approveProductsAggregator =
        new ApproveProductsAggregator(new ArrayList<>(), programId);
    when(requisitionService.getApproveProduct(facilityId,
        programId, requisition.getReportOnly())).thenReturn(approveProductsAggregator);
    when(requisitionRepository.saveAndFlush(any())).thenReturn(requisition);
    // when
    requisitionInternalApproveReplayer.replay(event);
  }
}