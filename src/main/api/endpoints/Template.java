package api.endpoints;

import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

public class Template implements Endpoint {

	private final Vertx vertx;
	private ThymeleafTemplateEngine engine;
	
	public Template(Vertx vertx) {
		this.vertx = vertx;
	}
	
	@Override
	public Router route() {
		
		engine = ThymeleafTemplateEngine.create().setMode(TemplateMode.HTML);
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setTemplateMode("XHTML");
		resolver.setSuffix(".html");
		TemplateHandler handler = TemplateHandler.create(engine);
		
		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/tmp/*").handler(handler);

		return router;
	}
	
	void handle(RoutingContext context) {
		System.out.println("rendering!");
		engine.render(context, "templates/", "home.html", (res) -> {context.request().response().end("rendered");});
		
	}
	
	
}
