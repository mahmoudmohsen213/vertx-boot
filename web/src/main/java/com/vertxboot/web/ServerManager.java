package com.vertxboot.web;

import com.vertxboot.beans.BeanConfig;
import com.vertxboot.commons.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerManager {
    protected static final String SERVER_PORT_CONFIG_KEY = "serverPort";
    protected static final String USE_SSL_CONFIG_KEY = "useSsl";

    protected Vertx vertx;
    protected Router router;
    protected HttpServer httpServer;

    protected ServerManager() {
    }

    public static Future<ServerManager> startServer(Vertx vertx, AppConfig config, InterceptorConfig interceptorConfig) {
        ServerManager serverManager = new ServerManager();
        JsonObject serverConfig = config.getSync()
                .getJsonObject("server", new JsonObject());

        HttpServerOptions httpServerOptions = new HttpServerOptions();
        if (serverConfig.getBoolean(USE_SSL_CONFIG_KEY, false)) {
            SelfSignedCertificate certificate = SelfSignedCertificate.create();
            httpServerOptions
                    .setSsl(true)
                    .setKeyCertOptions(certificate.keyCertOptions())
                    .setTrustOptions(certificate.trustOptions());
        }

        serverManager.vertx = vertx;
        serverManager.httpServer = serverManager.vertx.createHttpServer(httpServerOptions);
        serverManager.router = Router.router(serverManager.vertx);
        serverManager.router.route().handler(BodyHandler.create());
        RestLoader.load(serverManager.router, interceptorConfig);
        serverManager.httpServer.requestHandler(serverManager.router).listen(
                serverConfig.getInteger(SERVER_PORT_CONFIG_KEY, 8080));
        return Promise.succeededPromise(serverManager).future();
    }

    @BeanConfig(overridable = true)
    public static Future<ServerManager> serverManager(Vertx vertx, AppConfig appConfig, InterceptorConfig interceptorConfig) {
        return ServerManager.startServer(vertx, appConfig, interceptorConfig);
    }
}
