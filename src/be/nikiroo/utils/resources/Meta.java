package be.nikiroo.utils.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to give some information about the translation keys, so the
 * translation .properties file can be created programmatically.
 * 
 * @author niki
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Meta {
	/**
	 * What kind of item this key represent (a Key, a Label text, a format to
	 * use for something else...).
	 * 
	 * @return what it is
	 */
	String what();

	/**
	 * Where in the application will this key appear (in the action keys, in a
	 * menu, in a message...).
	 * 
	 * @return where it is
	 */
	String where();

	/**
	 * What format should/must this key be in.
	 * 
	 * @return the format it is in
	 */
	String format();

	/**
	 * Free info text to help translate.
	 * 
	 * @return some info
	 */
	String info();
}
