package api.parse;

import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import api.article.Article;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

public class ParserCBC {

	public ParserCBC(Vertx vertx) {
	}
	
	public Future<Article> parse(Future<Article> article)  {
		
		return article.map(a -> parse(a));
	}
	
	private Article parse(Article article) {

		Document doc = Jsoup.parse(article.source);
		article.text = doc.getElementsByClass("story").eachText().stream().collect(Collectors.joining("\n"));
		article.summary = doc.getElementsByClass("detailSummary").eachText().stream().collect(Collectors.joining("\n"));
		article.headline = doc.getElementsByClass("detailHeadline").eachText().stream().collect(Collectors.joining("\n"));
		return article;
	}
}
