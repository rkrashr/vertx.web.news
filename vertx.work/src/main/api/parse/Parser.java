package api.parse;

import com.rometools.rome.feed.synd.SyndEntry;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class Parser {

	Vertx vertx;
	
	public Parser(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public Future<Article> parse(SyndEntry entry)  {
		
	
		Future<Article> future1 = Future.future();
		future1.complete(new Article(entry));
		Future<Article> future = future1.compose(a -> a.fetch(vertx));
		return future;
	}
	
}
