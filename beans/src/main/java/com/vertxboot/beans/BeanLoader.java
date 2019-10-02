package com.vertxboot.beans;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.reflections.Reflections;
import org.reflections.scanners.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BeanLoader {

    private static Logger logger = LoggerFactory.getLogger(BeanLoader.class);

    private BeanLoader() {
    }

    public static void load(String beanScanUrlPrefix) {
        logger.info("BeanLoader: loading beans start...");

        BeanFactory beanFactory = BeanFactory.instance();
        Reflections reflections = new Reflections(beanScanUrlPrefix,
                new TypeElementsScanner(),
                new SubTypesScanner(),
                new MemberUsageScanner(),
                new MethodParameterScanner(),
                new MethodParameterNamesScanner(),
                new TypeAnnotationsScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());

        Reflections internalReflections = new Reflections("com.vertxboot",
                new TypeElementsScanner(),
                new SubTypesScanner(),
                new MemberUsageScanner(),
                new MethodParameterScanner(),
                new MethodParameterNamesScanner(),
                new TypeAnnotationsScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());

        SingletonBean<Reflections> reflectionsBean =
                new SingletonBean<Reflections>().initialize(reflections);

        beanFactory.registerBean(Reflections.class, reflectionsBean);

        // scanning for internal bean config methods
        logger.info("BeanLoader: scanning for internal bean config methods...");
        Set<Method> internalBeanConfigSet =
                internalReflections.getMethodsAnnotatedWith(BeanConfig.class);

        // scanning for bean config methods
        logger.info("BeanLoader: scanning for bean config methods...");
        Set<Method> beanConfigSet = reflections.getMethodsAnnotatedWith(BeanConfig.class);

        // restoring internal bean config methods effective return types
        logger.info("BeanLoader: restoring internal bean config methods effective return types...");
        Map<Method, Class<?>> internalBeanConfigToBeanClassMap = internalBeanConfigSet.stream()
                .collect(Collectors.toMap(Function.identity(), BeanLoader::getEffectiveReturnType));

        Map<Class<?>, Method> internalBeanClassToBeanConfigMap = internalBeanConfigSet.stream().collect(Collectors.toMap(
                internalBeanConfigToBeanClassMap::get, Function.identity(), (method1, method2) -> {
                    if (method2.getAnnotation(BeanConfig.class).overridable()) return method1;
                    if (method1.getAnnotation(BeanConfig.class).overridable()) return method2;
                    throw new RuntimeException(String.format("Duplicate non-overridable bean config for the same bean class %s",
                            internalBeanConfigToBeanClassMap.get(method1).getName()));
                }));

        // restoring bean config methods effective return types
        logger.info("BeanLoader: restoring bean config methods effective return types...");
        Map<Method, Class<?>> beanConfigToBeanClassMap = beanConfigSet.stream()
                .collect(Collectors.toMap(Function.identity(), BeanLoader::getEffectiveReturnType));

        Map<Class<?>, Method> beanClassToBeanConfigMap = beanConfigSet.stream().collect(Collectors.toMap(
                beanConfigToBeanClassMap::get, Function.identity(), (method1, method2) -> {
                    if (method2.getAnnotation(BeanConfig.class).overridable()) return method1;
                    if (method1.getAnnotation(BeanConfig.class).overridable()) return method2;
                    throw new RuntimeException(String.format("Duplicate non-overridable bean config for the same bean class %s",
                            beanConfigToBeanClassMap.get(method1).getName()));
                }));

        // resolving duplicates
        internalBeanClassToBeanConfigMap
                .forEach((beanClass, beanConfigMethod) -> {
                    beanConfigToBeanClassMap.put(beanConfigMethod, beanClass);
                    beanClassToBeanConfigMap.merge(beanClass, beanConfigMethod, (method1, method2) -> {
                        if (method2.getAnnotation(BeanConfig.class).overridable()) {
                            beanConfigSet.remove(method2);
                            beanConfigSet.add(method1);
                            return method1;
                        }

                        if (method1.getAnnotation(BeanConfig.class).overridable()) {
                            beanConfigSet.remove(method1);
                            beanConfigSet.add(method2);
                            return method2;
                        }

                        throw new RuntimeException(String.format(
                                "Duplicate non-overridable bean config for the same bean class %s", beanClass));
                    });
                });

        // building bean dependency graph
        logger.info("BeanLoader: building bean dependency graph...");
        Map<Method, Set<Method>> beanConfigDependencyGraph = new HashMap<>();
        beanConfigSet.forEach(method -> beanConfigDependencyGraph.put(method,
                Arrays.stream(method.getParameterTypes())
                        .map(beanClassToBeanConfigMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())));

        // calculating beans initialization order based on their dependency relations
        logger.info("BeanLoader: calculating beans initialization order...");
        Set<Method> isVisitedLookup = new HashSet<>();
        List<Method> evaluationOrder = new ArrayList<>();
        beanConfigSet.forEach(method -> sort(beanConfigDependencyGraph, isVisitedLookup, evaluationOrder, method));

        // checking for cyclic dependencies
        logger.info("BeanLoader: checking for cyclic dependencies...");
        isVisitedLookup.clear();
        Set<Method> recursionLookup = new HashSet<>();
        List<Method> reversedEvaluationOrder = new ArrayList<>(evaluationOrder);
        Collections.reverse(reversedEvaluationOrder);
        boolean hasCycles = reversedEvaluationOrder
                .stream()
                .map(method -> checkCycles(beanConfigDependencyGraph, isVisitedLookup, recursionLookup, method))
                .reduce(Boolean.FALSE, (partialResult, currentResult) -> (partialResult || currentResult));

        if (hasCycles)
            throw new RuntimeException("Cyclic bean dependency detected");

        // initializing beans
        logger.info("BeanLoader: creating beans...");
        evaluationOrder.forEach(beanConfigMethod -> {
            Class<?> beanClass = beanConfigToBeanClassMap.get(beanConfigMethod);
            logger.info(String.format("BeanLoader: creating bean for class %s", beanClass.getName()));
            List<Future> dependencyFutureList = Arrays
                    .stream(beanConfigMethod.getParameterTypes())
                    .map(beanFactory::get)
                    .collect(Collectors.toList());

            CompositeFuture dependencyListCompositeFuture = CompositeFuture.all(dependencyFutureList);
            dependencyListCompositeFuture.setHandler(dependencyListAsyncResult -> {
                if (dependencyListAsyncResult.failed()) {
                    logger.error(String.format("BeanLoader: bean dependency initialization failed for bean class %s",
                            beanClass.getName()), dependencyListAsyncResult.cause());
                    return;
                }

                List<Object> dependencyList = dependencyListAsyncResult.result().list();
                BeanLoader.registerBean(beanFactory, beanClass, beanConfigMethod, dependencyList);
            });
        });

        logger.info("BeanLoader: loading beans done");
    }

    private static <T> void registerBean(BeanFactory beanFactory, Class<T> beanClass,
                                         Method beanConfigMethod, List<Object> dependencyList) {
        beanFactory.registerBean(beanClass, beanConfigMethod.getAnnotation(BeanConfig.class).scope().create(
                beanClass, beanConfigMethod, dependencyList));
    }

    private static Class<?> getEffectiveReturnType(Method beanConfigMethod) {
        if (beanConfigMethod.getAnnotation(BeanConfig.class).async()) {
            if (!beanConfigMethod.getReturnType().equals(Future.class))
                throw new IllegalArgumentException(String.format("Illegal return type for bean config %s, " +
                                "async beans must return io.vertx.core.Future<T>, where T is the bean type",
                        beanConfigMethod.toString()));

            if (!(beanConfigMethod.getGenericReturnType() instanceof ParameterizedType))
                throw new IllegalArgumentException(String.format("Illegal return type for bean config %s, " +
                                "raw io.vertx.core.Future is not allowed, type arguments must be provided",
                        beanConfigMethod.toString()));

            ParameterizedType parameterizedType = (ParameterizedType) beanConfigMethod.getGenericReturnType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length != 1)
                throw new IllegalArgumentException(String.format("Illegal actual type arguments of return type for " +
                                "bean config %s, expected 1 actual type argument, found %d",
                        beanConfigMethod.toString(), actualTypeArguments.length));

            if (!(actualTypeArguments[0] instanceof Class))
                throw new IllegalArgumentException(String.format("Illegal actual type arguments of return type for " +
                                "bean config %s, expected java.lang.Class, found %s",
                        beanConfigMethod.toString(), actualTypeArguments[0].getTypeName()));

            return (Class<?>) actualTypeArguments[0];
        } else {
            return beanConfigMethod.getReturnType();
        }
    }

    private static void sort(Map<Method, Set<Method>> beanConfigDependencyGraph,
                             Set<Method> isVisitedLookup,
                             List<Method> topologicalSort,
                             Method currentMethod) {

        if (isVisitedLookup.contains(currentMethod))
            return;

        isVisitedLookup.add(currentMethod);

        beanConfigDependencyGraph
                .get(currentMethod)
                .forEach(method -> sort(beanConfigDependencyGraph, isVisitedLookup, topologicalSort, method));

        topologicalSort.add(currentMethod);
    }

    private static boolean checkCycles(Map<Method, Set<Method>> beanConfigDependencyGraph,
                                       Set<Method> isVisitedLookup,
                                       Set<Method> recursionLookup,
                                       Method currentMethod) {

        if (isVisitedLookup.contains(currentMethod))
            return false;

        isVisitedLookup.add(currentMethod);
        recursionLookup.add(currentMethod);

        boolean hasCycle = beanConfigDependencyGraph
                .get(currentMethod)
                .stream()
                .map(recursionLookup::contains)
                .reduce(Boolean.FALSE, (partialResult, currentResult) -> (partialResult || currentResult));

        if (hasCycle) return true;

        hasCycle = beanConfigDependencyGraph
                .get(currentMethod)
                .stream()
                .map(method -> checkCycles(beanConfigDependencyGraph, isVisitedLookup, recursionLookup, method))
                .reduce(Boolean.FALSE, (partialResult, currentResult) -> (partialResult || currentResult));

        recursionLookup.remove(currentMethod);
        return hasCycle;
    }
}
