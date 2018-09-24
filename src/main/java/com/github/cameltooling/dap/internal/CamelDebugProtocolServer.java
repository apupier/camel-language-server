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
package com.github.cameltooling.dap.internal;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.ExceptionInfoArguments;
import org.eclipse.lsp4j.debug.ExceptionInfoResponse;
import org.eclipse.lsp4j.debug.GotoArguments;
import org.eclipse.lsp4j.debug.GotoTargetsArguments;
import org.eclipse.lsp4j.debug.GotoTargetsResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesResponse;
import org.eclipse.lsp4j.debug.ModulesArguments;
import org.eclipse.lsp4j.debug.ModulesResponse;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.RestartArguments;
import org.eclipse.lsp4j.debug.RestartFrameArguments;
import org.eclipse.lsp4j.debug.ReverseContinueArguments;
import org.eclipse.lsp4j.debug.RunInTerminalRequestArguments;
import org.eclipse.lsp4j.debug.RunInTerminalResponse;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetVariableArguments;
import org.eclipse.lsp4j.debug.SetVariableResponse;
import org.eclipse.lsp4j.debug.SourceArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepBackArguments;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepInTargetsArguments;
import org.eclipse.lsp4j.debug.StepInTargetsResponse;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;

/**
 * 
 * JMX connection to Camel inspired from org.fusesource.ide.launcher.debug.model.CamelDebugFacade
 * 
 * @author Aurélien Pupier
 *
 */
public class CamelDebugProtocolServer implements IDebugProtocolServer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelDebugProtocolServer.class);
	public static final String CAMEL_DEBUGGER_MBEAN_DEFAULT = "org.apache.camel:type=tracer,name=BacklogDebugger,*";

	private JMXConnector jmxc;
	private IDebugProtocolClient client;

	@Override
	public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
		return CompletableFuture.completedFuture(new Capabilities());
	}

	@Override
	public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> launch(Map<String, Object> args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> attach(Map<String, Object> args) {
		String jmxURi = (String) args.get("jmx.uri");
		if (jmxURi == null) {
			throw new IllegalArgumentException("jmx.uri parameter is mandatory.");
		}
		try {
			JMXServiceURL url = new JMXServiceURL(jmxURi);
			jmxc = JMXConnectorFactory.connect(url, null);
			jmxc.connect();
			//TODO spin a thread calling getSuspndedbreakpoint on jmx connection and calling client.breakpoint when new one arise?
			Executors.newSingleThreadExecutor().execute(new Runnable() {
				
				@Override
				public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> restart(RestartArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> disconnect(DisconnectArguments args) {
		if (jmxc != null) {
			try {
				jmxc.close();
			} catch (IOException e) {
				LOGGER.error("Error during disconnection.", e);
			}
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
		SourceBreakpoint[] sourceBreakpoints = args.getBreakpoints();
		if(sourceBreakpoints != null && sourceBreakpoints.length != 0) {
			try {
				MBeanServerConnection mBeanServerConnection = jmxc.getMBeanServerConnection();
				Set<ObjectInstance> mBeans = mBeanServerConnection.queryMBeans(new ObjectName(CAMEL_DEBUGGER_MBEAN_DEFAULT), null);
				if (mBeans.size() == 1) {
					ObjectInstance mBeanDebugger = mBeans.iterator().next();
					for (SourceBreakpoint sourceBreakpoint : sourceBreakpoints) {
						Long line = sourceBreakpoint.getLine();
						String nodeId = findNodeIdFromLine(line);
						mBeanServerConnection.invoke(mBeanDebugger.getObjectName(), "addBreakpoint", new Object[] { nodeId } , new String[] { String.class.getName() });
					}
				} else {
					//TODO: need to search with different context ids
				}
			} catch (IOException | InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
				LOGGER.error("Cannot set breakpoint", e);
			}
		}
		return null;
	}

	private String findNodeIdFromLine(Long line) {
		// TODO Implement how to retrieve the nodeid from the line. But how to get the source code?
		return null;
	}

	@Override
	public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> next(NextArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> stepIn(StepInArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> stepOut(StepOutArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> stepBack(StepBackArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> goto_(GotoArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<Void> pause(PauseArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<SourceResponse> source(SourceArguments args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<ThreadsResponse> threads() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
		throw new UnsupportedOperationException();
	}

	public void connect(IDebugProtocolClient client) {
		this.client = client;
	}

}
