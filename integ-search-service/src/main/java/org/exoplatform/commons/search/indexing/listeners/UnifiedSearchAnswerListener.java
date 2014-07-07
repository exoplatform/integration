package org.exoplatform.commons.search.indexing.listeners;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.commons.api.indexing.data.SearchEntry;
import org.exoplatform.commons.api.indexing.data.SearchEntryId;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.impl.AnswerEventListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Indexing with :
 * - collection : "answer"
 * - type : "question"
 * - name : question id
 */
public class UnifiedSearchAnswerListener extends AnswerEventListener {

  private static Log log = ExoLogger.getLogger(UnifiedSearchAnswerListener.class);

  private final IndexingService indexingService;
  private final FAQService faqService;

  public UnifiedSearchAnswerListener(IndexingService indexingService, FAQService faqService) {
    this.indexingService = indexingService;
    this.faqService = faqService;
  }

  @Override
  public void saveQuestion(Question question, boolean isNew) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("question", question);
      if(isNew) {
        SearchEntry searchEntry = new SearchEntry("answer", "question", question.getId(), content);
        indexingService.add(searchEntry);
      } else {
        SearchEntryId searchEntryId = new SearchEntryId("answer", "question", question.getId());
        indexingService.update(searchEntryId, content);
      }
    }
  }

  @Override
  public void saveAnswer(String questionId, Answer answer, boolean isNew) {
    saveOrUpdateQuestion(questionId, isNew);
  }

  @Override
  public void saveAnswer(String questionId, Answer[] answers, boolean isNew) {
    saveOrUpdateQuestion(questionId, isNew);
  }

  @Override
  public void saveComment(String questionId, Comment comment, String language) {
    saveOrUpdateQuestion(questionId, false);
  }

  @Override
  public void voteQuestion(String questionId) {
    saveOrUpdateQuestion(questionId, false);
  }

  @Override
  public void unVoteQuestion(String questionId) {
    saveOrUpdateQuestion(questionId, false);
  }

  @Override
  public void removeQuestion(String questionId) {
    if(indexingService != null) {
      SearchEntryId searchEntryId = new SearchEntryId("answer", "question", questionId);
      indexingService.delete(searchEntryId);
    }
  }

  @Override
  public void removeAnswer(String questionId, String answerActivityId) {
    saveOrUpdateQuestion(questionId, false);
  }

  @Override
  public void removeComment(String questionId, String commentActivityId, String questionPath) {
    saveOrUpdateQuestion(questionId, false);
  }

  private void saveOrUpdateQuestion(String questionId, boolean isNew) {
    if(indexingService != null) {
      try {
        Question question = faqService.getQuestionById(questionId);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("question", question);
        if(isNew) {
          SearchEntry searchEntry = new SearchEntry("answer", "question", question.getId(), content);
          indexingService.add(searchEntry);
        } else {
          SearchEntryId searchEntryId = new SearchEntryId("answer", "question", question.getId());
          indexingService.update(searchEntryId, content);
        }
      } catch (Exception e) {
        log.error("Error while retrieving question " + questionId, e);
      }
    }
  }
}
