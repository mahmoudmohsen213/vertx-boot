package com.vertxboot.vault.repository;

import com.vertxboot.vault.entity.VaultEntityScope;
import io.vertx.config.vault.client.Secret;
import io.vertx.config.vault.client.VaultException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractVaultRepository<IdType, EntityType> {
    protected static final String DEFAULT_PATH_PREFIX = "secret";
    protected static final long DEFAULT_CHECKOUT_TIMEOUT = 3000;

    protected final Class<EntityType> entityClass;
    protected final Set<String> memberFieldNameSet;
    protected final Set<String> primitiveFieldNameSet;
    protected final VaultEntityScope scope;
    protected final String dataPath;
    protected final String metaDataPath;
    protected final SharedData sharedData;
    protected final VaultClient vaultClient;
    protected final Logger logger;
    protected String pathPrefix;
    protected long checkoutTimeout;

    @SuppressWarnings("unchecked")
    protected AbstractVaultRepository(VaultClient vaultClient, SharedData sharedData) {
        try {
            entityClass = this.getClass().getAnnotation(VaultRepository.class).entityClass();
        } catch (Exception e) {
            throw new IllegalStateException("Error capturing runtime entity type class", e);
        }

        try {
            dataPath = this.getClass().getAnnotation(VaultRepository.class).dataPath();
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing repository data path", e);
        }

        try {
            metaDataPath = this.getClass().getAnnotation(VaultRepository.class).metaDataPath();
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing repository metadata path", e);
        }

        try {
            scope = this.getClass().getAnnotation(VaultRepository.class).scope();
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing repository scope", e);
        }

        if (dataPath.startsWith("/") || dataPath.endsWith("/"))
            throw new IllegalStateException("Invalid repository data path");

        if (metaDataPath.startsWith("/") || metaDataPath.endsWith("/"))
            throw new IllegalStateException("Invalid repository metadata path");

        this.logger = LoggerFactory.getLogger(this.getClass());
        this.sharedData = sharedData;
        this.vaultClient = vaultClient;
        this.pathPrefix = DEFAULT_PATH_PREFIX;
        this.checkoutTimeout = DEFAULT_CHECKOUT_TIMEOUT;

        List<Field> fields = Arrays.asList(entityClass.getDeclaredFields());

        this.memberFieldNameSet = fields.stream()
                .map(Field::getName)
                .collect(Collectors.toCollection(HashSet::new));

        this.primitiveFieldNameSet = fields.stream()
                .filter(field -> field.getType().isPrimitive())
                .map(Field::getName)
                .collect(Collectors.toCollection(HashSet::new));

        this.logger.info("Repository initialized for: " + entityClass.getName() + " data path: " + dataPath);
    }

    protected AbstractVaultRepository(VaultClient vaultClient, SharedData sharedData, JsonObject config) {
        this(vaultClient, sharedData);
        this.pathPrefix = config.getString("pathPrefix", DEFAULT_PATH_PREFIX);
        this.checkoutTimeout = config.getLong("checkoutTimeout", DEFAULT_CHECKOUT_TIMEOUT);
    }

    protected abstract String buildEntityPath(String userId, IdType entityId);

    protected abstract String buildMetadataPath(String userId);

    protected Future<EntityType> findById(String vaultToken, String userId, IdType id) {
        return this.findByIdRaw(vaultToken, userId, id).map(entityJsonObject -> entityJsonObject.mapTo(entityClass));
    }

    protected Future<JsonObject> findByIdRaw(String vaultToken, String userId, IdType id) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.findByIdRaw: %s %s %s", vaultToken, userId, entityPath));
        Future<Secret> retrievedEntityFuture = Promise.<Secret>promise().future();
        vaultClient.read(vaultToken, entityPath, retrievedEntityFuture);
        return retrievedEntityFuture.map(Secret::getData);
    }

    protected Future<Lock> checkout(String vaultToken, String userId, IdType id) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.checkout: %s %s", userId, entityPath));
        Future<Lock> lockFuture = Promise.<Lock>promise().future();
        this.sharedData.getLockWithTimeout(vaultToken + entityPath, this.checkoutTimeout, lockFuture);
        return lockFuture;
    }

    protected Future<Boolean> lookup(String vaultToken, String userId, IdType id) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.lookup: %s %s %s", vaultToken, userId, entityPath));
        Promise<Boolean> lookupEntityPromise = Promise.promise();
        vaultClient.read(vaultToken, entityPath, secretAsyncResult -> {
            if (secretAsyncResult.failed() && (((VaultException) secretAsyncResult.cause()).getStatusCode() == 404))
                lookupEntityPromise.complete(false);
            else if (secretAsyncResult.failed())
                lookupEntityPromise.fail(secretAsyncResult.cause());
            else lookupEntityPromise.complete(true);
        });
        return lookupEntityPromise.future();
    }

    protected Future<EntityType> save(String vaultToken, String userId, IdType id, EntityType entity) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.save: %s %s %s", vaultToken, userId, entityPath));
        Future<Secret> savedEntityFuture = Promise.<Secret>promise().future();
        vaultClient.write(vaultToken, entityPath, JsonObject.mapFrom(entity), savedEntityFuture);
        return savedEntityFuture.map(secret -> entity);
    }

    protected Future<Void> delete(String vaultToken, String userId, IdType id) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.delete: %s %s %s", vaultToken, userId, entityPath));
        Future<Void> deleteEntityFuture = Promise.<Void>promise().future();
        vaultClient.delete(vaultToken, entityPath, deleteEntityFuture);
        return deleteEntityFuture;
    }

    protected Future<EntityType> put(String vaultToken, String userId, IdType id, String key, Object value) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.put: %s %s %s", vaultToken, userId, entityPath));

        if (!memberFieldNameSet.contains(key))
            return Future.failedFuture(new IllegalArgumentException("Unknown property name " + key));

        if ((value == null) && primitiveFieldNameSet.contains(key))
            return Future.failedFuture(new IllegalArgumentException("Non-nullable primitive property " + key));

        Future<Secret> retrievedEntityFuture = Promise.<Secret>promise().future();
        vaultClient.read(vaultToken, entityPath, retrievedEntityFuture);
        return retrievedEntityFuture.compose(entityRetrieveSecret -> {
            JsonObject updatedJsonObject = entityRetrieveSecret.getData().put(key, value);
            Future<Secret> savedEntityFuture = Promise.<Secret>promise().future();
            vaultClient.write(vaultToken, entityPath, updatedJsonObject, savedEntityFuture);
            return savedEntityFuture.map(secret -> updatedJsonObject.mapTo(entityClass));
        });
    }

    protected Future<EntityType> remove(String vaultToken, String userId, IdType id, String key) {
        String entityPath = this.buildEntityPath(userId, id);
        logger.debug(String.format("AbstractVaultRepository.remove: %s %s %s", vaultToken, userId, entityPath));

        if (!memberFieldNameSet.contains(key))
            return Future.failedFuture(new IllegalArgumentException("Unknown property name " + key));

        if (primitiveFieldNameSet.contains(key))
            return Future.failedFuture(new IllegalArgumentException("Non-removable primitive property " + key));

        Future<Secret> retrievedEntityFuture = Promise.<Secret>promise().future();
        vaultClient.read(vaultToken, entityPath, retrievedEntityFuture);
        return retrievedEntityFuture.compose(entityRetrieveSecret -> {
            JsonObject updatedJsonObject = entityRetrieveSecret.getData().putNull(key);
            Future<Secret> savedEntityFuture = Promise.<Secret>promise().future();
            vaultClient.write(vaultToken, entityPath, updatedJsonObject, savedEntityFuture);
            return savedEntityFuture.map(secret -> updatedJsonObject.mapTo(entityClass));
        });
    }
}
