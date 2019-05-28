package be.nikiroo.utils.ui;

import javax.swing.JComponent;
import javax.swing.JPasswordField;

import be.nikiroo.utils.resources.MetaInfo;

public class ConfigItemPassword<E extends Enum<E>> extends ConfigItem<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link ConfigItemPassword} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItemPassword(MetaInfo<E> info) {
		super(info, true);
	}

	@Override
	protected Object getFromField(int item) {
		JPasswordField field = (JPasswordField) getField(item);
		if (field != null) {
			return new String(field.getPassword());
		}

		return null;
	}

	@Override
	protected Object getFromInfo(int item) {
		return info.getString(item, false);
	}

	@Override
	protected void setToField(Object value, int item) {
		JPasswordField field = (JPasswordField) getField(item);
		if (field != null) {
			field.setText(value == null ? "" : value.toString());
		}
	}

	@Override
	protected void setToInfo(Object value, int item) {
		info.setString((String) value, item);
	}

	@Override
	protected JComponent createField(int item) {
		return new JPasswordField();
	}
}
