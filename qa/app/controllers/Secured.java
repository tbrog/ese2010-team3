package controllers;

import java.text.ParseException;

import models.Answer;
import models.Comment;
import models.Notification;
import models.Question;
import models.User;
import models.database.Database;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

@With(Secure.class)
public class Secured extends Controller {
	public static void newQuestion(@Required String content, String tags) {
		if (!validation.hasErrors()) {
			User user = Session.get().currentUser();
			Question question = Database.get().questions().add(Session.get().currentUser(), content);
			Question question = Question.register(user, content);
			question.setTagString(tags);
			user.startObserving(question);
			user.addRecentQuestions(question);
			Application.question(question.id());
		} else {
			Application.index();
		}
	}

	public static void newAnswer(int questionId, @Required String content) {
		if (!validation.hasErrors() && Database.get().questions().get(questionId) != null) {
			Answer answer = Database.get().questions().get(questionId)
			Session.get().currentUser().addRecentAnswers(answer);
			Application.question(questionId);
		} else {
			Application.index();
		}
	}

	public static void newCommentQuestion(int questionId,
			@Required String content) {
		if (!validation.hasErrors() && Database.get().questions().get(questionId) != null) {
			Comment comment = Database.get().questions().get(questionId).comment(Session.get().currentUser(), content);
			Session.get().currentUser().addRecentComments(comment);
			Application.commentQuestion(questionId);
		}
	}

	public static void newCommentAnswer(int questionId, int answerId,
			@Required String content) {
		Question question = Database.get().questions().get(questionId);
		Answer answer = question.getAnswer(answerId);

		if (!validation.hasErrors() && answer != null) {
			Comment comment = answer.comment(Session.get().currentUser(), content);
			Session.get().currentUser().addRecentComments(comment);
			Application.commentAnswer(questionId, answerId);
		}
	}

	public static void voteQuestionUp(int id) {
		if (Database.get().questions().get(id) != null) {
			Database.get().questions().get(id).voteUp(Session.get().currentUser());
			if (!redirectToCallingPage())
				Application.question(id);
		} else {
			Application.index();
		}
	}

	public static void voteQuestionDown(int id) {
		if (Database.get().questions().get(id) != null) {
			Database.get().questions().get(id).voteDown(Session.get().currentUser());
			if (!redirectToCallingPage())
				Application.question(id);
		} else {
			Application.index();
		}
	}

	public static void voteAnswerUp(int question, int id) {
		if (Database.get().questions().get(question) != null
				&& Database.get().questions().get(question).getAnswer(id) != null) {
			Database.get().questions().get(question).getAnswer(id).voteUp(Session.get().currentUser());
			Application.question(question);
		} else {
			Application.index();
		}
	}

	public static void voteAnswerDown(int question, int id) {
		if (Database.get().questions().get(question) != null
				&& Database.get().questions().get(question).getAnswer(id) != null) {
			Database.get().questions().get(question).getAnswer(id).voteDown(Session.get().currentUser());
			Application.question(question);
		} else {
			Application.index();
		}
	}

	public static void deleteQuestion(int questionId) {
		Question question = Database.get().questions().get(questionId);
		question.unregister();
		Application.index();
	}

	public static void deleteAnswer(int answerId, int questionId) {
		Question question = Database.get().questions().get(questionId);
		Answer answer = question.getAnswer(answerId);
		answer.unregister();
		Application.question(questionId);
	}

	public static void deleteCommentQuestion(int commentId, int questionId) {
		Question question = Database.get().questions().get(questionId);
		Comment comment = question.getComment(commentId);
		question.unregister(comment);
		Application.commentQuestion(questionId);
	}

	public static void deleteCommentAnswer(int commentId, int questionId,
			int answerId) {
		Question question = Database.get().questions().get(questionId);
		Answer answer = question.getAnswer(answerId);
		Comment comment = answer.getComment(commentId);
		answer.unregister(comment);
		Application.commentAnswer(questionId, answerId);
	}

	public static void deleteUser(String name) throws Throwable {
		User user = Database.get().users().get(name);
		if (hasPermissionToDelete(Session.get().currentUser(), user)) {
			boolean deleteSelf = name.equals(Session.get().currentUser().name());
			user.delete();
			if (deleteSelf)
				Secure.logout();
		}
		Application.index();
	}

	public static void anonymizeUser(String name) throws Throwable {
		User user = Database.get().users().get(name);
		if (hasPermissionToDelete(Session.get().currentUser(), user))
			user.anonymize(true, false);
		deleteUser(name);
	}

	public static void selectBestAnswer(int questionId, int answerId) {
		Question question = Database.get().questions().get(questionId);
		Answer answer = question.getAnswer(answerId);
		question.setBestAnswer(answer);
		Application.question(questionId);
	}

	private static boolean hasPermissionToDelete(User currentUser, User user) {
		return currentUser.name().equals(user.name());
	}

	private static boolean redirectToCallingPage() {
		Http.Header referer = request.headers.get("referer");
		if (referer == null)
			return false;
		redirect(referer.value());
		return true;
	}
	

	public static void saveProfile(String name, String email, String fullname,
			String birthday, String website, String profession,
			String employer, String biography) throws ParseException {

		User user = Session.get().currentUser();
		if (email != null)
			user.setEmail(email);
		if (fullname != null)
			user.setFullname(fullname);
		if (birthday != null)
			user.setDateOfBirth(birthday);
		if (website != null)
			user.setWebsite(website);
		if (profession != null)
			user.setProfession(profession);
		if (employer != null)
			user.setEmployer(employer);
		if (biography != null)
			user.setBiography(biography);
		Application.showprofile(user.name());
	}

	public static void updateTags(int id, String tags) {
		Question question = Database.get().questions().get(id);
		User user = Session.get().currentUser();
		if (question != null && user == question.owner())
			question.setTagString(tags);
		Application.question(id);
	}

	public static void watchQuestion(int id) {
		Question question = Question.get(id);
		User user = Session.get().currentUser();
		if (question != null)
			user.startObserving(question);
		Application.question(id);
	}

	public static void unwatchQuestion(int id) {
		Question question = Question.get(id);
		User user = Session.get().currentUser();
		if (question != null)
			user.stopObserving(question);
		Application.question(id);
	}

	public static void followNotification(int id) {
		User user = Session.get().currentUser();
		Notification n = user.getNotification(id);
		if (n != null)
			n.unsetNew();
		if (n != null && n.getAbout() instanceof Answer)
			Application.question(((Answer) n.getAbout()).question().id());
		else if (!redirectToCallingPage())
			Application.notifications();
	}

	public static void clearNewNotifications() {
		User user = Session.get().currentUser();
		for (Notification n : user.getNewNotifications())
			n.unsetNew();
		Application.notifications();
	}
	public static void deleteNotification(int id) {
		User user = Session.get().currentUser();
		Notification n = user.getNotification(id);
		if (n != null)
			n.unregister();
		Application.notifications();
	}
}
