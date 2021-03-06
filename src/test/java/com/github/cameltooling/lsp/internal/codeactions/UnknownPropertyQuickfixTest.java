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
package com.github.cameltooling.lsp.internal.codeactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.core.ConditionFactory;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.RangeChecker;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;

class UnknownPropertyQuickfixTest extends AbstractCamelLanguageServerTest {

	private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(5);
	private CamelLanguageServer camelLanguageServer;
	
	@Test
	void testReturnCodeActionForQuickfix() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic();
	
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CodeActionContext context = new CodeActionContext(lastPublishedDiagnostics.getDiagnostics(), Collections.singletonList(CodeActionKind.QuickFix));
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testReturnCodeActionForQuickfixWhenNoCodeActionKindSpecified() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic();
	
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CodeActionContext context = new CodeActionContext(lastPublishedDiagnostics.getDiagnostics());
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testReturnNoCodeActionForOtherThanQuickfix() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic();
		
		 List<String> codeActionKinds = Stream.of(CodeActionKind.Refactor, CodeActionKind.RefactorExtract, CodeActionKind.RefactorInline, CodeActionKind.RefactorRewrite, CodeActionKind.Source, CodeActionKind.SourceOrganizeImports)
			      .collect(Collectors.toList());
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CodeActionContext context = new CodeActionContext(lastPublishedDiagnostics.getDiagnostics(), codeActionKinds);
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		assertThat(codeActions.get()).isEmpty();
	}
	
	@Test
	void testReturnCodeActionForQuickfixEvenWithInvalidRangeDiagnostic() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic();
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
	
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		Diagnostic diagnosticWithInvalidRange = new Diagnostic(new Range(new Position(9,100), new Position(9,101)), "a different diagnostic coming with an invalid range.");
		diagnosticWithInvalidRange.setCode(DiagnosticService.ERROR_CODE_UNKNOWN_PROPERTIES);
		diagnostics.add(diagnosticWithInvalidRange);
		diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());
		
		CodeActionContext context = new CodeActionContext(diagnostics, Collections.singletonList(CodeActionKind.QuickFix));
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testNoErrorWithDiagnosticWithoutCode() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic();
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
	
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		Diagnostic diagnosticWithoutCode = new Diagnostic(new Range(new Position(9,33), new Position(9,37)), "a different diagnostic coming without code.");
		diagnostics.add(diagnosticWithoutCode);
		diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());
		
		CodeActionContext context = new CodeActionContext(diagnostics, Collections.singletonList(CodeActionKind.QuickFix));
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	private void checkRetrievedCodeAction(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic, CompletableFuture<List<Either<Command, CodeAction>>> codeActions)
			throws InterruptedException, ExecutionException {
		assertThat(codeActions.get()).hasSize(1);
		CodeAction codeAction = codeActions.get().get(0).getRight();
		assertThat(codeAction.getDiagnostics()).containsOnly(diagnostic);
		assertThat(codeAction.getKind()).isEqualTo(CodeActionKind.QuickFix);
		List<TextEdit> createdChanges = codeAction.getEdit().getChanges().get(textDocumentIdentifier.getUri());
		assertThat(createdChanges).isNotEmpty();
		TextEdit textEdit = createdChanges.get(0);
		Range range = textEdit.getRange();
		new RangeChecker().check(range, 9, 33, 9, 37);
		assertThat(textEdit.getNewText()).isEqualTo("delay");
	}

	private TextDocumentIdentifier initAnLaunchDiagnostic() throws FileNotFoundException {
		File f = new File("src/test/resources/workspace/diagnostic/camel-with-unknownParameter.xml");
		camelLanguageServer = initializeLanguageServer(new FileInputStream(f), ".xml");
		
		TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(DUMMY_URI+".xml");
		DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(textDocumentIdentifier);
		camelLanguageServer.getTextDocumentService().didSave(params);
		
		createAwait().untilAsserted(() -> assertThat(lastPublishedDiagnostics).isNotNull());
		createAwait().untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).hasSize(1));
		return textDocumentIdentifier;
	}
	
	private ConditionFactory createAwait() {
		return await().pollDelay(Duration.ZERO).pollInterval(AWAIT_POLL_INTERVAL).timeout(AWAIT_TIMEOUT);
	}
}
