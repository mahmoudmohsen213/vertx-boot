package com.vertxboot.vault.repository;

import io.vertx.config.vault.client.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VaultClient {
    public static final String TOKEN_HEADER = "X-Vault-Token";
    private static final String NULL_TOKEN_ERROR_MESSAGE = "The token must not be null";
    private static final String VAULT_ACCESS_FAILED_ERROR_MESSAGE = "Unable to access vault";

    private final WebClient client;

    public VaultClient(Vertx vertx, JsonObject config) {
        String host = config.getString("host");
        Integer port = config.getInteger("port", 8200);

        Objects.requireNonNull(host, "The Vault host must be set");

        client = WebClient.create(vertx, new WebClientOptions(config)
                .setDefaultPort(port).setDefaultHost(host)
        );
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public void read(String token, String path, Handler<AsyncResult<Secret>> responseHandler) {
        Objects.requireNonNull(responseHandler);

        client.get("/v1/" + Objects.requireNonNull(path))
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE))
                .send(response -> {
                    if (response.failed()) {
                        responseHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, response.cause()));
                        return;
                    }

                    HttpResponse<Buffer> result = response.result();
                    if (result.statusCode() != 200) {
                        responseHandler.handle(VaultException.toFailure(result.statusMessage(), result.statusCode(),
                                result.bodyAsString()));
                    } else {
                        Secret secret = result.bodyAsJson(Secret.class);
                        responseHandler.handle(Future.succeededFuture(secret));
                    }
                });
    }

    public void write(String token, String path, JsonObject secrets, Handler<AsyncResult<Secret>> resultHandler) {
        Objects.requireNonNull(resultHandler);
        client.post("/v1/" + Objects.requireNonNull(path))
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE))
                .sendJsonObject(Objects.requireNonNull(secrets, "The secret must be set"),
                        ar -> {
                            if (ar.failed()) {
                                resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                                return;
                            }

                            HttpResponse<Buffer> response = ar.result();
                            if (response.statusCode() == 200 || response.statusCode() == 204) {
                                resultHandler.handle(Future.succeededFuture(response.bodyAsJson(Secret.class)));
                            } else {
                                resultHandler.handle(VaultException.toFailure(response.statusMessage(), response.statusCode(),
                                        response.bodyAsString()));
                            }
                        });
    }

    public void list(String token, String path, Handler<AsyncResult<List<String>>> resultHandler) {
        Objects.requireNonNull(path, "The path is required to list secrets");
        String fullPath = path + "?list=true";
        Objects.requireNonNull(resultHandler);

        read(token, fullPath, ar -> {
            if (ar.failed() && !(ar.cause() instanceof VaultException)) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
            } else if (ar.failed()) {
                if (((VaultException) ar.cause()).getStatusCode() == 404) {
                    resultHandler.handle(Future.succeededFuture(Collections.emptyList()));
                } else {
                    resultHandler.handle(Future.failedFuture(ar.cause()));
                }
            } else {
                JsonArray keys = ar.result().getData().getJsonArray("keys");
                if (keys == null) {
                    resultHandler.handle(Future.failedFuture("Cannot find keys"));
                } else {
                    List<String> list = new ArrayList<>();
                    keys.forEach(o -> list.add((String) o));
                    resultHandler.handle(Future.succeededFuture(list));
                }
            }
        });
    }

    public void delete(String token, String path, Handler<AsyncResult<Void>> resultHandler) {
        Objects.requireNonNull(resultHandler);
        client.delete("/v1/" + Objects.requireNonNull(path))
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE))
                .send(ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }

                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() != 204) {
                        resultHandler.handle(VaultException.toFailure(response.statusMessage(), response.statusCode(),
                                response.bodyAsString()));
                    } else {
                        resultHandler.handle(Future.succeededFuture());
                    }
                });
    }

    public void createToken(String token, TokenRequest tokenRequest, Handler<AsyncResult<Auth>> resultHandler) {
        client.post("/v1/auth/token/create" + ((tokenRequest.getRole() == null) ? "" : "/" + tokenRequest.getRole()))
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE))
                .sendJsonObject(tokenRequest.toPayload(), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }
                    manageAuthResult(resultHandler, ar.result());
                });
    }

    private void manageAuthResult(Handler<AsyncResult<Auth>> resultHandler, HttpResponse<Buffer> response) {
        if (response.statusCode() != 200) {
            resultHandler.handle(VaultException.toFailure(response.statusMessage(), response.statusCode(),
                    response.bodyAsString()));
        } else {
            JsonObject object = response.bodyAsJsonObject();
            Auth auth = object.getJsonObject("auth").mapTo(Auth.class);
            resultHandler.handle(Future.succeededFuture(auth));
        }
    }

    public void loginWithAppRole(String roleId, String secretId, Handler<AsyncResult<Auth>>
            resultHandler) {
        JsonObject payload = new JsonObject()
                .put("role_id", Objects.requireNonNull(roleId, "The role must not be null"))
                .put("secret_id", Objects.requireNonNull(secretId, "The secret must not be null"));

        client.post("/v1/auth/approle/login")
                .sendJsonObject(payload, ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }

                    manageAuthResult(resultHandler, ar.result());
                });
    }

    public void loginWithUserCredentials(String username, String password, Handler<AsyncResult<Auth>>
            resultHandler) {
        JsonObject payload = new JsonObject()
                .put("password", Objects.requireNonNull(password, "The password must not be null"));

        client.post("/v1/auth/userpass/login/" + Objects.requireNonNull(username, "The username must not be null"))
                .sendJsonObject(payload, ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }

                    manageAuthResult(resultHandler, ar.result());
                });
    }

    public void loginWithCert(Handler<AsyncResult<Auth>> resultHandler) {
        client.post("/v1/auth/cert/login")
                .send(ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }

                    manageAuthResult(resultHandler, ar.result());
                });
    }

    public void renewSelf(String token, long leaseDurationInSecond, Handler<AsyncResult<Auth>> resultHandler) {
        JsonObject payload = null;
        if (leaseDurationInSecond > 0) {
            payload = new JsonObject().put("increment", leaseDurationInSecond);
        }
        HttpRequest<Buffer> request = client.post("/v1/auth/token/renew-self")
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE));

        Handler<AsyncResult<HttpResponse<Buffer>>> handler = ar -> {
            if (ar.failed()) {
                resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                return;
            }
            manageAuthResult(resultHandler, ar.result());
        };

        if (payload != null) {
            request.sendJsonObject(payload, handler);
        } else {
            request.send(handler);
        }
    }

    public void lookupSelf(String token, Handler<AsyncResult<Lookup>> resultHandler) {
        client.get("/v1/auth/token/lookup-self")
                .putHeader(TOKEN_HEADER, Objects.requireNonNull(token, NULL_TOKEN_ERROR_MESSAGE))
                .send(ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(VaultException.toFailure(VAULT_ACCESS_FAILED_ERROR_MESSAGE, ar.cause()));
                        return;
                    }
                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() != 200) {
                        resultHandler.handle(VaultException.toFailure(response.statusMessage(), response.statusCode(),
                                response.bodyAsString()));
                    } else {
                        JsonObject object = response.bodyAsJsonObject();
                        Lookup lookup = object.getJsonObject("data").mapTo(Lookup.class);
                        resultHandler.handle(Future.succeededFuture(lookup));
                    }
                });
    }
}
