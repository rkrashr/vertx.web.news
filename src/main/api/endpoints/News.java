package api.endpoints;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class News implements Handler<RoutingContext> {

	public void handle(RoutingContext context) {
		context.request().response().end("Handled News");
	}
	
	
	
}
