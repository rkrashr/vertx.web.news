package api.parse;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import api.article.Article;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

public class ParserCnn {

	private final Vertx vertx;
	
	public ParserCnn(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public Observable<Article> parse(Article article)  {
		final ObservableFuture<Article> future = RxHelper.observableFuture();
		vertx.executeBlocking(
				blocking -> blocking.complete(parseSync(article)),
				future.toHandler());
		return future;
	}
	
	private Article parseSync(Article article) {

		Document doc = Jsoup.parse(article.source);
		
		if (!tryParseByArticleTag(doc, article))
			if (!tryParseByIdBodyText(doc, article))
				System.out.println("Failed to parse article "+article.title);
		
		return article;
	}
	
	private boolean tryParseByArticleTag(Document doc, Article article) {
		
		Elements articleElements = doc.getElementsByTag("article");
		if (articleElements.isEmpty())
			return false;
		
		Element story = articleElements.first();
		if(story==null)
			return false;
		
		
		Map<String, String> meta = story.getElementsByTag("meta")
				.select("[itemprop]")
				.stream()
				.collect(Collectors.toMap(e -> e.attributes().get("itemprop"), e -> e.attributes().get("content")));
/*
		for (String key: meta.keySet()) {
			System.out.println(String.format("%s: %s", key, meta.get(key)));
		}
*/		
		
		Element body = story.getElementById("body-text");
		if (body==null)
			return false;
		
		article.text = body.select(".zn-body__paragraph")
				.eachText()
				.stream().collect(Collectors.joining("\n"));
		
		if (meta!=null) {
			article.author = meta.getOrDefault("author", article.author);
			article.summary = meta.getOrDefault("description", article.summary);
			article.keywords = Arrays.asList(meta.getOrDefault("keywords", "").split(","));
			article.headline = meta.getOrDefault("headline", article.headline);
			article.url = meta.getOrDefault("url", article.url);
			article.published = DateTime.parse(meta.getOrDefault("datePublished", article.published.toString())).toDate();
		}
		
		return true;
	}

	private boolean tryParseByIdBodyText(Document doc, Article article) {
		Element story = doc.getElementById("storytext");
		
		if (story==null)
			return false;
		
		Map<String, String> meta = doc.getElementsByTag("meta")
				.select("[name]")
				.stream()
				.collect(Collectors.toMap(e -> e.attributes().get("name"), e -> e.attributes().get("content")));

		article.text = story.select("p")
				.eachText()
				.stream().collect(Collectors.joining("\n"));
		
		if (meta!=null) {
			if (article.author.isEmpty())
				article.author = meta.getOrDefault("author", "");
			article.summary = meta.getOrDefault("description", "");
			article.keywords = Arrays.asList(meta.getOrDefault("keywords", "").split(","));
		}
		return true;
	}
}
