package be.nikiroo.utils.ui;

import javax.swing.JComponent;
import javax.swing.JSpinner;

import be.nikiroo.utils.resources.MetaInfo;

public class ConfigItemInteger<E extends Enum<E>> extends ConfigItem<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link ConfigItemInteger} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItemInteger(MetaInfo<E> info) {
		super(info, true);
	}

	@Override
	protected Object getFromField(int item) {
		JSpinner field = (JSpinner) getField(item);
		if (field != null) {
			return field.getValue();
		}

		return null;
	}

	@Override
	protected Object getFromInfo(int item) {
		return info.getInteger(item, false);
	}

	@Override
	protected void setToField(Object value, int item) {
		JSpinner field = (JSpinner) getField(item);
		if (field != null) {
			field.setValue(value == null ? 0 : (Integer) value);
		}
	}

	@Override
	protected void setToInfo(Object value, int item) {
		info.setInteger((Integer) value, item);
	}

	@Override
	protected JComponent createField(int item) {
		return new JSpinner();
	}
}
