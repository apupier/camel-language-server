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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.eclipse.lsp4j.CompletionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

public class KafkaTopicCompletionProvider {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTopicCompletionProvider.class);

	public CompletableFuture<List<CompletionItem>> get(PathParamURIInstance pathParamURIInstance, SettingsManager settingsManager) {
		Admin adminClient = null;
		try {
			adminClient = Admin.create(settingsManager.getKafkaConnectionSettings().generateClientConfigParameters());
			ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
			listTopicsOptions.listInternal(true);
			Set<String> topics = adminClient.listTopics(listTopicsOptions).names().get(500, TimeUnit.MILLISECONDS);
			List<CompletionItem> completionItemsForKafkaTopics = topics.stream().map(topic -> {
				CompletionItem completionItem = new CompletionItem(topic);
				CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
				return completionItem;
			}).collect(Collectors.toList());
			return CompletableFuture.completedFuture(completionItemsForKafkaTopics);
		} catch (InterruptedException e) {
			warnLog(settingsManager.getKafkaConnectionSettings().getKafkaConnectionUrl(), e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException e) {
			warnLog(settingsManager.getKafkaConnectionSettings().getKafkaConnectionUrl(), e);
		} finally {
			if(adminClient != null) {
				adminClient.close(Duration.ofMillis(50));
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private void warnLog(String kafkaConnectionURl, Exception e) {
		LOGGER.warn("Error while trying to connect to {}", kafkaConnectionURl, e);
	}

}
