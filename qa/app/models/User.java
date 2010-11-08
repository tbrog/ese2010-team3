package models;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import models.database.Database;
import models.helpers.Filter;
import models.helpers.IObservable;
import models.helpers.IObserver;
import models.helpers.Mapper;
import models.helpers.Tools;

/**
 * A user with a name. Can contain {@link Item}s i.e. {@link Question}s,
 * {@link Answer}s, {@link Comment}s and {@link Vote}s. When deleted, the
 * <code>User</code> requests all his {@link Item}s to delete themselves.
 * 
 * @author Simon Marti
 * @author Mirco Kocher
 * 
 */
public class User implements IObserver {

	private final String name;
	private final String password;
	private String email;
	private final HashSet<Item> items;
	private String fullname;
	protected Date dateOfBirth;
	private String website;
	private String profession;
	private String employer;
	private String biography;
	
	/**
	 * Creates a <code>User</code> with a given name.
	 * 
	 * @param name the name of the <code>User</code>
	 */
	public User(String name, String password) {
		this.name = name;
		this.password = Tools.encrypt(password);
		this.items = new HashSet<Item>();
	}

	/**
	 * Gets the name of the <code>User</code>.
	 * 
	 * @return name of the <code>User</code>
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Encrypt the password and check if it is the same as the stored one.
	 * 
	 * @param passwort
	 * @return true if the password is right
	 */
	public boolean checkPW(String password) {
		return this.password.equals(Tools.encrypt(password));
	}

	/**
	 * Registers an {@link Item} which should be deleted in case the
	 * <code>User</code> gets deleted.
	 * 
	 * @param item the {@link Item} to register
	 */
	public void registerItem(Item item) {
		this.items.add(item);
	}

	/**
	 * Causes the <code>User</code> to delete all his {@link Item}s.
	 */
	public void delete() {
		// operate on a clone to prevent a ConcurrentModificationException
		HashSet<Item> clone = (HashSet<Item>) this.items.clone();
		for (Item item : clone)
			item.unregister();
		this.items.clear();
		Database.get().users().remove(this.name);
	}

	/**
	 * Unregisters an {@link Item} which has been deleted.
	 * 
	 * @param item the {@link Item} to unregister
	 */
	public void unregister(Item item) {
		this.items.remove(item);
	}

	/**
	 * Checks if an {@link Item} is registered and therefore owned by a
	 * <code>User</code>.
	 * 
	 * @param item the {@link Item}to check
	 * @return true if the {@link Item} is registered
	 */
	public boolean hasItem(Item item) {
		return this.items.contains(item);
	}

	/**
	 * The amount of Comments, Answers and Questions the <code>User</code> has
	 * posted in the last 60 Minutes.
	 * 
	 * @return The amount of Comments, Answers and Questions for this
	 *         <code>User</code> in this Hour.
	 */
	public int howManyItemsPerHour() {
		Date now = SystemInformation.get().now();
		int i = 0;
		for (Item item : this.items) {
			if ((now.getTime() - item.timestamp().getTime()) <= 60 * 60 * 1000) {
				i++;
			}
		}
		return i;
	}

	/**
	 * The <code>User</code> is a Cheater if over 50% of his votes is for the
	 * same <code>User</code>.
	 * 
	 * @return True if the <code>User</code> is supporting somebody.
	 */
	public boolean isMaybeCheater() {
		HashMap<User, Integer> votesForUser = new HashMap<User, Integer>();
		for (Item item : this.items) {
			if (item instanceof Vote && ((Vote) item).up()) {
				Integer count = votesForUser.get(item.owner());
				if (count == null)
					count = 0;
				votesForUser.put(item.owner(), count + 1);
			}
		}

	    if (votesForUser.isEmpty())
			return false;

	    Integer maxCount = Collections.max(votesForUser.values());
		return maxCount > 3 && maxCount / votesForUser.size() > 0.5;
	}

	/**
	 * Anonymizes all questions, answers and comments by this user.
	 * 
	 * @param doAnswers - whether to anonymize this user's answers as well
	 * @param doComments - whether to anonymize this user's comments as well
	 */
	public void anonymize(boolean doAnswers, boolean doComments) {
		// operate on a clone to prevent a ConcurrentModificationException
		HashSet<Item> clone = (HashSet<Item>) this.items.clone();
		for (Item item : clone) {
			if (item instanceof Question || doAnswers && item instanceof Answer
					|| doComments && item instanceof Comment) {
				((Entry) item).anonymize();
				this.items.remove(item);
			}
		}
	}

