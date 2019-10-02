package com.vertxboot.vault.repository;

import com.vertxboot.vault.entity.VaultEntityScope;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import java.util.Objects;

public abstract class BaseMetaVaultRepository<EntityType> extends AbstractVaultRepository<String, EntityType> {
    protected BaseMetaVaultRepository(VaultClient vaultClient, SharedData sharedData) {
        super(vaultClient, sharedData);
    }

    protected BaseMetaVaultRepository(VaultClient vaultClient, SharedData sharedData, JsonObject config) {
        super(vaultClient, sharedData, config);
    }

    @Override
    protected String buildEntityPath(String userId, String entityId) {
        if (Objects.nonNull(userId) && (userId.startsWith("/") || userId.endsWith("/")))
            throw new IllegalArgumentException("Invalid user ID");

        if (scope == VaultEntityScope.GLOBAL)
            return String.format("%s/global/%s", pathPrefix, dataPath);
        else if (scope == VaultEntityScope.USER)
            return String.format("%s/%s/%s", pathPrefix, Objects.requireNonNull(userId), dataPath);
        else throw new RuntimeException("Unknown repository scope");
    }

    @Override
    protected String buildMetadataPath(String userId) {
        if (Objects.nonNull(userId) && (userId.startsWith("/") || userId.endsWith("/")))
            throw new IllegalArgumentException("Invalid user ID");

        if (scope == VaultEntityScope.GLOBAL)
            return String.format("%s/global/%s", pathPrefix, metaDataPath);
        else if (scope == VaultEntityScope.USER)
            return String.format("%s/%s/%s", pathPrefix, Objects.requireNonNull(userId), metaDataPath);
        else throw new RuntimeException("Unknown repository scope");
    }

    public Future<EntityType> find(String vaultToken, String userId) {
        return super.findById(vaultToken, userId, null);
    }

    public Future<JsonObject> findRaw(String vaultToken, String userId) {
        return super.findByIdRaw(vaultToken, userId, null);
    }

    public Future<Lock> checkout(String vaultToken, String userId) {
        return super.checkout(vaultToken, userId, null);
    }

    public Future<Boolean> lookup(String vaultToken, String userId) {
        return super.lookup(vaultToken, userId, null);
    }

    public Future<EntityType> save(String vaultToken, String userId, EntityType entity) {
        return super.save(vaultToken, userId, null, entity);
    }

    public Future<Void> delete(String vaultToken, String userId) {
        return super.delete(vaultToken, userId, null);
    }

    public Future<EntityType> put(String vaultToken, String userId, String key, Object value) {
        return super.put(vaultToken, userId, null, key, value);
    }

    public Future<EntityType> remove(String vaultToken, String userId, String key) {
        return super.remove(vaultToken, userId, null, key);
    }
}
