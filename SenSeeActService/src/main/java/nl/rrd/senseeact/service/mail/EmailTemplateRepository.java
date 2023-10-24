package nl.rrd.senseeact.service.mail;

import nl.rrd.utils.AppComponent;

@AppComponent
public interface EmailTemplateRepository {
	/**
	 * Returns the template collection for a reset password email. The templates
	 * take the following parameters for the HTML content:
	 *
	 * <p><ul>
	 * <li>code: the reset code</li>
	 * </ul></p>
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getResetPasswordTemplates();

	/**
	 * Returns the template collection for a new user email, where the user
	 * is asked to confirm their email address. The templates take the following
	 * parameters for the HTML content:
	 *
	 * <p><ul>
	 * <li>code: the confirmation code</li>
	 * </ul></p>
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getNewUserTemplates();

	/**
	 * Returns the template collection for email that is sent when a user
	 * changes their email address. This mail is sent to the old address if that
	 * address was verified, and it will mention the new address. The templates
	 * take the following parameters for the HTML content:
	 *
	 * <p><ul>
	 * <li>new_email: the new email address</li>
	 * </ul></p>
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getEmailChangedOldVerifiedTemplates();

	/**
	 * Returns the template collection for email that is sent when a user
	 * changes their email address. This mail is sent to the old address if that
	 * address was not verified. Because the old address was not verified, the
	 * new address will not be mentioned. The templates do not need parameters
	 * for the HTML content.
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getEmailChangedOldUnverifiedTemplates();

	/**
	 * Returns the template collection for email that is sent when a user
	 * changes their email address, while the old address was verified. This
	 * mail is sent to the new address. The user is asked to confirm the new
	 * address. The templates take the following parameters for the HTML
	 * content:
	 *
	 * <p><ul>
	 * <li>old_email: the old email address</li>
	 * <li>new_email: the new email address</li>
	 * <li>code: the confirmation code</li>
	 * </ul></p>
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getEmailChangedNewVerifiedTemplates();

	/**
	 * Returns the template collection for email that is sent when a user
	 * changes their email address, while the old address was not verified. This
	 * mail is sent to the new address. The user is asked to confirm the new
	 * address. The templates take the following parameters for the HTML
	 * content:
	 *
	 * <p><ul>
	 * <li>old_email: the old email address</li>
	 * <li>new_email: the new email address</li>
	 * <li>code: the confirmation code</li>
	 * </ul></p>
	 *
	 * @return the template collection
	 */
	EmailTemplateCollection getEmailChangedNewUnverifiedTemplates();
}
