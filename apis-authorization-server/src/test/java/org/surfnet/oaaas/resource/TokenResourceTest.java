/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.surfnet.oaaas.resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.surfnet.oaaas.auth.AbstractAuthenticator;
import org.surfnet.oaaas.auth.AbstractUserConsentHandler;
import org.surfnet.oaaas.auth.OAuth2Validator;
import org.surfnet.oaaas.auth.principal.AuthenticatedPrincipal;
import org.surfnet.oaaas.model.AccessToken;
import org.surfnet.oaaas.model.AuthorizationRequest;
import org.surfnet.oaaas.model.Client;
import org.surfnet.oaaas.repository.AccessTokenRepository;
import org.surfnet.oaaas.repository.AuthorizationRequestRepository;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TokenResourceTest {

  @InjectMocks
  private TokenResource tokenResource;

  @Mock
  private HttpServletRequest request;

  @Mock
  private AuthorizationRequestRepository authorizationRequestRepository;

  @Mock
  private OAuth2Validator oAuth2Validator;

  @Mock
  private AccessTokenRepository accessTokenRepository;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testPrincipalDisplayName() {
    AuthorizationRequest authRequest = createAuthRequest(OAuth2Validator.IMPLICIT_GRANT_RESPONSE_TYPE);
    authRequest.getClient().setIncludePrincipal(true);

    AccessToken accessToken = createAccessToken();

    when(authorizationRequestRepository.findByAuthState("auth_state")).thenReturn(authRequest);
    when(request.getAttribute(AbstractAuthenticator.AUTH_STATE)).thenReturn("auth_state");
    when(request.getAttribute(AbstractUserConsentHandler.GRANTED_SCOPES)).thenReturn(accessToken.getScopes().toArray());
    when(accessTokenRepository.save((AccessToken) any())).thenReturn(accessToken);

    URI uri = (URI) tokenResource.authorizeCallback(request).getMetadata().get("Location").get(0);


    long expiresIn = 1800;
    assertEquals("http://localhost:8080#access_token=ABCDEF&token_type=bearer&expires_in=" + expiresIn + "&scope=read,write&state=important&principal=sammy%20sammy", uri.toString());
    assertTrue(uri.getFragment().endsWith("principal=" + authRequest.getPrincipal().getDisplayName()));
  }

  private AccessToken createAccessToken() {
    AccessToken token = new AccessToken();
    token.setToken("ABCDEF");
    token.setExpires(System.currentTimeMillis() + 1800 * 1000);
    token.setScopes(Arrays.asList("read","write"));
    return token;
  }


  private AuthorizationRequest createAuthRequest(String implicitGrantResponseType) {
    AuthorizationRequest authRequest = new AuthorizationRequest();
    Client client = new Client();
    authRequest.setClient(client);
    authRequest.setResponseType(implicitGrantResponseType);
    authRequest.setPrincipal(new AuthenticatedPrincipal("sammy sammy"));
    authRequest.setRedirectUri("http://localhost:8080");
    authRequest.setState("important");
    return authRequest;
  }
}
