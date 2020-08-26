package org.siglus.siglusapi.service.fc;

import static org.junit.Assert.*;

import org.junit.Test;

public class CallFcServiceTest {

  @Test
  public void test() {
    String url = "http://fc.cmam.gov.mz:8095/issueVoucher/issuevouchers?key=2020082403&psize=20&date=20200501&page=1";
    String[] split = url.split("psize=20&");
    System.out.println(split[1]);
  }

}