package com.vertxboot.vault.repository;

import com.vertxboot.vault.entity.BaseVaultEntity;
import com.vertxboot.vault.entity.VaultEntityId;
import com.vertxboot.vault.entity.VaultEntityScope;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import java.lang.reflect.Field;
import java.util.Objects;

public abstract class BaseVaultRepository<IdType, EntityType extends BaseVaultEntity<IdType>> extends AbstractVaultRepository<IdType, EntityType> {
    protected String idFieldName;

    protected BaseVaultRepository(VaultClient vaultClient, SharedData sharedData) {
        super(vaultClient, sharedData);
        this.initialize();
    }

    protected BaseVaultRepository(VaultClient vaultClient, SharedData sharedData, JsonObject config) {
        super(vaultClient, sharedData, config);
        this.initialize();
    }

    private void initialize() {
        String tempIdFieldName = null;
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(VaultEntityId.class) && tempIdFieldName != null)
                throw new IllegalStateException("Only one member field can be annotated as the entity id");
            else if (field.isAnnotationPresent(VaultEntityId.class))
                tempIdFieldName = field.getName();
        }

        if (tempIdFieldName == null)
            throw new IllegalStateException("Error capturing entity id field name");

        this.idFieldName = tempIdFieldName;
    }

    @Override
    protected String buildEntityPath(String userId, IdType entityId) {
        if (Objects.nonNull(userId) && (userId.startsWith("/") || userId.endsWith("/")))
            throw new IllegalArgumentException("Invalid user ID");

        if (Objects.isNull(entityId))
            throw new IllegalArgumentException("Entity ID is null");

        String entityIdString = entityId.toString();
        if (entityIdString.startsWith("/") || entityIdString.endsWith("/"))
            throw new IllegalArgumentException("Invalid entity ID");

        if (scope == VaultEntityScope.GLOBAL)
            return String.format("%s/global/%s/%s", pathPrefix, dataPath, entityIdString);
        else if (scope == VaultEntityScope.USER)
            return String.format("%s/%s/%s/%s", pathPrefix, Objects.requireNonNull(userId), dataPath, entityIdString);
        else throw new IllegalStateException("Unknown repository scope");
    }

    @Override
    protected String buildMetadataPath(String userId) {
        if (Objects.nonNull(userId) && (userId.startsWith("/") || userId.endsWith("/")))
            throw new IllegalArgumentException("Invalid user ID");

        if (scope == VaultEntityScope.GLOBAL)
            return String.format("%s/global/%s", pathPrefix, metaDataPath);
        else if (scope == VaultEntityScope.USER)
            return String.format("%s/%s/%s", pathPrefix, Objects.requireNonNull(userId), metaDataPath);
        else throw new IllegalStateException("Unknown repository scope");
    }

    @Override
    public Future<EntityType> findById(String vaultToken, String userId, IdType id) {
        return super.findById(vaultToken, userId, id);
    }

    @Override
    public Future<JsonObject> findByIdRaw(String vaultToken, String userId, IdType id) {
        return super.findByIdRaw(vaultToken, userId, id);
    }

    @Override
    public Future<Lock> checkout(String vaultToken, String userId, IdType id) {
        return super.checkout(vaultToken, userId, id);
    }

    @Override
    public Future<Boolean> lookup(String vaultToken, String userId, IdType id) {
        return super.lookup(vaultToken, userId, id);
    }

    public Future<EntityType> save(String vaultToken, String userId, EntityType entity) {
        return super.save(vaultToken, userId, entity.getId(), entity);
    }

    public Future<Void> delete(String vaultToken, String userId, EntityType entity) {
        return super.delete(vaultToken, userId, entity.getId());
    }

    @Override
    public Future<Void> delete(String vaultToken, String userId, IdType id) {
        return super.delete(vaultToken, userId, id);
    }

    @Override
    public Future<EntityType> put(String vaultToken, String userId, IdType id, String key, Object value) {
        if (idFieldName.equals(key))
            return Future.failedFuture(new IllegalArgumentException("Cannot overwrite the entity id property"));

        return super.put(vaultToken, userId, id, key, value);
    }

    @Override
    public Future<EntityType> remove(String vaultToken, String userId, IdType id, String key) {
        if (idFieldName.equals(key))
            return Future.failedFuture(new IllegalArgumentException("Cannot remove the entity id property"));

        return super.remove(vaultToken, userId, id, key);
    }
}
