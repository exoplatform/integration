package org.exoplatform.forum.ext.impl;

import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.social.core.space.SpaceUtils;

public class ActivityUtils {

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
    if (categoryId.indexOf(Utils.CATE_SPACE_ID_PREFIX) < 0) 
      return "";
    String prettyname = categoryId.split(Utils.CATE_SPACE_ID_PREFIX)[1];
    spaceGroupId = SpaceUtils.SPACE_GROUP + CommonUtils.SLASH + prettyname;
    return spaceGroupId;
  }
}
