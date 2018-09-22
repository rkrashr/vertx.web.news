package api.parse;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import api.article.Article;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ParserReuters {

	public ParserReuters(Vertx vertx) {
	}
	
	public Future<Article> parse(Future<Article> article)  {
		
		return article.map(a -> parse(a));
	}
	
	private Article parse(Article article) {

		Document doc = Jsoup.parse(article.source);
		Map<String, String> meta = doc.getElementsByTag("meta")
				.select("[name]")
				.stream()
				.collect(Collectors.toMap(e -> e.attributes().get("name"), e -> e.attributes().get("content")));
		
		article.text = doc.getElementsByClass("StandardArticleBody_body").select("p").eachText().stream().collect(Collectors.joining("\n"));
		article.summary = meta.get("description");
		article.keywords = Arrays.asList(meta.get("keywords").split(","));
		return article;
	}
	
}
