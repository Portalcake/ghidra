/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.byteviewer;

import java.util.*;

import org.jdom.Element;

import ghidra.app.events.ProgramLocationPluginEvent;
import ghidra.app.events.ProgramSelectionPluginEvent;
import ghidra.app.services.*;
import ghidra.framework.model.DomainFile;
import ghidra.framework.model.DomainObject;
import ghidra.framework.options.SaveState;
import ghidra.framework.plugintool.Plugin;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import utility.function.Callback;

public abstract class AbstractByteViewerPlugin<P extends ProgramByteViewerComponentProvider>
		extends Plugin {

	protected Program currentProgram;
	private boolean areEventsDisabled;
	protected ProgramLocation currentLocation;

	protected P connectedProvider;

	protected List<P> disconnectedProviders = new ArrayList<>();

	public AbstractByteViewerPlugin(PluginTool tool) {
		super(tool);

		connectedProvider = createProvider(true);
	}

	protected abstract P createProvider(boolean isConnected);

	protected void showConnectedProvider() {
		tool.showComponentProvider(connectedProvider, true);
	}

	public P createNewDisconnectedProvider() {
		P newProvider = createProvider(false);
		disconnectedProviders.add(newProvider);
		tool.showComponentProvider(newProvider, true);
		return newProvider;
	}

	@Override
	protected void init() {
		ClipboardService clipboardService = tool.getService(ClipboardService.class);
		if (clipboardService != null) {
			connectedProvider.setClipboardService(clipboardService);
			for (P provider : disconnectedProviders) {
				provider.setClipboardService(clipboardService);
			}
		}
	}

	/**
	 * Tells a plugin that it is no longer needed. The plugin should remove itself from anything
	 * that it is registered to and release any resources.
	 */
	@Override
	public void dispose() {
		removeProvider(connectedProvider);
		for (P provider : disconnectedProviders) {
			removeProvider(provider);
		}
		disconnectedProviders.clear();
	}

	/**
	 * Process the plugin event; delegates the processing to the byte block.
	 */

	/**
	 * Tells a Plugin to write any data-independent (preferences) properties to the output stream.
	 */
	@Override
	public void writeConfigState(SaveState saveState) {
		connectedProvider.writeConfigState(saveState);
	}

	/**
	 * Tells the Plugin to read its data-independent (preferences) properties from the input stream.
	 */
	@Override
	public void readConfigState(SaveState saveState) {
		connectedProvider.readConfigState(saveState);
	}

	/**
	 * Read data state; called after readConfigState(). Events generated by plugins we depend on
	 * should have been already been thrown by the time this method is called.
	 */
	@Override
	public void readDataState(SaveState saveState) {

		doWithEventsDisabled(() -> {

			ProgramManager programManagerService = tool.getService(ProgramManager.class);

			connectedProvider.readDataState(saveState);

			int numDisconnected = saveState.getInt("Num Disconnected", 0);
			for (int i = 0; i < numDisconnected; i++) {
				Element xmlElement = saveState.getXmlElement("Provider" + i);
				SaveState providerSaveState = new SaveState(xmlElement);
				String programPath = providerSaveState.getString("Program Path", "");
				DomainFile file = tool.getProject().getProjectData().getFile(programPath);
				if (file == null) {
					continue;
				}
				Program program = programManagerService.openProgram(file);
				if (program != null) {
					P provider = createProvider(false);
					provider.doSetProgram(program);
					provider.readConfigState(providerSaveState);
					provider.readDataState(providerSaveState);
					tool.showComponentProvider(provider, true);
					addProvider(provider);
				}
			}

		});
	}

	/**
	 * Tells the Plugin to write any data-dependent state to the output stream.
	 */
	@Override
	public void writeDataState(SaveState saveState) {
		connectedProvider.writeDataState(saveState);
		saveState.putInt("Num Disconnected", disconnectedProviders.size());
		int i = 0;
		for (P provider : disconnectedProviders) {
			SaveState providerSaveState = new SaveState();
			DomainFile df = provider.getProgram().getDomainFile();
			if (df.getParent() == null) {
				continue; // not contained within project
			}
			String programPathname = df.getPathname();
			providerSaveState.putString("Program Path", programPathname);
			provider.writeConfigState(providerSaveState);
			provider.writeDataState(providerSaveState);
			String elementName = "Provider" + i;
			saveState.putXmlElement(elementName, providerSaveState.saveToXml());
			i++;
		}
	}

	@Override
	public Object getUndoRedoState(DomainObject domainObject) {
		Map<Long, Object> stateMap = new HashMap<>();

		addUndoRedoState(stateMap, domainObject, connectedProvider);

		for (P provider : disconnectedProviders) {
			addUndoRedoState(stateMap, domainObject, provider);
		}

		if (stateMap.isEmpty()) {
			return null;
		}
		return stateMap;
	}

	private void addUndoRedoState(Map<Long, Object> stateMap, DomainObject domainObject,
			P provider) {
		if (provider == null) {
			return;
		}
		Object state = provider.getUndoRedoState(domainObject);
		if (state != null) {
			stateMap.put(provider.getInstanceID(), state);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restoreUndoRedoState(DomainObject domainObject, Object state) {
		Map<Long, Object> stateMap = (Map<Long, Object>) state;
		restoreUndoRedoState(stateMap, domainObject, connectedProvider);
		for (P provider : disconnectedProviders) {
			restoreUndoRedoState(stateMap, domainObject, provider);
		}

	}

	private void restoreUndoRedoState(Map<Long, Object> stateMap, DomainObject domainObject,
			P provider) {
		if (provider == null) {
			return;
		}
		Object state = stateMap.get(provider.getInstanceID());
		if (state != null) {
			provider.restoreUndoRedoState(domainObject, state);
		}
	}

	@Override
	public Object getTransientState() {
		Object[] state = new Object[2];

		SaveState ss = new SaveState();
		connectedProvider.writeDataState(ss);

		state[0] = ss;
		state[1] = connectedProvider.getCurrentSelection();

		return state;
	}

	@Override
	public void restoreTransientState(Object objectState) {

		doWithEventsDisabled(() -> {
			Object[] state = (Object[]) objectState;
			connectedProvider.restoreLocation((SaveState) state[0]);
			connectedProvider.setSelection((ProgramSelection) state[1]);
		});
	}

	private void doWithEventsDisabled(Callback callback) {
		areEventsDisabled = true;
		try {
			callback.call();
		}
		finally {
			areEventsDisabled = false;
		}
	}

	protected boolean eventsDisabled() {
		return areEventsDisabled;
	}

	void setStatusMessage(String msg) {
		tool.setStatusInfo(msg);
	}

	void addProvider(P provider) {
		disconnectedProviders.add(provider);
		provider.setClipboardService(tool.getService(ClipboardService.class));
	}

	Program getProgram() {
		return currentProgram;
	}

	// Silly Junits - only public until we move to the new multi-view system
	public P getProvider() {
		return connectedProvider;
	}

	public abstract void updateSelection(ByteViewerComponentProvider provider,
			ProgramSelectionPluginEvent event, Program program);

	public abstract void highlightChanged(ByteViewerComponentProvider provider,
			ProgramSelection highlight);

	public void closeProvider(ByteViewerComponentProvider provider) {
		if (provider == connectedProvider) {
			tool.showComponentProvider(provider, false);
		}
		else {
			disconnectedProviders.remove(provider);
			removeProvider(provider);
		}
	}

	protected void exportLocation(Program program, ProgramLocation location) {
		GoToService service = tool.getService(GoToService.class);
		if (service != null) {
			service.goTo(location, program);
		}
	}

	protected void removeProvider(ByteViewerComponentProvider provider) {
		tool.removeComponentProvider(provider);
		provider.dispose();
	}

	protected abstract void updateLocation(
			ProgramByteViewerComponentProvider programByteViewerComponentProvider,
			ProgramLocationPluginEvent event, boolean export);

	protected abstract void fireProgramLocationPluginEvent(
			ProgramByteViewerComponentProvider programByteViewerComponentProvider,
			ProgramLocationPluginEvent pluginEvent);
}
