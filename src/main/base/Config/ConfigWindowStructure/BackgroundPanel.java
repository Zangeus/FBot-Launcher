package Config.ConfigWindowStructure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class BackgroundPanel extends JPanel {
    private final Image backgroundImage;
    private final float transparency;
    private Color edgeColor = Color.WHITE;

    private Point initialClick;
    private Window window;

    public BackgroundPanel(Image backgroundImage, float transparency) {
        this.backgroundImage = backgroundImage;
        this.transparency = transparency;
        setLayout(new BorderLayout());
        calculateEdgeColor();

        // Добавляем обработчики мыши для перемещения
        addMouseListeners();
    }

    // Устанавливаем ссылку на окно
    public void setWindow(Window window) {
        this.window = window;
    }

    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (initialClick != null && window != null) {
                    int thisX = window.getLocation().x;
                    int thisY = window.getLocation().y;

                    // Вычисляем новую позицию окна
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;

                    int x = thisX + xMoved;
                    int y = thisY + yMoved;

                    window.setLocation(x, y);
                }
            }
        });
    }

    private void calculateEdgeColor() {
        try {
            BufferedImage bufferedImage = (BufferedImage) backgroundImage;
            int rgb = bufferedImage.getRGB(0, 0); // Брать пиксель из (0,0)
            edgeColor = new Color(rgb);
        } catch (Exception e) {
            edgeColor = new Color(40, 40, 40);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Заливаем фон цветом края
        g2d.setColor(edgeColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Рисуем фоновое изображение с прозрачностью
        if (backgroundImage != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));

            // Масштабируем изображение для заполнения панели с сохранением пропорций
            double widthRatio = (double) getWidth() / backgroundImage.getWidth(null);
            double heightRatio = (double) getHeight() / backgroundImage.getHeight(null);
            double scale = Math.max(widthRatio, heightRatio);

            int scaledWidth = (int) (backgroundImage.getWidth(null) * scale);
            int scaledHeight = (int) (backgroundImage.getHeight(null) * scale);

            int x = (getWidth() - scaledWidth) / 2;
            int y = (getHeight() - scaledHeight) / 2;

            g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, null);
        }

        g2d.dispose();
    }
}