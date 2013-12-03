/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */package org.exoplatform.forum.ext.faq;

import java.util.ArrayList;
import java.util.Date;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.FileAttachment;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.QuestionLanguage;
import org.exoplatform.faq.service.Watch;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.faq.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.faq.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.faq.test.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/rest/exo.faq.component.service.test.configuration.xml")
})
public abstract class FAQServiceBaseTestCase extends BaseExoTestCase {

  protected static Log    LOG         = ExoLogger.getLogger(FAQServiceBaseTestCase.class);

  protected static String USER_ROOT   = "root";

  protected static String USER_JOHN   = "john";

  protected static String USER_DEMO   = "demo";

  protected FAQService    faqService_;

  protected FAQSetting    faqSetting_ = new FAQSetting();

  public FAQServiceBaseTestCase() throws Exception {
  }

  public void setUp() throws Exception {
    begin();
    ConversationState conversionState = ConversationState.getCurrent();
    if(conversionState == null) {
      conversionState = new ConversationState(new Identity(USER_ROOT));
      ConversationState.setCurrent(conversionState);
    }
    faqService_ = (FAQService) getService(FAQService.class);
    
    //
    faqSetting_ = new FAQSetting();
    faqSetting_.setDisplayMode("both");
    faqSetting_.setOrderBy("created");
    faqSetting_.setOrderType("asc");
    faqSetting_.setSortQuestionByVote(true);
    faqSetting_.setIsAdmin("TRUE");
    faqSetting_.setEmailMoveQuestion("content email move question");
    faqSetting_.setEmailSettingSubject("Send notify watched");
    faqSetting_.setEmailSettingContent("Question content: &questionContent_ <br/>Response: &questionResponse_ <br/> link: &questionLink_");
  }

  public void tearDown() throws Exception {
    end();
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
  
  protected Category createCategory(String categoryName, int  index) {
    Date date = new Date();
    Category category = new Category();
    category.setName(categoryName);
    category.setDescription("Description");
    category.setModerateQuestions(true);
    category.setModerateAnswers(true);
    category.setViewAuthorInfor(true);
    category.setModerators(new String[] { USER_ROOT });
    category.setCreatedDate(date);
    category.setUserPrivate(new String[] { "" });
    category.setIndex(index);
    category.setView(true);
    return category;
  }

  protected Question createQuestion(String cateId) throws Exception {
    Question question = new Question();
    question.setLanguage("English");
    question.setQuestion("This question for test");
    question.setDetail("Add new question 1");
    question.setAuthor(USER_ROOT);
    question.setEmail("exotesting@exoplatform.com");
    question.setActivated(true);
    question.setApproved(true);
    question.setCreatedDate(new Date());
    question.setCategoryId(cateId);
    question.setCategoryPath(cateId);
    question.setRelations(new String[] {});
    question.setAttachMent(new ArrayList<FileAttachment>());
    question.setAnswers(new Answer[] {});
    question.setComments(new Comment[] {});
    question.setUsersVote(new String[] {});
    question.setMarkVote(0.0);
    question.setUsersWatch(new String[] {});
    question.setEmailsWatch(new String[] {});
    question.setTopicIdDiscuss(null);
    return question;
  }
  
  protected Answer createAnswer(String user, String content) {
    Answer answer = new Answer();
    answer.setActivateAnswers(true);
    answer.setApprovedAnswers(true);
    answer.setDateResponse(new Date());
    answer.setMarksVoteAnswer(0);
    answer.setMarkVotes(0);
    answer.setNew(true);
    answer.setPostId(null);
    answer.setResponseBy(user);
    answer.setResponses(content);
    answer.setUsersVoteAnswer(null);
    answer.setLanguage("English");
    return answer;
  }

  protected Comment createComment(String user, String content) {
    Comment comment = new Comment();
    comment.setCommentBy(user);
    comment.setComments(content);
    comment.setDateComment(new Date());
    comment.setNew(true);
    comment.setPostId(null);
    comment.setFullName(user + " " + user);
    return comment;
  }

  protected QuestionLanguage createQuestionLanguage(String language) {
    QuestionLanguage questionLanguage = new QuestionLanguage();
    questionLanguage.setAnswers(null);
    questionLanguage.setComments(null);
    questionLanguage.setDetail("detail for language " + language);
    questionLanguage.setLanguage(language);
    questionLanguage.setQuestion("test question for language " + language);
    return questionLanguage;
  }

  protected Watch createNewWatch(String user, String mail) {
    Watch watch = new Watch();
    watch.setUser(user);
    watch.setEmails(mail);
    return watch;
  }
}
