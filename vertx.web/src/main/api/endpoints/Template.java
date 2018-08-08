package api.endpoints;

import org.thymeleaf.templatemode.TemplateMode;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.TemplateEngine;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

public class Template implements Handler<RoutingContext> {

	private TemplateEngine engine;
	
	public Template(Vertx vertx) {
		engine = ThymeleafTemplateEngine.create().setMode(TemplateMode.HTML);
	}

	public void handle(RoutingContext context) {
		System.out.println("rendering!");
		engine.render(context, "templates/", "home.html", (res) -> {context.request().response().end(res.otherwiseEmpty().result());});
		
	}
	
	
}
