/*******************************************************************************
 * Copyright 2021 Itzbenz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package Ozone.UI;

import Ozone.Desktop.Bootstrap.Dependency;
import Ozone.Desktop.Bootstrap.OzoneLoader;
import Ozone.Desktop.UI.BotControllerDialog;
import Ozone.Desktop.UI.DebugMenuDialog;
import Ozone.Internal.Module;
import Shared.SharedBoot;
import mindustry.core.Version;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static Ozone.Desktop.Manifest.*;

public class DesktopUI implements Module {
	@Override
	public void init() throws Throwable {
		if (SharedBoot.test) return;
		botControllerDialog = new BotControllerDialog();
		dbgMenu = new DebugMenuDialog();
		ModsMenu.add(botControllerDialog);
		ModsMenu.add(dbgMenu);
		envInf.onInit(this::envInfShown);
	}
	
	void envInfShown() {
		envInf.ad(Version.h);
		dep();
	}
	
	
	@Override
	public List<Class<? extends Module>> dependOnModule() throws IOException {
		return Arrays.asList(Ozone.Patch.UIPatch.class);
	}
	
	void dep() {
		try {
			for (URL u : ((OzoneLoader) this.getClass().getClassLoader()).getURLs()) {
				envInf.ad("Library-URL", u.toExternalForm());
			}
		}catch (Throwable ignored) {
		}
		for (Dependency d : Dependency.dependencies)
			try {
				envInf.ad(d.type.name(), d.getDownload());
			}catch (Throwable i) {
				envInf.ad(d.type.name(), i.toString());
			}
		if (!envInf.b) try {
			Dependency.save();
			envInf.b = true;
		}catch (Throwable ignored) {
		
		}
	}
	
}
