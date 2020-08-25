package org.siglus.siglusapi.dto.fc;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {

  private String reference;

  private Date expiryDate;

  private int packSize;

  private int numberOfPallets;

  private String fnmCode;

  private int orderedQuantity;

  private int quantityPerPallet;

  private int shippedQuantity;

  private String batch;

  private String productDescription;

  private int approvedQuantity;
}
