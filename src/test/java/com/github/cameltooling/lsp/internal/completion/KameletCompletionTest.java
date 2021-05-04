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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class KameletCompletionTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testKameletTemplateIdCompletionForSource() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer("<from uri=\"kamelet:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n");
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 19)).get().getLeft();
		
		assertThat(completions)
			.hasSizeGreaterThan(10)
			.contains(createAwsddbSourceCompletionItem())
			.doesNotContain(createAwsKinesisSinkCompletionItem());
	}
	
	@Test
	void testKameletTemplateIdCompletionForSink() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer("<to   uri=\"kamelet:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n");
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 19)).get().getLeft();
		
		assertThat(completions)
			.hasSizeGreaterThan(10)
			.contains(createAwsKinesisSinkCompletionItem())
			.doesNotContain(createAwsddbSourceCompletionItem());
	}

	private CompletionItem createAwsddbSourceCompletionItem() {
		CompletionItem completionItem = new CompletionItem("aws-ddb-streams-source");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 19), new Position(0, 19)),
								"aws-ddb-streams-source")));
		completionItem.setDocumentation("Receive events from AWS DynamoDB Streams.");
		return completionItem;
	}
	
	private CompletionItem createAwsKinesisSinkCompletionItem() {
		CompletionItem completionItem = new CompletionItem("aws-kinesis-sink");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 19), new Position(0, 19)),
								"aws-kinesis-sink")));
		completionItem.setDocumentation("Send data to AWS Kinesis.\n\n"
				+ "The Kamelet expects the following header:\n\n"
				+ "- `partition` / `ce-partition`: to set the Kinesis partition key\n\n"
				+ "If the header won't be set the exchange ID will be used.\n\n"
				+ "The Kamelet is also able to recognize the following header:\n\n"
				+ "- `sequence-number` / `ce-sequence-number`: to set the Sequence number\n\n"
				+ "This header is optional.");
		return completionItem;
	}
		
	private CamelLanguageServer initLanguageServer(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(text, ".xml");
	}
	
}
