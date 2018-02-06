package in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util;

// Code borrowed from https://github.com/spring-io/initializr/blob/master/initializr-generator
/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Thrown if a input represents an invalid version.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public class InvalidVersionException extends RuntimeException {

  public InvalidVersionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidVersionException(String message) {
    super(message);
  }

}
