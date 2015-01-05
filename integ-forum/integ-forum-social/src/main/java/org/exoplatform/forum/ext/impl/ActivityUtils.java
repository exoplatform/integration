package org.exoplatform.forum.ext.impl;

import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ActivityUtils {
  private static final Log LOG = ExoLogger.getLogger(ActivityUtils.class);

  public static String getQuestionRate(Question question) {
    if (question == null) {
      return "0";
    }
    return String.valueOf(question.getMarkVote());
  }
  
  public static String getNbOfComments(Question question) {
    if (question == null) {
      return "0";
    }
    int nbComments = (question.getComments() != null) ? question.getComments().length : 0;
    return String.valueOf(nbComments);
  }
  
  public static String getNbOfAnswers(Question question) {
    if (question == null) {
      return "0";
    }
    int nbAnswers = (question.getAnswers() != null) ? question.getAnswers().length : 0;
    return String.valueOf(nbAnswers);
  }
  
  public static String processContent(String content) {
    content = CommonUtils.processBBCode(CommonUtils.decodeSpecialCharToHTMLnumberIgnore(content));
    content = ForumActivityBuilder.getFourFirstLines(content);
    return content;
  }
  
  public static String getSpaceGroupId(String categoryId) {
    String spaceGroupId = "";
    String spaceCateId = categoryId;
    try {
      // Get category id in case of sub-category in a space
      FAQService faqService = CommonsUtils.getService(FAQService.class);
      Category category = faqService.getCategoryById(categoryId);
      String categoryPath = category.getPath();
      if (categoryPath.indexOf(Utils.CATE_SPACE_ID_PREFIX) >= 0){
        spaceCateId = categoryPath.split("/")[1];
      }
    } catch (Exception e) {
      LOG.warn("Get category id failed.", e);
    }
    if (spaceCateId.indexOf(Utils.CATE_SPACE_ID_PREFIX) < 0) 
      return "";
    String prettyname = spaceCateId.split(Utils.CATE_SPACE_ID_PREFIX)[1];
    spaceGroupId = SpaceUtils.SPACE_GROUP + CommonUtils.SLASH + prettyname;
    return spaceGroupId;
  }
}
