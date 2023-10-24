package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import nl.rrd.senseeact.service.SenSeeActContext;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Controller
@SecurityScheme(
	name="X-Auth-Token",
	type=SecuritySchemeType.APIKEY,
	in=SecuritySchemeIn.HEADER
)
public class SwaggerController {
	private static final Object LOCK = new Object();

	@RequestMapping("/")
	public RedirectView redirectRoot() {
		return new RedirectView(SenSeeActContext.getBaseUrl() + "/swagger-ui.html");
	}

	@Bean
	public OpenAPI api() {
		String url = SenSeeActContext.getBaseUrl() + "/v" +
				SenSeeActContext.getCurrentVersion();
		return new OpenAPI()
				.addServersItem(new Server().url(url))
				.addSecurityItem(new SecurityRequirement().addList(
						"X-Auth-Token"));
	}

	@Bean
	public OpenApiCustomizer openApiCustomiser() {
		return this::customiseApi;
	}

	private void customiseApi(OpenAPI api) {
		transformPaths(api.getPaths());
	}

	private void transformPaths(Paths paths) {
		synchronized (LOCK) {
			List<String> keys = new ArrayList<>(paths.keySet());
			String versionPath = "/v{version}";
			Map<String,PathItem> unorderedMap = new HashMap<>();
			for (String key : keys) {
				PathItem item = paths.remove(key);
				if (!key.startsWith(versionPath))
					continue;
				String operationPath = key.substring(versionPath.length());
				unorderedMap.put(operationPath, item);
			}
			if (unorderedMap.isEmpty())
				return;
			List<String> orderedKeys = new ArrayList<>(unorderedMap.keySet());
			orderedKeys.sort(null);
			for (String key : orderedKeys) {
				paths.put(key, unorderedMap.get(key));
			}
		}
	}
}
