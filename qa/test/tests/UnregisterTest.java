package tests;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import models.*;
import play.test.*;


public class UnregisterTest extends UnitTest {
	
	private User jack;
	private User john;
	private User bill;
	private User kate;
	private User sahra;
	private User michael;
	private Question question;
	private Answer answer;
	private Comment questionComment;
	private Comment answerComment;
	private Vote questionVote;
	private Vote answerVote;

	@Before
	public void setUp() {
		this.jack = new User("Jack");
		this.john = new User("John");
		this.bill = new User("Bill");
		this.kate = new User("Kate");
		this.sahra = new User("Sahra");
		this.michael = new User("Michael");
		this.question = new Question(this.jack, "Why did the chicken cross the road?");
		this.answer = this.question.answer(this.john, "To get to the other side.");
		this.questionVote = this.question.voteUp(this.kate);
		this.answerVote = this.answer.voteDown(this.bill);
		this.questionComment = this.question.comment(this.michael, "Strange question");
		this.answerComment = this.answer.comment(this.sahra, "Good answer");
	}
	
	@Test
	public void shouldUnregisterAnswer() {
		assertTrue(this.question.hasAnswer(this.answer));
		this.john.delete();
		assertFalse(this.question.hasAnswer(this.answer));
	}
	
	@Test
	public void shouldUnregisterAnswersToQuestion() {
		assertTrue(this.john.hasItem(this.answer));
		this.jack.delete();
		assertFalse(this.john.hasItem(this.answer));
	}
	
	@Test
	public void shouldUnregisterVotesOfUser() {
		assertEquals(this.question.upVotes(), 1);
		this.kate.delete();
		assertEquals(this.question.upVotes(), 0);
		
		assertEquals(this.answer.downVotes(), 1);
		this.bill.delete();
		assertEquals(this.answer.downVotes(), 0);
	}
	
	@Test
	public void shouldUnregisterVotesOfEntry() {
		assertTrue(this.kate.hasItem(this.questionVote));
		assertTrue(this.bill.hasItem(this.answerVote));
		this.jack.delete();
		assertFalse(this.kate.hasItem(this.questionVote));
		assertFalse(this.bill.hasItem(this.answerVote));
		
	}
	
	@Test
	public void shouldUnregisterCommentsToQuestion() {
		assertTrue(question.hasComment(questionComment));
		this.michael.delete();
		assertFalse(question.hasComment(questionComment));
	}
	
	@Test
	public void shouldUnregisterCommentsToAnswer() {
		assertTrue(answer.hasComment(answerComment));
		this.sahra.delete();
		assertFalse(answer.hasComment(answerComment));
	}
}