	/**
	 * The <code>User</code> is a Spammer if he posts more than 30 comments,
	 * answers or questions in the last hour.
	 * 
	 * @return True if the <code>User</code> is a Spammer.
	 */
	public boolean isSpammer() {
		int number = this.howManyItemsPerHour();
		if (number >= 60) {
			return true;
		}
		return false;
	}

	/**
	 * Set the <code>User</code> as a Cheater if he spams the Site or supports
	 * somebody.
	 * 
	 */
	public boolean isCheating() {
		return (isSpammer() || isMaybeCheater());
	}

	/**
	 * Calculates the age of the <code>User</code> in years.
	 * 
	 * @return age of the <code>User</code>
	 */
	private int age() {
		Date now = SystemInformation.get().now();
		if (dateOfBirth != null) {
			long age = now.getTime() - dateOfBirth.getTime();
			return (int) (age / ((long) 1000 * 3600 * 24 * 365));
		} else
			return (0);
	}

	/* Getter and Setter for profile data */

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return this.email;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getFullname() {
		return this.fullname;
	}

	public void setDateOfBirth(String birthday) throws ParseException {
		this.dateOfBirth = Tools.stringToDate(birthday);
	}

	public String getDateOfBirth() {
		return Tools.dateToString(dateOfBirth);
	}

