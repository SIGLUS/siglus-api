package org.siglus.siglusapi.web.request;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class CreatePodSubDraftRequest {

  @NotNull
  private UUID orderId;

  @NotNull
  @Range(min = 1L, max = 10L)
  private Integer splitNum;
}
