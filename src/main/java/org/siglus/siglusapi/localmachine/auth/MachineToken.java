/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.localmachine.auth;

import static org.siglus.siglusapi.constant.FieldConstants.DOT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MachineToken {
  private static final String ALG_HEADER = "alg";
  private UUID machineId;
  private UUID facilityId;
  private String payload;

  public static MachineToken sign(UUID agentId, UUID facilityId, byte[] privateKey) {
    Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
    Date expiration =
        Date.from(LocalDateTime.now().plusHours(12).atZone(ZoneId.systemDefault()).toInstant());

    Map<String, Object> header = new HashMap<>();
    header.put(ALG_HEADER, SignatureAlgorithm.RS256.getValue());
    String jws =
        Jwts.builder()
            .setHeader(header)
            .setIssuer(agentId.toString())
            .setSubject(facilityId.toString())
            .setExpiration(expiration)
            .setIssuedAt(now)
            .signWith(getDecodedPrivateKey(privateKey), SignatureAlgorithm.RS256)
            .compact();
    return new MachineToken(agentId, facilityId, jws);
  }

  public static MachineToken parse(String token) {
    String[] splitToken = token.split("\\.");
    String unsignedToken = splitToken[0] + DOT + splitToken[1] + DOT;
    Claims claims = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken).getBody();
    return new MachineToken(
        UUID.fromString(claims.getIssuer()), UUID.fromString(claims.getSubject()), token);
  }

  private static PrivateKey getDecodedPrivateKey(byte[] privateKey) {
    try {
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKey);
      KeyFactory kf = KeyFactory.getInstance(SignatureAlgorithm.RS256.getFamilyName());
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalStateException("privateKey parse fail", e);
    }
  }

  private static PublicKey getDecodedPublicKey(byte[] publicKey) {
    try {
      X509EncodedKeySpec x509PublicKey = new X509EncodedKeySpec(publicKey);
      KeyFactory kf = KeyFactory.getInstance(SignatureAlgorithm.RS256.getFamilyName());
      return kf.generatePublic(x509PublicKey);
    } catch (Exception e) {
      throw new IllegalStateException("publicKey parse fail", e);
    }
  }

  public boolean verify(byte[] publicKey) {
    Claims claims =
        Jwts.parserBuilder()
            .setSigningKey(getDecodedPublicKey(publicKey))
            .build()
            .parseClaimsJws(payload)
            .getBody();
    return claims.getExpiration().after(new Date());
  }
}
