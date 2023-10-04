# Vert.x-boot
This is a [Vert.x](https://vertx.io/) based library, that offers some missing features, which are inspired by [spring-boot](https://spring.io/projects/spring-boot), as an extension to vertx framework, while preserving it's event driven and async spirit.

The main purpose of this library is to make the life of the developer easy, without putting any constraints to his usage to vertx.

The library is divided into multiple modules with an acyclic dependency hierarchy, so that the user can choose the subset of features he wants, and include only the libraries needed for these features.

The beans module is responsible for the bean scanning, creation, and management. The core module is responsible for the message POJOs and verticles scanning. The web module targets REST backends, it handles rest handlers scanning, creation, and registration to the vertx router. It also handles the creation and start up of the HTTP server. The vault module is an abstraction layer for [Vault](https://www.vaultproject.io) persistence.

Each module has a separate README with full description of how it works and how to use it. Also, there is an example project that uses all of these modules in a simple use case.

Before diving into the details of each module, first...

## What problems are we trying to solve?

### No dependency injection or inversion of control
Vert.x itself doesn't include any means of DI and doesn't implement any form of IoC. DI and IoC may not be a necessity for small applications, but as the project grows larger, things start to get messy as the developer tries to connect different system components to each other.

The developer in this case has three options, the first option is to bring in some other framework as spring to benefit from all its powrfel DI and IoC features, and as a matter of fact there is an example in the official [Vert.x examples repository](https://github.com/vert-x3/vertx-examples) that does exactly that. If this option is adequate to the case in hand, this is great. However, this option might be frowned upon in some cases, because mixing different frameworks with totally different mentalities might not be the ideal solution.

The second option is to implement the basic singleton pattern in all system components, this way different components will be able to access their dependencies using the static instance methods, but there will many difficulties in these instances construction. The difficulties will originate from the fact that some components will require dependencies needed only during their construction, but not available to the dependents while referencing these components. 

Suppose we have Components A, B, and C, where A depends on B, and B depends on C, and the construction of each of them requires some set of arguments. Logically, we should construct the singleton instance of C, then the instance of B, and finally the instance of A. During the construction of A (which is the last one), we either let A directly get the singleton instance of B, or feed it into its constructor, but to do that, B's instance must be already initialized, otherwise, A will have to access the required arguments by B, and pass it to its static instance method.

The third solution, which solves the scenario mentioned above, is to have a central initializer for the entire system components, where the initialization of all components is hardcoded one by one in the correct order. But this will be too messy with big number of components, also it will be subject to frequent changes for every time we add a new component, change their dependency hierarchy, or change the required arguments by any of them.

### No rest handlers management
Vert.x web support is inspired greatly by expressjs, so basically, you add callbacks (represented by vertx [Handler generic interface](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)) to endpoints and HTTP methods, with the ability to have wildcards in the endpoint, add some callback to multiple HTTP methods. The concrete implementation of these callbacks naturally contains the business logic of the controller layer.

The idea itself is great, but when using it in large projects, the developer will end up having a class, with access to the singleton vertx instance (provided by the vertx framework), then this class will get a reference to the router instance, and add the logic of all handlers of all endpoints to the router one by one. The result will be a giant class will the business logic of ALL controllers, including the query param validation callbacks, request body validation callbacks, error handlers, which is a good solution if you want the reviewer to shot you!

A better solution is to isolate the rest handlers logic in separate classes, and have some central class which register these different rest handlers to different end points, or invoke a method on them, with the router as an argument. But this class will still depend on the rest handlers, and there order (if we need interceptors or something).

### Custom message classes
Vert.x eventbus supports sending messages only for primitive types, strings, and vertx JsonObject. To send a POJO on the eventbus, you have to either specify a message codec for it each time you send it, or register a default message codec for this POJO class, which naturally requires writing one. 

By looking at this [example](https://github.com/vert-x3/vertx-examples/blob/master/core-examples/src/main/java/io/vertx/example/core/eventbus/messagecodec/util/CustomMessageCodec.java), we can see that the message encoding and decoding can be standardized even for different classes, the same way the JSON serialization and deserialization doesn't require writing a different serializer for each specific class. Accordingly, there should be a way to dynamically generate such message codecs and register it to the eventbus.

### No application startup life cycle
After solving all the above problems, the application will have multiple <i>manager</i> classes, and these manager classes will need something to construct them or invoke their static methods. Vert.x on its own doesn't provide an organized application startup life cycle, so you will have to write a code for this as well.

The whole thing can be started by simply:
1. Defining a `BeanConfig` for the `Vertx` instance ([examples](https://github.com/search?q=repo%3Amahmoudmohsen213%2Fvertx-boot%20%40BeanConfig&type=code)). This is a dependency for all internal beans. The bean loader will detect this definition and initialize it in the startup.
2. Invoking
   ```
   com.vertxboot.beans.BeanLoader.VertxApplication.run(YourMainJavaClass.class);
   ```
   in the application `main` method (See [VertxApplication](https://github.com/mahmoudmohsen213/vertx-boot/blob/master/beans/src/main/java/com/vertxboot/VertxApplication.java)), where `YourMainJavaClass` must be in a top-level package.
