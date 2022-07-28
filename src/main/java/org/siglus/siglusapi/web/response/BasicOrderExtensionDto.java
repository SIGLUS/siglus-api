package org.siglus.siglusapi.web.response;

import lombok.Data;
import org.openlmis.fulfillment.web.util.BasicOrderDto;

@Data
public class BasicOrderExtensionDto extends BasicOrderDto {

  private boolean hasSubDraft;
}
