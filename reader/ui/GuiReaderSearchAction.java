package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ProgressBar;

public class GuiReaderSearchAction extends JFrame {
	private static final long serialVersionUID = 1L;

	private GuiReaderBookInfo info;
	private ProgressBar pgBar;

	public GuiReaderSearchAction(BasicLibrary lib, GuiReaderBookInfo info) {
		super(info.getMainInfo());
		this.setSize(800, 600);
		this.info = info;

		setLayout(new BorderLayout());

		JPanel main = new JPanel(new BorderLayout());
		JPanel props = new GuiReaderPropertiesPane(lib, info.getMeta());

		main.add(props, BorderLayout.NORTH);
		main.add(new GuiReaderViewerPanel(info.getMeta(), info.getMeta()
				.isImageDocument()), BorderLayout.CENTER);
		main.add(createImportButton(lib), BorderLayout.SOUTH);

		add(main, BorderLayout.CENTER);

		pgBar = new ProgressBar();
		pgBar.setVisible(false);
		add(pgBar, BorderLayout.SOUTH);

		pgBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				pgBar.setProgress(null);
				setEnabled(true);
				validate();
			}
		});

		pgBar.addUpdateListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				validate();
				repaint();
			}
		});
	}

	private Component createImportButton(final BasicLibrary lib) {
		JButton imprt = new JButton("Import into library");
		imprt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				final Progress pg = new Progress();
				pgBar.setProgress(pg);

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							lib.imprt(new URL(info.getMeta().getUrl()), null);
						} catch (IOException e) {
							Instance.getTraceHandler().error(e);
						}

						pg.done();
					}
				}).start();
			}
		});

		return imprt;
	}
}
