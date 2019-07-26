package org.exoplatform.news.webui.activity;

import org.exoplatform.news.model.News;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "war:/groovy/news/webui/activity/UINewsActivity.gtmpl", events = {
    @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
    @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
    @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class) })
public class UINewsActivity extends BaseUIActivity {
  public static final String ACTIVITY_TYPE = "news";

  private News news;

  public News getNews() {
    return news;
  }

  public void setNews(News news) {
    this.news = news;
  }
}
