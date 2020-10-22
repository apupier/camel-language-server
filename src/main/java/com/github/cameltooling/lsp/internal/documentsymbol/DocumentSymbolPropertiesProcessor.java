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
package com.github.cameltooling.lsp.internal.documentsymbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyEntryInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

public class DocumentSymbolPropertiesProcessor {

	private TextDocumentItem textDocumentItem;

	public DocumentSymbolPropertiesProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public List<Either<SymbolInformation, DocumentSymbol>> getSymbolInformations() {
		List<Either<SymbolInformation, DocumentSymbol>> res = new ArrayList<>();
		Set<CamelPropertyEntryInstance> propertyEntries = loadModel(textDocumentItem);
		Optional<CamelPropertyEntryInstance> propertyEntry = propertyEntries.stream()
				.filter(entry -> CamelKafkaUtil.CONNECTOR_CLASS.equals(entry.getPropertyKey()))
				.findAny();
		DocumentSymbol documentSymbolForConnectorClass = null;
		if (propertyEntry.isPresent()) {
			String fullyQualifiedConnectorClassName = propertyEntry.get().getPropertyValue();
			String simpleName = extractSimpleClassName(fullyQualifiedConnectorClassName);
			documentSymbolForConnectorClass = new DocumentSymbol();
			documentSymbolForConnectorClass.setName(simpleName);
			documentSymbolForConnectorClass.setDetail(fullyQualifiedConnectorClassName);
			documentSymbolForConnectorClass.setRange(propertyEntry.get().getRange());
			documentSymbolForConnectorClass.setSelectionRange(propertyEntry.get().getRange());
			documentSymbolForConnectorClass.setKind(SymbolKind.Class);
			res.add(Either.forRight(documentSymbolForConnectorClass));
		}
		
		List<DocumentSymbol> children = new ArrayList<>(); 
		children.addAll(createDocumentSymbol(propertyEntries, "camel.sink.path.", SymbolKind.Field));
		children.addAll(createDocumentSymbol(propertyEntries, "camel.source.path.", SymbolKind.Field));
		children.addAll(createDocumentSymbol(propertyEntries, "camel.sink.endpoint.", SymbolKind.Constant));
		children.addAll(createDocumentSymbol(propertyEntries, "camel.source.endpoint.", SymbolKind.Constant));
		if (documentSymbolForConnectorClass != null) {
			documentSymbolForConnectorClass.setChildren(children);
		}
		return res;
	}

	private List<DocumentSymbol> createDocumentSymbol(Set<CamelPropertyEntryInstance> propertyEntries, String propertyPrefix, SymbolKind symbolKind) {
		return propertyEntries.stream()
			.filter(entry -> entry.getPropertyKey().startsWith(propertyPrefix))
			.map(camelPathPropertyEntry -> {
				DocumentSymbol documentSymbol = new DocumentSymbol();
				documentSymbol.setName(camelPathPropertyEntry.getPropertyKey().substring(propertyPrefix.length()));
				documentSymbol.setRange(camelPathPropertyEntry.getRange());
				documentSymbol.setSelectionRange(camelPathPropertyEntry.getRange());
				documentSymbol.setKind(symbolKind);
				return documentSymbol;
			})
			.collect(Collectors.toList());
	}

	private Set<CamelPropertyEntryInstance> loadModel(TextDocumentItem documentItem) {
		Set<CamelPropertyEntryInstance> res = new HashSet<>();
		String[] lines = documentItem.getText().split("\\r?\\n");
		for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
			res.add(new CamelPropertyEntryInstance(lines[lineNumber], new Position(lineNumber, 0), documentItem));
		}
		return res;
	}

	private String extractSimpleClassName(String qualifiedConnectorClassName) {
		int lastDotIndex = qualifiedConnectorClassName.lastIndexOf('.');
		if (lastDotIndex != -1) {
			return qualifiedConnectorClassName.substring(lastDotIndex + 1);
		} else {
			return qualifiedConnectorClassName;
		}
	}

}
