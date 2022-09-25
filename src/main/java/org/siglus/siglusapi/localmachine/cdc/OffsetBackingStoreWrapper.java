package org.siglus.siglusapi.localmachine.cdc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OffsetBackingStoreWrapper implements InitializingBean {

  @Autowired
  private CdcOffsetBackingRepository cdcOffsetBackingRepository;

  private static CdcOffsetBackingRepository repository;

  @Override
  public void afterPropertiesSet() {
    repository = cdcOffsetBackingRepository;
  }

  public static CdcOffsetBackingRepository getRepository() {
    return repository;
  }
}
