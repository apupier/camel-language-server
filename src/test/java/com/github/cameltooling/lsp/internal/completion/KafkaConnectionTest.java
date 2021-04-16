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
package com.github.cameltooling.lsp.internal.completion;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.consol.citrus.kafka.embedded.EmbeddedKafkaServer;
import com.consol.citrus.kafka.embedded.EmbeddedKafkaServerBuilder;

class KafkaConnectionTest {
	
	private EmbeddedKafkaServer kafkaServer;
	
	@AfterEach
	public void tearDown() {
		if(kafkaServer != null) {
			kafkaServer.stop();
		}
	}

	@Test
	void testSasl() throws Exception {
		Map<String, String> brokerProperties = new HashMap<String, String>();
		Path jaasConf = Files.createTempFile("jaas-kafka-server", ".conf");
		byte[] jaasContent = ("KafkaServer {\n"
				+ "    org.apache.kafka.common.security.plain.PlainLoginModule required\n"
				+ "    username=\"admin\"\n"
				+ "    password=\"admin\"\n"
				+ "    user_admin=\"admin\"\n"
				+ "    user_alice=\"alice\"\n"
				+ "    user_bob=\"bob\"\n"
				+ "    user_charlie=\"charlie\";\n"
				+ "};").getBytes();
		Files.write(jaasConf, jaasContent);
		System.setProperty("java.security.auth.login.config", jaasConf.toAbsolutePath().toString());
		brokerProperties.put("authorizer.class.name", "kafka.security.authorizer.AclAuthorizer");
		brokerProperties.put("listeners", "SASL_PLAINTEXT://:9094");
		brokerProperties.put("security.inter.broker.protocol", "SASL_PLAINTEXT");
		brokerProperties.put("sasl.mechanism.inter.broker.protocol", "PLAIN");
		brokerProperties.put("sasl.enabled.mechanisms", "PLAIN");
		kafkaServer = new EmbeddedKafkaServerBuilder()
				.kafkaServerPort(9094)
				.topics("aTopic")
				.brokerProperties(brokerProperties)
				.build();
		kafkaServer.start();
		System.out.println("r");
	}

}
