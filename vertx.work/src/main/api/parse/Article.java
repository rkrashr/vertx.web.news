package api.parse;

public class Article {

	String title;
	
	public Article(String data) {
		this.title = data;
	}

	@Override
	public String toString() {
		return title;
	}
	
	
}
