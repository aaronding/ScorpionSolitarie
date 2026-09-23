/*
 * HelpDialog.java
 *
 * Created on December 11, 2006, 11:11 PM
 */

package com.family.help;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.SAXParserFactory;

/**
 * Help pages, listed in a tree on the left (from help.xml) and shown on the right.
 *
 * @author Aaron
 */
public class HelpDialog extends JDialog {

    private static final String PATH = "com/family/help/";

    public HelpDialog(Frame parent, String appName) {
        super(parent, appName + " Help", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTree tree = new JTree();
        try {
            HelpHandler handler = new HelpHandler();
            SAXParserFactory.newInstance().newSAXParser().parse(resource("help.xml").openStream(), handler);
            tree.setModel(new DefaultTreeModel(handler.getNodes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        tree.setRootVisible(false);
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);

        page.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.getStyleSheet().addRule("body { font-family: '" + UIManager.getFont("Label.font").getFamily()
            + "', sans-serif; font-size: 13pt; margin: 14px 18px; }");
        kit.getStyleSheet().addRule("h1 { font-size: 17pt; margin-top: 0; }");
        kit.getStyleSheet().addRule("li { margin-bottom: 4px; }");
        page.setEditorKit(kit);

        page.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                try {
                    page.setPage(e.getURL());
                } catch (Exception ex) {
                    page.setText("Page not found.");
                }
            }
        });
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            if (node != null && ((NodeInfo)node.getUserObject()).url != null)
                show(((NodeInfo)node.getUserObject()).url);
        });
        tree.setSelectionRow(0);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), new JScrollPane(page));
        split.setDividerLocation(190);
        getContentPane().add(split, BorderLayout.CENTER);
        setSize(new Dimension(760, 540));
        setLocationRelativeTo(parent);
    }

    private void show(String url) {
        try {
            page.setPage(resource(url));
        } catch (Exception e) {
            page.setText("Page not found.");
        }
    }

    private static URL resource(String name) {
        return HelpDialog.class.getClassLoader().getResource(PATH + name);
    }

    private final JEditorPane page = new JEditorPane();
}
