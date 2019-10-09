package com.vertxboot.core;

import com.vertxboot.beans.BeanConfig;
import com.vertxboot.beans.BeanFactory;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.reflections.Reflections;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageCodecLoader {

    protected static Logger logger = LoggerFactory.getLogger(MessageCodecLoader.class);

    protected MessageCodecLoader() {
    }

    public static MessageCodecLoader load(EventBus eventBus, MessageCodecConfig messageCodecConfig) {
        logger.info("MessageCodecLoader: registering message codecs start...");
        MessageCodecLoader messageCodecLoader = new MessageCodecLoader();
        logger.info("MessageCodecLoader: scanning for message POJOs");
        Reflections reflections = BeanFactory.instance().getSync(Reflections.class);
        Set<Class<?>> messageClassesSet = reflections.getTypesAnnotatedWith(Message.class);
        Set<Class<?>> customMessageCodecsSet = reflections.getTypesAnnotatedWith(CustomMessageCodec.class);
        Map<Class<?>, MessageCodec<?, ?>> messageClassToMessageCodecMap = customMessageCodecsSet
                .stream()
                .peek(customMessageCodecClass -> {
                    if (Stream.of(customMessageCodecClass.getInterfaces())
                            .noneMatch(MessageCodec.class::equals))
                        throw new RuntimeException("Classes annotated with CustomMessageCodec must implement "
                                + MessageCodec.class.getName());
                })
                .collect(Collectors.toMap(
                        customMessageCodecClass -> ((CustomMessageCodec) customMessageCodecClass
                                .getAnnotation(CustomMessageCodec.class)).messageClass(),
                        MessageCodecLoader::uncheckedNewInstance));

        messageClassToMessageCodecMap.forEach((messageClass, messageCodec) ->
                MessageCodecLoader.registerCodec(eventBus, messageClass, messageCodec));

        if (Objects.nonNull(messageCodecConfig)) {
            logger.info("MessageCodecLoader: registering message codecs for built in classes");
            messageCodecConfig.builtInMessageClasses().forEach(messageClass -> {
                try {
                    if (messageClassToMessageCodecMap.containsKey(messageClass)) {
                        logger.info("MessageCodecLoader: custom message codec registered for " + messageClass.getName());
                        return;
                    }

                    logger.info("MessageCodecLoader: registering generated message codec for " + messageClass.getName());
                    MessageCodecLoader.registerDefaultCodec(eventBus, messageClass);
                } catch (Exception e) {
                    logger.error("MessageCodecLoader: registering failed: " + messageClass.getName(), e);
                }
            });
        }

        logger.info("MessageCodecLoader: registering message codecs for message POJOs");
        messageClassesSet.forEach(messageClass -> {
            try {
                if (messageClassToMessageCodecMap.containsKey(messageClass)) {
                    logger.info("MessageCodecLoader: custom message codec registered for " + messageClass.getName());
                    return;
                }

                logger.info("MessageCodecLoader: registering generated message codec for " + messageClass.getName());
                MessageCodecLoader.registerDefaultCodec(eventBus, messageClass);
            } catch (Exception e) {
                logger.error("MessageCodecLoader: registering failed: " + messageClass.getName(), e);
            }
        });

        logger.info("MessageCodecLoader: registering message codecs done");
        return messageCodecLoader;
    }

    protected static <T> void registerCodec(EventBus eventBus, Class<T> messageClass, MessageCodec<?, ?> messageCodec) {
        try {
            eventBus.registerDefaultCodec(messageClass, (MessageCodec<T, ?>) messageCodec);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error registering CustomMessageCodec instance of class %s for message class %s",
                    messageCodec.getClass().getName(), messageClass.getName()));
        }
    }

    protected static <T> void registerDefaultCodec(EventBus eventBus, Class<T> messageClass) {
        eventBus.registerDefaultCodec(messageClass, MessageCodecFactory.messageCodec(messageClass));
    }

    protected static MessageCodec<?, ?> uncheckedNewInstance(Class<?> customMessageCodecClass) {
        try {
            return ((Class<? extends MessageCodec<?, ?>>) customMessageCodecClass).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating CustomMessageCodec instance for class " + customMessageCodecClass.getName());
        }
    }

    @BeanConfig(async = false, overridable = true)
    public static MessageCodecLoader messageCodecLoader(Vertx vertx, MessageCodecConfig messageCodecConfig) {
        return MessageCodecLoader.load(vertx.eventBus(), messageCodecConfig);
    }
}
