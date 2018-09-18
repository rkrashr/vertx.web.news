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
		
	
		Future<Article> future = Future.future();
		Article article = new Article(entry);
		article.fetch(vertx);
		return future;
	}
	
}
