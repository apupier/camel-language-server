/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.eclipse.lsp4j.InitializeParams;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;

class SettingsManagerTest {

	@Test
	void testKafkaConnectionWithoutAuthSettings() throws Exception {
		SettingsManager settingsManager = new SettingsManager(new CamelTextDocumentService(null));
		
		InitializeParams params = new InitializeParams();
		String initializationOptions = "{"
				+ "\"camel\": {"
				+ "\""+SettingsManager.KAKFA_CONNECTION_URL + "\": \"locahost:9090\""
				+ "}}";
		params.setInitializationOptions(initializationOptions);
		settingsManager.apply(params);
		
		KafkaConnectionSettings kafkaConnectionSettings = settingsManager.getKafkaConnectionSettings();
		assertThat(kafkaConnectionSettings.getKafkaConnectionUrl()).isEqualTo("locahost:9090");
		assertThat(kafkaConnectionSettings.generateClientConfigParameters())
			.containsOnly(entry("bootstrap.servers","locahost:9090"));
	}
	
	@Test
	void testKafkaConnectionWithAuthSettings() throws Exception {
		SettingsManager settingsManager = new SettingsManager(new CamelTextDocumentService(null));
		
		InitializeParams params = new InitializeParams();
		String initializationOptions = "{"
				+ "\"camel\": {"
				+ "\""+SettingsManager.KAKFA_CONNECTION_URL + "\": \"locahost:9190\","
				+ "\""+SettingsManager.KAKFA_CONNECTION_AUTHENTICATION_TYPE + "\": \"SASL_PLAIN\","
				+ "\""+SettingsManager.KAKFA_CONNECTION_SASL_USERNAME + "\": \"a username\","
				+ "\""+SettingsManager.KAKFA_CONNECTION_SASL_PASSWORD + "\": \"a password\""
				+ "}}";
		params.setInitializationOptions(initializationOptions);
		settingsManager.apply(params);
		
		KafkaConnectionSettings kafkaConnectionSettings = settingsManager.getKafkaConnectionSettings();
		assertThat(kafkaConnectionSettings.getKafkaConnectionUrl()).isEqualTo("locahost:9190");
		assertThat(kafkaConnectionSettings.generateClientConfigParameters())
			.contains(entry("bootstrap.servers","locahost:9190"),
					entry("security.protocol", "SASL_SSL"),
					entry("sasl.mechanism", "PLAIN"),
					entry("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"a username\" password=\"a password\";"));
	}

}
