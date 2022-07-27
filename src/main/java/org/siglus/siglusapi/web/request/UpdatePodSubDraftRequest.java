package org.siglus.siglusapi.web.request;

import lombok.Data;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftEnum;

@Data
public class UpdatePodSubDraftRequest {

  private ProofOfDeliveryDto proofOfDeliveryDto;

  private PodSubDraftEnum subDraftStatus;
}
