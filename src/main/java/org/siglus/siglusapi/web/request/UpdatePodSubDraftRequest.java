package org.siglus.siglusapi.web.request;

import javax.validation.constraints.NotNull;
import lombok.Data;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;

@Data
public class UpdatePodSubDraftRequest {

  @NotNull
  private ProofOfDeliveryDto podDto;

  @NotNull
  private OperateTypeEnum operateType;
}
