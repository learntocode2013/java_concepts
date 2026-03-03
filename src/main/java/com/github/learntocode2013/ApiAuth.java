package com.github.learntocode2013;

import com.twilio.client.account.v3a.ahc.AccountClientAhc;
import com.twilio.client.auth.api.v5a.ApiAuthorizationClient;
import com.twilio.client.auth.api.v5a.ApiAuthorizationClientImpl;
import com.twilio.client.auth.edge.v1.EdgeAuthClient;
import com.twilio.client.auth.edge.v1.EdgeAuthClientImpl;
import com.twilio.client.auth.scoped.v1a.ScopedAuthenticationClient;
import com.twilio.client.auth.scoped.v1a.impl.ScopedAuthenticationClientImpl;
import com.twilio.client.iam.accounts.v1.IamAccountsClient;
import com.twilio.client.iam.authz.v1.IamAuthorizationClient;
import com.twilio.client.iam.authz.v1.IamAuthorizationClientImpl;
import com.twilio.core.client.base.response.Response;
import com.twilio.core.routing.URIServiceRouteResolver;
import com.twilio.domain.account.v3.Account;
import com.twilio.domain.auth.edge.v1.EdgeAuthorizationRequest;
import com.twilio.domain.auth.edge.v1.EdgeAuthorizationRequest.Builder;
import com.twilio.domain.auth.edge.v1.EdgeAuthorizationResponse;
import com.twilio.domain.auth.edge.v1.RequestInfo;
import com.twilio.domain.auth.scoped.v1.ScopedAuthenticationRequest;
import com.twilio.domain.auth.scoped.v1.ScopedResponse;
import com.twilio.domain.auth.scoped.v1.grants.payload.FreeformGrantPayload;
import com.twilio.domain.iam.permission.v1.IamPermission;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.sids.AccountSid;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class ApiAuth {

  public static void main(String[] args) throws Exception {
//    demoUsingAccountSidInHeader();
    demoUsingAccessToken();
//    demoEdgeServiceClient();
//    fetchAccountInfo();
  }

  private static void demoEdgeServiceClient() throws ExecutionException, InterruptedException {
    EdgeAuthClient authClient = EdgeAuthClientImpl.builder()
        .connectionTimeout(Duration.ofMinutes(1))
        .requestTimeout(Duration.ofMinutes(1))
        .routeResolver(
            new URIServiceRouteResolver("http://ec2-3-85-140-45.compute-1.amazonaws.com:7746"))
        .build();
    EdgeAuthorizationRequest request = new Builder()
        .requestInfo(new RequestInfo.Builder().addHeader("Authorization",
            "Basic QUM2MjNhOWYzNDA2MzdlNjkyOGJkOGZjMzU2YmQ5MDQyNDplMjRhNTg4OGE1MjhmMGZkMDQxNWYzNDRmODViMGU3OQ==").build()).build();
    Response<EdgeAuthorizationResponse> response = authClient.authorize(
        request).callRaw().toCompletableFuture().get();
    System.out.println(response.getPayload().get());
  }

  //https://www.twilio.com/docs/iam/access-tokens
  //id-scoped-auth-service
  //ec2-54-174-204-142.compute-1.amazonaws.com
  private static void demoUsingAccessToken() throws ExecutionException, InterruptedException {
    AccessToken token = generateAccessToken();
    processRequestAfterTokenValidation(token);
  }

  private static void processRequestAfterTokenValidation(AccessToken accessToken) {
    ScopedAuthenticationClient authenticationClient = ScopedAuthenticationClientImpl.builder()
        .connectionTimeout(Duration.ofMinutes(1))
        .requestTimeout(Duration.ofMinutes(1))
        .routeResolver(new URIServiceRouteResolver("http://ec2-54-174-204-142.compute-1.amazonaws.com:7755"))
        .build();
    authenticationClient.authenticate(
            new ScopedAuthenticationRequest(accessToken.toJwt()))
        .callRaw()
        .whenComplete((scopedAuthenticationResponse,error) -> {
          if (Objects.nonNull(error)) {
            return;
          }
          boolean isValid = scopedAuthenticationResponse.getPayload().map(
              ScopedResponse::isAuthorized).orElse(false);
          boolean isFromAuthorizedSource = scopedAuthenticationResponse.getPayload()
              .map(payload -> payload.getGrantPayload(FreeformGrantPayload.class)
                  .getGrants().findValue("identity")
              ).map(identityValue -> identityValue.asText().equals("oneAdmin"))
              .orElse(false);
          if(isValid && isFromAuthorizedSource) {
            //do something useful
            System.out.println("Doing something useful since token is valid");
          } else {
            System.out.println("You are not allowed to change domain gateway config");
          }
        });
  }

  private static AccessToken generateAccessToken() {
    String twilioAccountSid = System.getenv("ACCOUNT_SID");
    String twilioApiKey = System.getenv("API_SID");
    String twilioApiSecret = System.getenv("API_SECRET");
    var accessToken = new AccessToken.Builder(twilioAccountSid, twilioApiKey, twilioApiSecret)
        .identity("oneAdmin")
        .build();
    System.out.println(accessToken.toJwt());
    return accessToken;
  }

  private static void demoUsingAccountSidInHeader() {
    String authToken = System.getenv("AUTH_TOKEN");
    AccountSid accountSid = AccountSid.parse(System.getenv("ACCOUNT_SID"));
    ApiAuthorizationClient client = ApiAuthorizationClientImpl.builder().requestTimeout(
            Duration.ofMinutes(1))
        .build();

    CompletionStage<String> cf = client.authorize(accountSid, authToken)
        .call()
        .thenApply(resp -> {
          if (resp.isAuthorized()) {
            System.out.println("You are authorized to make the API call");
            System.out.println(resp.getAuthorization());
            return "Pass";
          } else {
            System.err.println("You are not authorized to make the API call");
            return "Fail";
          }
        }).exceptionally(error -> {
          System.err.println("Failed due to: " + error);
          return "Fail";
        });
//    System.out.println("Before exit...." + cf.toCompletableFuture().get());
  }

  private static void fetchAccountInfo() throws ExecutionException, InterruptedException {
    AccountClientAhc clientAhc = new AccountClientAhc.Builder()
        .requestTimeout(Duration.ofMinutes(1))
        .connectionTimeout(Duration.ofMinutes(1))
        .routeResolver(
            new URIServiceRouteResolver("http://ec2-54-172-187-74.compute-1.amazonaws.com:9650"))
        .build();
    Response<Optional<Account>> response = clientAhc.get(
            AccountSid.parse(System.getenv("ACCOUNT_SID")))
        .addMetadata("I-Twilio-Auth-Account", System.getenv("ACCOUNT_SID"))
        .callRaw()
        .toCompletableFuture()
        .get();
    response.getPayload().ifPresent(maybeAcc -> {
      var acc = maybeAcc.orElse(null);
      System.out.println(acc);
    });
  }

  private static void demoAccessControlService() {
    
  }

}
