package com.vertxboot.core;

import com.vertxboot.beans.BeanConfig;
import com.vertxboot.beans.BeanFactory;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.reflections.Reflections;

import java.util.Objects;
import java.util.Set;

public class MessageCodecLoader {

    protected static Logger logger = LoggerFactory.getLogger(MessageCodecLoader.class);

    protected MessageCodecLoader() {
    }

    public static MessageCodecLoader load(EventBus eventBus) {
        logger.info("MessageCodecLoader: loading message codecs start...");
        MessageCodecLoader messageCodecLoader = new MessageCodecLoader();
        MessageCodecConfig messageCodecConfig = (BeanFactory.instance().isInitialized(MessageCodecConfig.class)) ?
                BeanFactory.instance().getSync(MessageCodecConfig.class) : null;

        if (Objects.nonNull(messageCodecConfig)) {
            logger.info("MessageCodecLoader: loading message codecs for built in classes");
            messageCodecConfig.builtInMessageClasses().forEach(messageClass -> {
                try {
                    logger.info("MessageCodecLoader: loading message codec for " + messageClass.getName());
                    MessageCodecLoader.registerDefaultCodec(eventBus, messageClass);
                } catch (Exception e) {
                    logger.error("MessageCodecLoader: loading failed: " + messageClass.getName(), e);
                }
            });
        }

        logger.info("MessageCodecLoader: scanning for message POJOs");
        Reflections reflections = BeanFactory.instance().getSync(Reflections.class);
        Set<Class<?>> set = reflections.getTypesAnnotatedWith(Message.class);

        logger.info("MessageCodecLoader: loading message codecs for message POJOs");
        set.forEach(messageClass -> {
            try {
                logger.info("MessageCodecLoader: loading message codec for " + messageClass.getName());
                MessageCodecLoader.registerDefaultCodec(eventBus, messageClass);
            } catch (Exception e) {
                logger.error("MessageCodecLoader: loading failed: " + messageClass.getName(), e);
            }
        });

        logger.info("MessageCodecLoader: loading message codecs done");
        return messageCodecLoader;
    }

    protected static <T> void registerDefaultCodec(EventBus eventBus, Class<T> messageClass) {
        eventBus.registerDefaultCodec(messageClass, MessageCodecFactory.messageCodec(messageClass));
    }

    @BeanConfig(async = false, overridable = true)
    public static MessageCodecLoader messageCodecLoader(Vertx vertx) {
        return MessageCodecLoader.load(vertx.eventBus());
    }
}
