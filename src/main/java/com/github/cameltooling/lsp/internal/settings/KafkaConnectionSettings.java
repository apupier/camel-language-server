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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class KafkaConnectionSettings {
	
	/**
	 * When launching a local Kafka cluster, this is the default connection provided.
	 * Using it by default avoids users to specify a setting in this simple case.
	 */
	private static final String DEFAULT_CONNECTION = "localhost:9092";
	public static final String CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL = "CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL";
	
	private String kafkaConnectionUrl;
	private KafkaAuthenticationType connectionType;
	private String username;
	private String password;

	public String getKafkaConnectionUrl() {
		if (kafkaConnectionUrl == null) {
			kafkaConnectionUrl = System.getProperty(CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL, DEFAULT_CONNECTION);
		}
		return kafkaConnectionUrl;
	}

	public void setKafkaConnectionUrl(String kafkaConnectionUrl) {
		this.kafkaConnectionUrl = kafkaConnectionUrl;
	}
	
	public Map<String, Object> generateClientConfigParameters() {
		Map<String, Object> clientConfigParameters = new HashMap<>();
		clientConfigParameters.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaConnectionUrl());
		if (KafkaAuthenticationType.SASL_PLAIN.equals(connectionType)) {
			clientConfigParameters.put("security.protocol", "SASL_SSL");
			clientConfigParameters.put("sasl.mechanism", "PLAIN");
			clientConfigParameters.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""+username+"\" password=\""+password+"\";");
		}
		return clientConfigParameters;
	}

	public void setConnectionType(String connectionType) {
		try {
			this.connectionType = KafkaAuthenticationType.valueOf(connectionType);
		} catch (Exception ex) {
			this.connectionType = null;
		}
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
