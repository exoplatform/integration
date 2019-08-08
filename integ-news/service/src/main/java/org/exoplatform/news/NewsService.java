package org.exoplatform.news;

import org.exoplatform.news.model.News;

import javax.jcr.RepositoryException;

public interface NewsService {
  News createNews(News news) throws Exception;

  News getNews(String id) throws Exception;

  void updateNews(News news) throws Exception;
}
