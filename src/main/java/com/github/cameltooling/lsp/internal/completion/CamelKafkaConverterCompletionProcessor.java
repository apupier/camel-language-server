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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyValueInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

public class CamelKafkaConverterCompletionProcessor {

	private CamelPropertyValueInstance camelPropertyValueInstance;
	private CamelKafkaConnectorCatalogManager camelKafkaConnectorManager;
	private TextDocumentItem textDocumentItem;

	public CamelKafkaConverterCompletionProcessor(TextDocumentItem textDocumentItem, CamelPropertyValueInstance camelPropertyValueInstance, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		this.textDocumentItem = textDocumentItem;
		this.camelPropertyValueInstance = camelPropertyValueInstance;
		this.camelKafkaConnectorManager = camelKafkaConnectorManager;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(String startFilter) {
		String connectorClass = new CamelKafkaUtil().findConnectorClass(textDocumentItem);
		if (connectorClass != null) {
			Collection<CamelKafkaConnectorModel> camelKafkaConnectors = camelKafkaConnectorManager.getCatalog().getConnectorsModel().values();
			Optional<CamelKafkaConnectorModel> model = camelKafkaConnectors.stream()
					.filter(iteratorModel -> connectorClass.equals(iteratorModel.getConnectorClass()))
					.findAny();
			if (model.isPresent()) {
				List<String> converters = model.get().getConverters();
				if (converters != null) {
					List<CompletionItem> completions = converters.stream()
							.map(converter -> {
								CompletionItem completionItem = new CompletionItem(converter);
								CompletionResolverUtils.applyTextEditToCompletionItem(camelPropertyValueInstance, completionItem);
								return completionItem;
							}).filter(FilterPredicateUtils.matchesCompletionFilter(startFilter)).collect(Collectors.toList());
					return CompletableFuture.completedFuture(completions);
				}
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}
