IntelliJ plugin/extension for `Spring Boot auto configuration suggestions` in your `yml` & `properties` files
=============================================================================================================

![Plugin in action](help.gif)

## Usage

If any of the spring boot's auto configuration jars are present in the classpath, this plugin will make the `@ConfigurationProperties` available as suggestions in all your `yml` aswell as `properties` files

Suggestions would appear as soon as you press `CTRL+SPACE`. Short form suggestions are also supported such as, `sp.d` will show you `spring.data`, `spring.datasource`, e.t.c as suggestions that make your typing faster

This would show suggestions for your own `@ConfigurationProperties` aswell, as soon as build is successfully completed.

> For your own files, suggestions would appear only after you trigger the build after completing changes to `@ConfigurationProperties` class

Both of the below issues are not problems with the plugin, rather issues with the owner's of the documentation

> If two groups from different auto configurations conflict with each other, the documentation for the group picked is random & undefined
> If a group & property represent the depth, the behaviour of the plugin is undefined.

If you know how the above two cases can be handled better, feel free to open an [issue](https://github.com/1tontech/springboot-autoconfigure/issues)

## Installation (in 3 easy steps)

To install the plugin open your editor (IntelliJ) and hit:

1. `File > Settings > Plugins` and click on the `Browse repositories` button.
2. Look for `Spring boot autoconfigure` the right click and select `Download plugin`.
3. Finally hit the `Apply` button, agree to restart your IDE and you're all done!

Feel free to let me know what else you want added via the [issues](https://github.com/1tontech/springboot-autoconfigure/issues)

Suggestions, feedback and other comments welcome via [@1tontech](https://twitter.com/1tontech) on Twitter

## Changelog

See [here](CHANGELOG.md)

## License

Spring Boot auto configuration suggestions - IntelliJ Plugin is open-sourced software licensed under the [MIT license](http://opensource.org/licenses/MIT)
