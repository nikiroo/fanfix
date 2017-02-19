package be.nikiroo.fanfix.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

public class BasicSupportTest extends TestLauncher {

	public BasicSupportTest(String[] args) {
		super("BasicSupport", args);

		addSeries(new TestLauncher("General", args) {
			{
				addTest(new TestCase("BasicSupport.makeParagraphs()") {
					@Override
					public void test() throws Exception {
						BasicSupportEmpty support = new BasicSupportEmpty() {
							@Override
							protected boolean isHtml() {
								return true;
							}

							@Override
							public void fixBlanksBreaks(List<Paragraph> paras) {
							}

							@Override
							protected List<Paragraph> requotify(Paragraph para) {
								List<Paragraph> paras = new ArrayList<Paragraph>(
										1);
								paras.add(para);
								return paras;
							}
						};

						List<Paragraph> paras = null;

						paras = support.makeParagraphs(null, "");
						assertEquals(
								"An empty content should not generate paragraphs",
								0, paras.size());

						paras = support.makeParagraphs(null,
								"Line 1</p><p>Line 2</p><p>Line 3</p>");
						assertEquals(5, paras.size());
						assertEquals("Line 1", paras.get(0).getContent());
						assertEquals(ParagraphType.BLANK, paras.get(1)
								.getType());
						assertEquals("Line 2", paras.get(2).getContent());
						assertEquals(ParagraphType.BLANK, paras.get(3)
								.getType());
						assertEquals("Line 3", paras.get(4).getContent());

						paras = support.makeParagraphs(null,
								"<p>Line1</p><p>Line2</p><p>Line3</p>");
						assertEquals(6, paras.size());
					}
				});

				addTest(new TestCase("BasicSupport.removeDoubleBlanks()") {
					@Override
					public void test() throws Exception {
						BasicSupportEmpty support = new BasicSupportEmpty() {
							@Override
							protected boolean isHtml() {
								return true;
							}
						};

						List<Paragraph> paras = null;

						paras = support
								.makeParagraphs(null,
										"<p>Line1</p><p>Line2</p><p>Line3<br/><br><p></p>");
						assertEquals(5, paras.size());

						paras = support
								.makeParagraphs(null,
										"<p>Line1</p><p>Line2</p><p>Line3<br/><br><p></p>* * *");
						assertEquals(5, paras.size());

						paras = support.makeParagraphs(null, "1<p>* * *<p>2");
						assertEquals(3, paras.size());
						assertEquals(ParagraphType.BREAK, paras.get(1)
								.getType());

						paras = support.makeParagraphs(null,
								"1<p><br/><p>* * *<p>2");
						assertEquals(3, paras.size());
						assertEquals(ParagraphType.BREAK, paras.get(1)
								.getType());

						paras = support.makeParagraphs(null,
								"1<p>* * *<br/><p><br><p>2");
						assertEquals(3, paras.size());
						assertEquals(ParagraphType.BREAK, paras.get(1)
								.getType());

						paras = support.makeParagraphs(null,
								"1<p><br/><br>* * *<br/><p><br><p>2");
						assertEquals(3, paras.size());
						assertEquals(ParagraphType.BREAK, paras.get(1)
								.getType());
					}
				});
			}
		});
	}

	private class BasicSupportEmpty extends BasicSupport {
		@Override
		protected String getSourceName() {
			return null;
		}

		@Override
		protected boolean supports(URL url) {
			return false;
		}

		@Override
		protected boolean isHtml() {
			return false;
		}

		@Override
		protected MetaData getMeta(URL source, InputStream in)
				throws IOException {
			return null;
		}

		@Override
		protected String getDesc(URL source, InputStream in) throws IOException {
			return null;
		}

		@Override
		protected List<Entry<String, URL>> getChapters(URL source,
				InputStream in) throws IOException {
			return null;
		}

		@Override
		protected String getChapterContent(URL source, InputStream in,
				int number) throws IOException {
			return null;
		}

		@Override
		// and make it public!
		public List<Paragraph> makeParagraphs(URL source, String content)
				throws IOException {
			return super.makeParagraphs(source, content);
		}

		@Override
		// and make it public!
		public void fixBlanksBreaks(List<Paragraph> paras) {
			super.fixBlanksBreaks(paras);
		}
	}
}
