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
package com.github.cameltooling.lsp.internal.modelinemodel;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

public class CamelKModelineResourceOption implements ICamelKModelineOptionValue {

	private String value;
	private int startPosition;
	private TextDocumentItem textDocumentItem;

	public CamelKModelineResourceOption(TextDocumentItem textDocumentItem, String value, int startPosition) {
		this.textDocumentItem = textDocumentItem;
		this.value = value;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + value.length();
	}

	@Override
	public String getValueAsString() {
		return value;
	}

	@Override
	public boolean isInRange(int position) {
		return getStartPositionInLine() <= position && position <= getEndPositionInLine();
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(position == getStartPositionInLine()) {
			String documentUri = textDocumentItem.getUri();
			URI uri = URI.create(documentUri);
			File integrationFile = Paths.get(uri).toFile();
			File parentFile = integrationFile.getParentFile();
			if(parentFile.isDirectory()) {
				File[] siblings = parentFile.listFiles(childFile -> !integrationFile.equals(childFile));
				if (siblings != null) {
					List<CompletionItem> completionItems = new ArrayList<>();
					for (File sibling : siblings) {
						completionItems.add(new CompletionItem(sibling.getName()));
					}
					return CompletableFuture.completedFuture(completionItems);
				}
			}
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}

}
