Spring Boot Assistant - IntelliJ-based plugin that assists you in developing spring boot applications
=====================================================================================

![Plugin in action](help.gif)

## What does the plugin do

This plugins provides the following features as of now.

1. Auto completion of the configuration properties in your `yaml` files based on the spring boot's auto configuration
   jars are present in the classpath
2. Auto completion of the configuration properties in your `yaml` files if you have classes annotated
   with `@ConfigurationProperties`
   , [if your build is properly configured](#setup-for-showing-configurationproperties-as-suggestions-within-current-module)
3. Short form search & search for element deep within is also supported. i.e, `sp.d` will show you `spring.data`
   , `spring.datasource`, also, `port` would show `server.port` as a suggestion
4. Quick documentation for groups & properties (not all groups & properties will have documentation, depends on whether
   the original author specified documentation or not for any given element)

## Installation (in 3 easy steps)

To install the plugin open your editor (IntelliJ) and hit:

1. `File > Settings > Plugins` and click on the `Browse repositories` button.
2. Look for `Spring Boot Assistant` the right click and select `Download plugin`.
3. Finally, hit the `Apply` button, agree to restart your IDE and you're all done!

## Usage

Assuming that you have Spring boot's autoconfiguration jars are present in the classpath, this plugin will automatically
allow you to autocomplete properties as suggestions in all your `yml` files

Suggestions would appear as soon as you type/press `CTRL+SPACE`.

Short form suggestions are also supported such as, `sp.d` will show you `spring.data`, `spring.datasource`, e.t.c as
suggestions that make your typing faster

In addition to libraries in the classpath, the plugin also allows you to have your own `@ConfigurationProperties`
available as suggestions in all your `yml` files.

For this to work, you need to ensure the following steps are followed for your project/module

### Setup for showing ConfigurationProperties as suggestions within current module

1. Make sure `Enable annotation processing` is checked under `Settings > Build, Execution & Deployment > Compiler > Annotation Processors`
2. Make sure you add the following changes to  to your project

    *For Maven*

    Add the following dependency

    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    ```

    *For Gradle*

   Add the following build configuration. You can use
   the [propdeps-plugin](https://github.com/spring-gradle-plugins/propdeps-plugin) for `optional` scope (as we dont
   need `spring-boot-configuration-processor` as a dependency in the generated jar/war)

    ```gradle
    dependencies {
        optional "org.springframework.boot:spring-boot-configuration-processor"
    }

    compileJava.dependsOn(processResources)
    ```
3. (**OPTIONAL**) If intellij is generating build artifacts to `output` directory instead of gradle's default `build`
   directory, then you may need
   to `File | Settings | Build, Execution, Deployment | Build Tools | Gradle | Runner => Delegate IDE build/run actions to gradle`
   & restart the IDE. This will ensure that gradle plugin generates metadata & Intellij is pointing to it

> If you want to look at a sample project, look inside [samples](samples/) directory where the above setup is done. These samples allow properties from `@ConfigurationProperties` to be shown as suggestions

**IMPORTANT**

> After changing your custom `@ConfigurationProperties` files, suggestions would be refreshed only after you trigger the build explicitly using keyboard (`Ctrl+F9`)/UI

### Known behaviour in ambiguous cases

> 1. If two groups from different auto configurations conflict with each other, the documentation for the group picked is random & undefined
> 2. If a group & property represent the depth, the behaviour of the plugin is undefined.

## Support

For report bugs, or request a new feature, use [Issues](https://github.com/flikas/idea-spring-boot-assistant/issues).

## Changelog

See [here](CHANGELOG.md).

## License

Spring Boot Assistant - IntelliJ-based Plugin is open-sourced software licensed under
the [MIT License](http://opensource.org/licenses/MIT).

## About

This project is based on [Spring Assistant - IntelliJ Plugin](https://github.com/1tontech/intellij-spring-assistant),
thanks to the excellent work by [@1tontech](https://twitter.com/1tontech).