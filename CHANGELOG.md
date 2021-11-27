# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com), and this project adheres
to [Semantic Versioning](https://semver.org).

## [Unreleased]
### Added
- Spring configuration file have got a new icon.
- A warning if the property is not defined.
### Changed
- 'Go to declaration(Ctrl+B)' on property will now open the source code of it.
### Fixed
- Auto-completion will not show up if the property is already present in the file.
- Auto-completion is not updated when @ConfigurationProperties annotated class is update and rebuild.
- Javadoc is missing on lombok generated properties.
- additional-spring-configuration-metadata.json file has not been read sometimes.

## [0.2.2] - 2021-10-20
### Added
- Compatible with IntelliJ IDEA Community Edition 2021.3.

## [0.1.3] - 2021-10-10
### Fixed
- Fixed issue with metadata not updated after maven reimport.

## [0.1.0] - 2021-10-08
### Changed
- This plugin is now compatible with IntelliJ IDEA Community Edition from version 2019.3 to 2021.2.