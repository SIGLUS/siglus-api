package org.siglus.siglusapi.web.request;

import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;

public enum OperateTypeEnum {
  SAVE,
  SUBMIT;

  public static PodSubDraftStatusEnum getPodSubDraftEnum(OperateTypeEnum operateEnum) {
    switch (operateEnum) {
      case SAVE:
        return PodSubDraftStatusEnum.DRAFT;
      case SUBMIT:
      default:
        return PodSubDraftStatusEnum.SUBMITTED;
    }
  }
}
