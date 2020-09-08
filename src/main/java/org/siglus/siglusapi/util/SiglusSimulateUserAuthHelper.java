package org.siglus.siglusapi.util;

import com.google.common.collect.ImmutableMap;
import org.siglus.siglusapi.security.CustomUserAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.siglus.siglusapi.security.CustomUserAuthenticationConverter.REFERENCE_DATA_USER_ID;

@Component
public class SiglusSimulateUserAuthHelper {

  public void simulateUserAuth(UUID userId) {
    UserAuthenticationConverter userAuthenticationConverter = new CustomUserAuthenticationConverter();
    Authentication authentication = userAuthenticationConverter.extractAuthentication(
        ImmutableMap.of(REFERENCE_DATA_USER_ID, userId.toString()));

    OAuth2Authentication orignAuth = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();
    OAuth2Authentication newAuth = new OAuth2Authentication(orignAuth.getOAuth2Request(),
        authentication);

    SecurityContextHolder
        .getContext().setAuthentication(newAuth);
  }
}
