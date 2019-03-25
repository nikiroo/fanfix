package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Bundle;

/**
 * The type of configuration information the associated {@link Bundle} will
 * convey.
 * 
 * @author niki
 */
public enum Target {
	/**
	 * Configuration options that the user can change in the
	 * <tt>.properties</tt> file
	 */
	config,
	/** Translation resources (Core) */
	resources_core,
	/** Translation resources (GUI) */
	resources_gui,
	/** UI resources (from colours to behaviour) */
	ui,
	/** Description of UI resources. */
	ui_description,
}
