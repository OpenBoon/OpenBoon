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

import com.zorroa.security.saml.SamlProperties;
import com.zorroa.security.saml.ZorroaExtendedMetadata;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/saml")
public class SSOController {

	@Autowired
	private MetadataManager metadata;

	@Autowired
	private SamlProperties properties;

	@GetMapping(value = "/options")
	public @ResponseBody Object samlOptions() {
		Set<String> idps = metadata.getIDPEntityNames();
		List<String> urls = new ArrayList();
		for (String idp : idps) {
			urls.add("/saml/login?disco=true&idp=" + idp);
		}

		Map<String, Object> result = new HashMap();
		result.put("logout", properties.logout);
		result.put("discovery", properties.discovery);
		result.put("landing", properties.landingPage);
		result.put("baseUrl", properties.baseUrl);
		result.put("proxyBase", properties.baseUrlIsProxy);
		result.put("idps", urls);
		return result;

	}

	@RequestMapping(value = "/idpSelection")
	public String idpSelection(HttpServletRequest request, Model model) throws MetadataProviderException {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			return "redirect:/";
		} else {
			if (isForwarded(request)) {
				Set<String> idps = metadata.getIDPEntityNames();
				List<Properties> props = new ArrayList();

				for (String idp : idps) {
					ZorroaExtendedMetadata zd = (ZorroaExtendedMetadata) metadata.getExtendedMetadata(idp);
					zd.getProps().setProperty("idpUrl", "/saml/login?disco=true&idp=" + idp);
					props.add(zd.getProps());
				}
				model.addAttribute("idps", idps);
				model.addAttribute("props", props);

				return "saml/idpselection";
			} else {
				//"Direct accesses to '/idpSelection' route are not allowed"
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
