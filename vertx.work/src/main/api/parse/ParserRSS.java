package api.parse;

import com.rometools.rome.feed.synd.SyndEntry;

import api.article.Article;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

public class ParserRSS {

	WebClient client;
	
	public ParserRSS(WebClient client) {
		this.client = client;
	}
	
	public Single<Article> parse(SyndEntry entry)  {
		Article article = new Article(entry);
		return article.fetch(client);
	}
	
	
}
