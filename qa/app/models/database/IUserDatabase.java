package models.database;

import java.util.Collection;

import models.IMailbox;
import models.User;

/**
 * A user database is both a factory for creating new users and a container
 * tracking all the created users and their privileges (currently: moderator
 * status).
 */
public interface IUserDatabase {

	/**
	 * Get the <code>User</code> with the given name.
	 * 
	 * @param name
	 *            unique user name
	 * @return a <code>User</code> or null if the given name doesn't exist.
	 */
	public User get(String name);

	/**
	 * Creates a <code>User</code> with the given credentials. Asserts that
	 * <code>needsSignUp(username)</code> before executing this and
	 * <code>register(username,password,email) == get(name)</code>
	 * 
	 * @param username
	 *            unique identifier
	 * @param password
	 *            the password
	 * @param email
	 *            the email
	 * @return The user with this credentials
	 */
	public User register(String username, String password, String email);

	/**
	 * Checks at Sign Up if the entered username is available. This way we can
	 * avoid having two User called "SoMeThinG" and "SoMetHinG" which might be
	 * hard to distinguish
	 * 
	 * @param username
	 *            the username
	 * @return True iff there is no <code>User</code> of that name
	 */
	public boolean isAvailable(String username);

	/**
	 * A collection of all registered Users in the system.
	 * 
	 * @return the collection
	 */

	public Collection<User> all();

	/**
	 * The number of all registered Users.
	 * 
	 * @return int n >= 0
	 */

	public int count();

	/**
	 * Deletes every user from the DB.
	 * 
	 * @param keepAdmins
	 *            whether to not delete administrators (in case there's no other
	 *            way to re-gain enough access to the system for creating new
	 *            users)
	 */

	public void clear(boolean keepAdmins);

	/**
	 * Get moderating crowd.
	 * 
	 * @return the collection
	 */
	public Collection<User> allModerators();

	/**
	 * Returns the mailbox that every moderator can read.
	 * 
	 * @return the moderator mailbox
	 */

	public IMailbox getModeratorMailbox();
}
