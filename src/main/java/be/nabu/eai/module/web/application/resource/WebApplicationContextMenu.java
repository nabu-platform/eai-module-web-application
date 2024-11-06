/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.web.application.resource;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.types.api.ComplexContent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

public class WebApplicationContextMenu implements EntryContextMenuProvider {

	public static void clearCache(WebApplication artifact) throws InterruptedException, ExecutionException {
		Service service = (Service) EAIResourceRepository.getInstance().resolve("nabu.web.application.Services.clearOptimizedCache");
		if (service != null) {
			ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
			input.set("webApplicationId", artifact.getId());
			Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
			ServiceResult serviceResult = run.get();
			if (serviceResult.getException() != null) {
				MainController.getInstance().notify(serviceResult.getException());
			}
		}
	}
	
	@Override
	public MenuItem getContext(Entry entry) {
		if (entry.isNode() && WebApplication.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			try {
				WebApplication application = (WebApplication) entry.getNode().getArtifact();
				if (application.getConfig().isOptimizedLoad()) {
					MenuItem item = new MenuItem("Clear cache");
					item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							try {
								clearCache(application);
								MainController.getInstance().getRepository().reload(entry.getId());
							}
							catch (Exception e) {
								MainController.getInstance().notify(e);
							}
//							ResourceContainer<?> privateDirectory = (ResourceContainer<?>) application.getDirectory().getChild(EAIResourceRepository.PRIVATE);
//							if (privateDirectory != null) {
//								ResourceContainer<?> cacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
//								if (cacheFolder != null) {
//									List<Resource> resourcesToDelete = new ArrayList<Resource>();
//									for (Resource resource : cacheFolder) {
//										resourcesToDelete.add(resource);
//									}
//									try {
//										for (Resource resource : resourcesToDelete) {
//											((ManageableContainer<?>) cacheFolder).delete(resource.getName());
//										}
//										MainController.getInstance().getRepository().reload(entry.getId());
//										MainController.getInstance().getServer().getRemote().reload(entry.getId());
//									}
//									catch (Exception e) {
//										MainController.getInstance().notify(e);						
//									}
//								}
//							}
						}
					});
					return item;
				}
			}
			catch (Exception e) {
				MainController.getInstance().notify(e);
			}
			
//			MenuItem item = new MenuItem("View application");
//			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
//				@Override
//				public void handle(ActionEvent arg0) {
//					try {
//						WebApplication application = (WebApplication) entry.getNode().getArtifact();
//						new WebBrowser(application).open();
//					}
//					catch (IOException | ParseException e) {
//						MainController.getInstance().notify(e);
//					}
//				}
//			});
		}
		return null;
	}

}
