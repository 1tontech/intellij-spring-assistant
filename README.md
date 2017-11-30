Spring Assistant - IntelliJ plugin that assists you in developing spring applications
=====================================================================================

![Plugin in action](help.gif)

## What does the plugin do

This plugins provides the following features as of now.

1. Auto completion of the configuration properties in your `yaml` files based on the spring boot's auto configuration jars are present in the classpath
2. Auto completion of the configuration properties in your `yaml` files if you have classes annotated with `@ConfigurationProperties`, [if your build is properly configured](#setup-for-showing-configurationproperties-as-suggestions-within-current-module)
3. Short form search & search for element deep within is also supported. i.e, `sp.d` will show you `spring.data`, `spring.datasource`, e.t.c
4. Quick documentation for known groups & properties (not all groups & properties will have documentation, this depends on whether the original author specified documentation or not)

## Future plans

1. Support for `properties` files
2. Support for creating spring applications using `Spring initializr`

## Usage

Assuming that you have Spring boot's auto configuration jars are present in the classpath, this plugin will automatically allows you to autocomplete properties as suggestions in all your `yml` files

Suggestions would appear as soon as you type/press `CTRL+SPACE`.

Short form suggestions are also supported such as, `sp.d` will show you `spring.data`, `spring.datasource`, e.t.c as suggestions that make your typing faster

In addition to libraries in the classpath, the plugin also allows you to have your own `@ConfigurationProperties` available as suggestions in all your `yml` files.

For this to work, you need to ensure the following steps are followed for your project/module

### Setup for showing ConfigurationProperties as suggestions within current module

1. Make sure you add the following dependency to your project

    *For Maven*

    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    ```
    *For Gradle*

    You can use the [propdeps-plugin](https://github.com/spring-gradle-plugins/propdeps-plugin) for `optional` scope (as we dont need `spring-boot-configuration-processor` as a dependency in the generated jar/war) & specify:

    ```gradle
    dependencies {
        optional "org.springframework.boot:spring-boot-configuration-processor"
    }

    compileJava.dependsOn(processResources)
    ```

    Have a look at the [samples](samples/) folder for projects where `@ConfigurationProperties` are shown as suggestions

2. Make sure `Enable annotation processing` is checked under `Settings > Build, Execution & Deployment > Compiler > Annotation Processors`


**IMPORTANT**
> After changing dependencies/changing your `@ConfigurationProperties` files, suggestions would be refreshed only after you trigger the build explicitly using keyboard (`Ctrl+F9`)/UI

### Known behaviour in ambigious cases

> 1. If two groups from different auto configurations conflict with each other, the documentation for the group picked is random & undefined
> 2. If a group & property represent the depth, the behaviour of the plugin is undefined.

## Installation (in 3 easy steps)

To install the plugin open your editor (IntelliJ) and hit:

1. `File > Settings > Plugins` and click on the `Browse repositories` button.
2. Look for `Spring Assistant` the right click and select `Download plugin`.
3. Finally hit the `Apply` button, agree to restart your IDE and you're all done!

Feel free to let me know what else you want added via the [issues](https://github.com/1tontech/intellij-spring-assistant/issues)

Suggestions, feedback and other comments welcome via [@1tontech](https://twitter.com/1tontech) on Twitter

## Changelog

See [here](CHANGELOG.md)

## License

Spring Assistant - IntelliJ Plugin is open-sourced software licensed under the [MIT license](http://opensource.org/licenses/MIT)
