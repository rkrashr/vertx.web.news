package api.parse;

import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import api.article.Article;
import io.vertx.core.Vertx;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;


public class ParserCBC {

	private final Vertx vertx;
	
	public ParserCBC(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public ObservableFuture<Article> parse(ObservableFuture<Article> article)  {

		ObservableFuture<Article> future = RxHelper.observableFuture();
		article.map(doc -> {
			vertx.executeBlocking(blocking -> blocking.complete(parse(doc)),
			future.toHandler());
			return future;
		});
		
		return future;
	}
	
	private Article parse(Article article) {

		Document doc = Jsoup.parse(article.source);
		article.text = Optional
				.ofNullable(doc.getElementsByClass("story"))
				.orElse(new Elements())
				.eachText()
				.stream()
				.collect(Collectors.joining("\n"));
		article.summary = doc.getElementsByClass("detailSummary").eachText().stream().collect(Collectors.joining("\n"));
		article.headline = doc.getElementsByClass("detailHeadline").eachText().stream().collect(Collectors.joining("\n"));
		return article;
	}
}
