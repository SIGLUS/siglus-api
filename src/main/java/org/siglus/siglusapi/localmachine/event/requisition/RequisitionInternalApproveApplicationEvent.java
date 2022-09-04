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

import java.util.List;

import lombok.Data;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.localmachine.EventPayload;

@EventPayload
@Data
public class RequisitionInternalApproveApplicationEvent {

  private Requisition requisition;

  private RequisitionExtension requisitionExtension;

  private List<RequisitionLineItemExtension> lineItemExtensions;

  private List<AgeGroupLineItem> ageGroupLineItemRequisitionUsage;
  private List<ConsultationNumberLineItem> consultationNumberLineItemRequisitionUsage;
  private List<UsageInformationLineItem> usageInformationLineItemRequisitionUsage;
  private List<PatientLineItem> patientLineItemRequisitionUsage;
  private List<TestConsumptionLineItem> testConsumptionLineItemRequisitionUsage;
  private List<RegimenLineItem> regimenLineItemRequisitionUsage;
  private List<RegimenSummaryLineItem> regimenSummaryLineItemRequisitionUsage;
  List<KitUsageLineItem> kitUsageLineItemRequisitionUsage;
}
