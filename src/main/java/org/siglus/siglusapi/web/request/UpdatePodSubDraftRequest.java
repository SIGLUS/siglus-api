package org.siglus.siglusapi.web.request;

import java.util.UUID;
import lombok.Data;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;

@Data
public class UpdatePodSubDraftRequest {

  private ProofOfDeliveryDto proofOfDeliveryDto;

  private UUID subDraftId;
}
