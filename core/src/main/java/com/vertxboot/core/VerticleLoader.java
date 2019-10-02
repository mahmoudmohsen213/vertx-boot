package com.vertxboot.core;

import com.vertxboot.beans.BeanConfig;
import com.vertxboot.beans.BeanFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class VerticleLoader {

    protected static final String STATIC_DEPLOYMENT_OPTIONS_GETTER = "getDeploymentOptions";

    protected static Logger logger = LoggerFactory.getLogger(VerticleLoader.class);

    protected VerticleLoader() {
    }

    @SuppressWarnings("unchecked")
    public static VerticleLoader load(Vertx vertx, DeploymentOptions deploymentOptions) {
        logger.info("VerticleLoader: loading verticles start...");
        VerticleLoader verticleLoader = new VerticleLoader();
        logger.info("VerticleLoader: scanning for verticles");
        Reflections reflections = BeanFactory.instance().getSync(Reflections.class);
        Set<Class<?>> set = reflections.getTypesAnnotatedWith(Verticle.class);

        set.forEach(verticleClass -> {
            DeploymentOptions verticleDeploymentOptions = deploymentOptions;
            Verticle verticleAnnotation = verticleClass.getAnnotation(Verticle.class);

            if (!verticleAnnotation.useDefaultOptions()) {
                try {
                    logger.info("VerticleLoader: getting deployment options for " + verticleClass.getName());
                    verticleDeploymentOptions = (DeploymentOptions) verticleClass.getMethod(
                            STATIC_DEPLOYMENT_OPTIONS_GETTER).invoke(null);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    logger.error("VerticleLoader: failed to get deployment options for " + verticleClass.getName(), e);
                    return;
                }

                if (verticleDeploymentOptions == null) {
                    logger.error("VerticleLoader: null deployment options for " + verticleClass.getName(),
                            new NullPointerException("Deployment options cannot be null"));
                    return;
                }
            }

            try {
                logger.info("VerticleLoader: deploying " + verticleClass.getName());
                vertx.deployVerticle((Class<? extends AbstractVerticle>) verticleClass, verticleDeploymentOptions);
            } catch (Exception e) {
                logger.error("VerticleLoader: deployment failed: " + verticleClass.getName(), e);
            }
        });

        logger.info("VerticleLoader: loading verticles done");
        return verticleLoader;
    }

    @BeanConfig(async = false, overridable = true)
    public static DeploymentOptions defaultDeploymentOptions() {
        logger.info("VerticleLoader: no deployment options bean is found, creating default deployment options");
        return new DeploymentOptions()
                .setHa(true)
                .setInstances(Runtime.getRuntime().availableProcessors());
    }

    @BeanConfig(async = false, overridable = true)
    public static VerticleLoader messageCodecLoader(Vertx vertx, DeploymentOptions deploymentOptions) {
        return VerticleLoader.load(vertx, deploymentOptions);
    }
}
