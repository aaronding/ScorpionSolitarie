/*
 * LevelPanel.java
 *
 * Created on December 9, 2006, 8:08 PM
 */

package com.family.solitaire.ui;

/**
 *
 * @author  Aaron
 */
public class LevelPanel extends javax.swing.JPanel {
    
    /** Creates new form LevelPanel */
    public LevelPanel() {
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        buttonGroup = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        easy = new javax.swing.JRadioButton();
        medium = new javax.swing.JRadioButton();
        difficult = new javax.swing.JRadioButton();
        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();

        jLabel1.setText("Select the game difficulty leve that you want:");

        buttonGroup.add(easy);
        easy.setText("Easy,");
        easy.setActionCommand("Easy");
        easy.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        easy.setMargin(new java.awt.Insets(0, 0, 0, 0));

        buttonGroup.add(medium);
        medium.setText("Medium");
        medium.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        medium.setMargin(new java.awt.Insets(0, 0, 0, 0));

        buttonGroup.add(difficult);
        difficult.setSelected(true);
        difficult.setText("Difficult");
        difficult.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        difficult.setMargin(new java.awt.Insets(0, 0, 0, 0));

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(ok, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(19, 19, 19)
                        .addComponent(cancel, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(96, 96, 96)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(easy)
                            .addComponent(medium)
                            .addComponent(difficult)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jLabel1)))
                .addContainerGap(81, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancel, ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(easy, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(medium, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addComponent(difficult, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ok)
                    .addComponent(cancel))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
        retValue = false;
        getTopLevelAncestor().setVisible(false);
    }//GEN-LAST:event_cancelActionPerformed

    private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
        retValue = true;
        getTopLevelAncestor().setVisible(false);
    }//GEN-LAST:event_okActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton difficult;
    private javax.swing.JRadioButton easy;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JRadioButton medium;
    private javax.swing.JButton ok;
    // End of variables declaration//GEN-END:variables
    
    private boolean retValue;
    
    public boolean getRetValue() {
        return retValue;
    }
    
    public String getLevel() {
        if (easy.isSelected())
            return "easy";
        
        if (medium.isSelected())
            return "medium";
        
        if (difficult.isSelected())
            return "difficult";
        
        throw new IllegalStateException("");
    }
}
