import javax.swing.SwingUtilities;

void main() {
    SwingUtilities.invokeLater(() -> {
        window myWindow = new window();
        myWindow.setVisible(true); // Ez teszi láthatóvá az ablakot
    });
}