	public int getAge() {
		return this.age();
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getWebsite() {
		return this.website;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public String getProfession() {
		return this.profession;
	}

	public void setEmployer(String employer) {
		this.employer = employer;
	}

	public String getEmployer() {
		return this.employer;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getBiography() {
		return this.biography;
	}

	public String getSHA1Password() {
		return this.password;
	}
	/**
	 * Start observing changes for an entry (e.g. new answers to a question).
	 * 
	 * @param what the entry to watch
	 */
	public void startObserving(IObservable what) {
		what.addObserver(this);
	}

	/**
	 * Checks if a specific entry is being observed for changes.
	 * 
	 * @param what the entry to check
	 */
	public boolean isObserving(IObservable what) {
		return what.hasObserver(this);
	}
	
	/**
	 * Stop observing changes for an entry (e.g. new answers to a question).
	 * 
	 * @param what the entry to unwatch
	 */
	public void stopObserving(IObservable what) {
		what.removeObserver(this);
	}

	/**
	 * @see models.IObserver#observe(models.IObservable, java.lang.Object)
	 */
	public void observe(IObservable o, Object arg) {
		if (o instanceof Question && arg instanceof Answer
				&& ((Answer) arg).owner() != this)
			new Notification(this, (Answer) arg);
	}

	/**
	 * Get a List of the last three <code>Question</code>s of this <code>User</code>.
	 * 
	 * @return List<Question> The last three <code>Question</code>s of this <code>User</code>
	 */
	public List<Question> getRecentQuestions() {
		return getRecentItemsByType(Question.class);
	}

	/**
	 * Get a List of the last three <code>Answer</code>s of this <code>User</code>.
	 * 
	 * @return List<Answer> The last three <code>Answer</code>s of this <code>User</code>
	 */
	public List<Answer> getRecentAnswers() {
		return getRecentItemsByType(Answer.class);
	}
	
	/**
	 * Get a List of the last three <code>Comment</code>s of this <code>User</code>.
	 * 
	 * @return List<Comment> The last three <code>Comment</code>s of this <code>User</code>
	 */
	public List<Comment> getRecentComments() {
		return getRecentItemsByType(Comment.class);
	}

	/**
	 * Get a List of the last three <code>Items</code>s of type T of this <code>User</code>.
	 * 
	 * @return List<Item> The last three <code>Item</code>s of this <code>User</code>
	 */
	protected List getRecentItemsByType(Class type) {
		List recentItems = this.getItemsByType(type);
		Collections.sort(recentItems, new Comparator<Item>() {
			public int compare(Item i1, Item i2) {
				return i1.timestamp().compareTo(i2.timestamp());
			}
		});
		if (recentItems.size() > 3)
			return recentItems.subList(0, 3);
		return recentItems;
	}

	/**
	 * Get a sorted ArrayList of all <code>Questions</code>s of this <code>User</code>.
	 * 
	 * @return ArrayList<Question> All questions of this <code>User</code>
	 */
	public List<Question> getQuestions() {
		return this.getItemsByType(Question.class);
	}

	/**
	 * Get a sorted ArrayList of all <code>Answer</code>s of this <code>User</code>.
	 * 
	 * @return ArrayList<Answer> All <code>Answer</code>s of this <code>User</code>
	 */
	public List<Answer> getAnswers() {
		return this.getItemsByType(Answer.class);
	}
	
	/**
	 * Get a sorted ArrayList of all <code>Comment</code>s of this <code>User</code>
	 * 
	 * @return ArrayList<Comment> All <code>Comments</code>s of this <code>User</code>
	 */
	public List<Comment> getComments() {
		return this.getItemsByType(Comment.class);
	}

	/**
	 * Get an ArrayList of all best rated answers
	 * 
	 * @return List<Answer> All best rated answers
	 */
	public List<Answer> bestAnswers() {
		return Mapper.filter(this.getAnswers(), new Filter<Answer, Boolean>() {
			public Boolean visit(Answer a) {
				return a.isBestAnswer();
			}
		});
	}

	/**
	 * Get an ArrayList of all highRated answers
	 * 
	 * @return List<Answer> All high rated answers
	 */
	public List<Answer> highRatedAnswers() {
		return Mapper.filter(this.getAnswers(), new Filter<Answer, Boolean>() {
			public Boolean visit(Answer a) {
				return a.isHighRated();
			}
		});
	}

	/**
	 * Get an ArrayList of all notifications of this user, sorted most-recent
	 * one first and optionally fulfilling one filter criterion.
	 * 
	 * @param filter
	 *            an optional name of a filter method (e.g. "isNew")
	 * @return ArrayList<Notification> All notifications of this user
	 */
	protected List<Notification> getAllNotifications() {
		ArrayList<Notification> result = new ArrayList<Notification>();
		/*
		 * Hack: remove all notifications to deleted answers
		 * 
		 * unfortunately, there's currently no other way to achieve this, as
		 * there is no global list of all existing notifications nor an easy way
		 * to register all users for observing the deletion of answers (because
		 * there's no global list of all existing users, either)
		 */
		List<Notification> notifications = this
				.getItemsByType(Notification.class);
		for (Notification n : notifications) {
			if (n.getAbout() instanceof Answer) {
				Answer answer = (Answer) n.getAbout();
				if (answer.getQuestion() != null)
					result.add(n);
				else
					n.unregister();
			}
		}
		return result;
	}

	/**
	 * Get an ArrayList of all notifications of this user, sorted most-recent
	 * one first.
	 * 
	 * @return ArrayList<Notification> All notifications of this user
	 */
	public List<Notification> getNotifications() {
		return this.getAllNotifications();
	}

	/**
	 * Get an ArrayList of all unread notifications of this user
	 * 
	 * @return the unread notifications
	 */
	public List<Notification> getNewNotifications() {
		return Mapper.filter(this.getAllNotifications(), new Filter<Notification, Boolean>() {
			public Boolean visit(Notification n) {
				return n.isNew();
			}
		});
	}

	/**
	 * Gets the most recent unread notification, if there is any very recent one
	 * 
	 * @return a very recent notification (or null, if there isn't any)
	 */
	public Notification getVeryRecentNewNotification() {
		for (Notification n : this.getNewNotifications())
			if (n.isVeryRecent())
				return n;
		return null;
	}

	/**
	 * Gets a notification by its id value.
	 * 
	 * NOTE: slightly hacky since we don't track notifications in a separate
	 * IDTable but in this.items like everything else - this should get fixed
	 * once we migrate to using a real DB.
	 * 
	 * @param id
	 *            the notification's id
	 * @return a notification with the given id
	 */
	public Notification getNotification(int id) {
		for (Notification n : this.getNotifications())
			if (n.getID() == id)
				return n;
		return null;
	}

	/**
	 * Get an ArrayList of all items of this user being an instance of a
	 * specific type.
	 * 
	 * @param type
	 *            the type
	 * @return ArrayList All type-items of this user
	 */
	protected List getItemsByType(Class type) {
		List items = new ArrayList();
		for (Item item : this.items)
			if (type.isInstance(item))
				items.add(item);
		Collections.sort(items);
		return items;
	}

	/**
	 * Checks at Sign Up if the entered username is available. This way we can
	 * avoid having two User called "SoMeThinG" and "SoMetHinG" which might be
	 * hard to distinguish
	 * 
	 * @param username
	 * @return true if the username is available.
	 */
	public static boolean isAvailable(String username) {
		return (Database.get().users().get(username.toLowerCase()) == null);
	}

	public static User get(String name) {
		return Database.get().users().get(name.toLowerCase());
	}

}
