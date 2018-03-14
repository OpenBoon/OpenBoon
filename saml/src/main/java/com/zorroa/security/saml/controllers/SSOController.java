/*
 * Copyright 2017 Vincenzo De Notaris
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
 */

package com.zorroa.security.saml.controllers;

import com.zorroa.security.saml.ZorroaExtendedMetadata;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/saml")
public class SSOController {

	// Logger
	private static final Logger LOG = LoggerFactory
			.getLogger(SSOController.class);

	@Autowired
	private MetadataManager metadata;


	@RequestMapping(value = "/idps")
	public Object idpList() throws MetadataProviderException {
		List<Map<String, Object>> result = new ArrayList();

		List<String> idps = new ArrayList();
		for (String name: metadata.getIDPEntityNames()) {

			ZorroaExtendedMetadata zd = (ZorroaExtendedMetadata) metadata.getExtendedMetadata(name);

			Map<String,Object> entry = new HashMap();
			for (String key: zd.getProps().stringPropertyNames()) {
				entry.put(key, zd.getProps().getProperty(key));
			}
			entry.put("url", "/saml/login?idp=" + name);
			result.add(entry);

		}

		return result;
	}

	@RequestMapping(value = "/idpSelection")
	public String idpSelection(HttpServletRequest request, Model model) {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			LOG.warn("The current user is already logged.");
			return "redirect:/";
		} else {

			if (isForwarded(request)) { Set<String> idps = metadata.getIDPEntityNames();
				for (String idp : idps)
					LOG.info("Configured Identity Provider for SSO: " + idp);
				model.addAttribute("idps", idps);
				return "saml/idpselection";
			} else {
				LOG.warn("Direct accesses to '/idpSelection' route are not allowed");
				return "redirect:/";
			}
		}
	}


	/*
	 * Checks if an HTTP request has been forwarded by a servlet.
	 */
	private boolean isForwarded(HttpServletRequest request){
		if (request.getAttribute("javax.servlet.forward.request_uri") == null)
			return false;
		else
			return true;
	}

}
