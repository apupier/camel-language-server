/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.references;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.parser.helper.CamelXmlHelper;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;
import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;

public class ReferencesProcessor {
	
	private static final List<String> POSSIBLE_DIRECT_REFERENCE = Arrays.asList("direct","direct-vm");
	private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProcessor.class);
	private ParserXMLFileHelper parserXMLFileHelper = new ParserXMLFileHelper();
	private TextDocumentItem textDocumentItem;

	public ReferencesProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	@SuppressWarnings("squid:S1452")
	public CompletableFuture<List<? extends Location>> getReferences(Position position) {
		if (textDocumentItem.getUri().endsWith(".xml")) {
			try {
				String camelComponentUri = parserXMLFileHelper.getCamelComponentUri(textDocumentItem, position);
				CamelURIInstance camelURIInstanceToSearchReference = parserXMLFileHelper.createCamelURIInstance(textDocumentItem, position, camelComponentUri);
				if (isDirectComponentKind(camelURIInstanceToSearchReference)) {
					Map<CamelURIInstance, Node> allCamelUriInstances = retrieveAllEndpoints();
					return CompletableFuture.completedFuture(findReferences(camelURIInstanceToSearchReference, allCamelUriInstances));
				}
			} catch (Exception e) {
				LOGGER.error("Cannot compute references", e);
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private List<Location> findReferences(CamelURIInstance camelURIInstanceToSearchReference, Map<CamelURIInstance, Node> allCamelUriInstance) {
		List<Location> references = new ArrayList<>();
		String directId = getDirectId(camelURIInstanceToSearchReference);
		if (directId != null && !directId.isEmpty()) {
			for (Entry<CamelURIInstance, Node> entry : allCamelUriInstance.entrySet()) {
				CamelURIInstance camelURIInstance = entry.getKey();
				if (isReference(camelURIInstanceToSearchReference, directId, camelURIInstance)) {
					references.add(parserXMLFileHelper.retrieveLocation(entry.getValue(), textDocumentItem));
				}
			}
		}
		return references;
	}

	private boolean isReference(CamelURIInstance camelURIInstanceToSearchReference, String directId, CamelURIInstance camelURIInstance) {
		return isDirectComponentKind(camelURIInstance)
				&& (camelURIInstanceToSearchReference.isProducer() && !camelURIInstance.isProducer()
						|| !camelURIInstanceToSearchReference.isProducer() && camelURIInstance.isProducer())
				&& directId.equals(getDirectId(camelURIInstance));
	}

	private Map<CamelURIInstance, Node> retrieveAllEndpoints() throws Exception {
		List<Node> allEndpoints = parserXMLFileHelper.getAllEndpoints(textDocumentItem);
		Map<CamelURIInstance, Node> allCamelUriInstance = new HashMap<>();
		for (Node endpoint : allEndpoints) {
			String uriToParse = CamelXmlHelper.getSafeAttribute(endpoint, "uri");
			if (uriToParse != null) {
				allCamelUriInstance.put(new CamelURIInstance(uriToParse, endpoint, textDocumentItem), endpoint);
			}
		}
		return allCamelUriInstance;
	}
	
	private static String getDirectId(CamelURIInstance camelDirectURIInstance) {
		Set<PathParamURIInstance> pathParams = camelDirectURIInstance.getComponentAndPathUriElementInstance().getPathParams();
		if (!pathParams.isEmpty()) {
			return pathParams.iterator().next().getValue();
		}
		return null;
	}

	public static boolean isDirectComponentKind(CamelUriElementInstance camelURIInstanceToSearchReference) {
		return POSSIBLE_DIRECT_REFERENCE.contains(camelURIInstanceToSearchReference.getComponentName());
	}

	public static List<String> retrieveEndpointIDsOfScheme(String scheme, ParserXMLFileHelper xmlFileHelper, TextDocumentItem docItem) throws Exception {
		List<Node> allEndpoints = xmlFileHelper.getAllEndpoints(docItem);
		List<String> endpointIDs = new ArrayList<>();
		for (Node endpoint : allEndpoints) {
			String uriToParse = CamelXmlHelper.getSafeAttribute(endpoint, "uri");
			if (uriToParse != null) {
				CamelURIInstance uriInstance = new CamelURIInstance(uriToParse, endpoint, docItem);
				if (isDirectComponentKind(uriInstance) && uriInstance.getComponentName().equalsIgnoreCase(scheme)) {
					String dId = getDirectId(uriInstance);
					String directValue = String.format("%s:%s", scheme, dId);
					if (dId != null && dId.trim().length()>0 && !endpointIDs.contains(directValue)) {
						endpointIDs.add(directValue);
					}
				}
			}
		}
		return endpointIDs;
	}
}
