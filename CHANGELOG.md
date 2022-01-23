# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com), and this project adheres
to [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- Inspection: If the property is not defined.
- Inspection: If the property value is in wrong format.
- Intelligence insertion: add new property anywhere, insertion will happen at right place.
- Bug reporting.
- Support
  for [value providers](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html#configuration-metadata.manual-hints.value-providers)
  .
- Join lines will join keys in yaml.

### Changed

### Fixed

## [0.13.0-eap1]

### Added

- Spring properties (yaml) file have got a new icon.
- 'Go to declaration(Ctrl-B or Ctrl-Click)' will navigate to the source code of the property in yaml file.

### Changed

- This plugin will be activated only in application*.properties/yml/yaml files by default, this will avoid some annoying
  side effect while you are editing other yaml/properties files, these settings can be changed at Settings->Editor->File
  Types->Spring (yaml) properties file.
- The document of properties is better formatted.

### Fixed

- After rebuild, generated metadata files in project is not correctly reindex-ed.
- 'additional-spring-configuration-metadata.json' file is not been correctly processed sometimes.
- Lack of document if @ConfigurationProperties annotated class is using lombok @Getter @Setter feature.

## [0.2.2] - 2021-10-20

### Added
- Compatible with IntelliJ IDEA Community Edition 2021.3.

## [0.1.3] - 2021-10-10
### Fixed
- Fixed issue with metadata not updated after maven reimport.

## [0.1.0] - 2021-10-08
### Changed
- This plugin is now compatible with IntelliJ IDEA Community Edition from version 2019.3 to 2021.2.