/*
 * The MIT License
 *
 * Copyright 2017 Olav Övrebö.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
*/

package relief;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Math.pow;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 *
 * @author Olav
 */
public class Relief extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
    float imagePointHeight(BufferedImage img, int x, int y) {
        int pixel = img.getRGB(x, y);
        int r, g, b;
        r = (pixel >> 16) & 0xff;
        g = (pixel >> 8)  & 0xff;
        b = pixel & 0xff;
        
        return (float) (r + g + b)/0x2fd;
    }
    
    float imageIntersectionPointHeight(BufferedImage img, int x, int y) {
        int divider = 4;
        float ur, ul, dr, dl;
        
        if (x == 0 || y == 0) {
            ul = 0;
            divider--;
        }
        else 
            ul = imagePointHeight(img, x - 1, y - 1);
        
        if (x == 0 || y == img.getHeight()) {
            dl = 0;
            divider --;
        }
        else
            dl = imagePointHeight(img, x - 1, y);
        
        if (x == img.getWidth() || y == 0) {
            ur = 0;
            divider--;
        }
        else
            ur = imagePointHeight(img, x, y - 1);
        
        if (x == img.getWidth() || y == img.getHeight()) {
            dr = 0;
            divider--;
        }
        else
            dr = imagePointHeight(img, x, y);
        
        return (ur + ul + dr + dl)/divider;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select an image to generate 3D object");
        ExtensionFilter exin = new ExtensionFilter("Images", "*.png", "*.PNG", "*.jpeg", "*.JPEG", "*.jpg", "*.JPG", "*.bmp", "*.BMP");
        ExtensionFilter exout = new ExtensionFilter("3D image", "*.stl");
        fc.getExtensionFilters().add(exin);
        fc.setSelectedExtensionFilter(exin);
        File fileChosen = fc.showOpenDialog(primaryStage);
        if (fileChosen == null)
            System.exit(0);
        
        BufferedImage image = ImageIO.read(fileChosen);
        
        fc.setTitle("Select an output file");
        fc.getExtensionFilters().remove(exin);
        fc.getExtensionFilters().add(exout);
        fc.setSelectedExtensionFilter(exout);
        File saveFile = fc.showSaveDialog(primaryStage);
        
        FileOutputStream fo = new FileOutputStream(saveFile);
        for (int i = 0; i < 80; i++)
            fo.write(' ');    //comment section of file left blank
        
        int facetsTotal = 2 * image.getHeight() * image.getWidth();
        fo.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(facetsTotal).array());      //writes number of facets to file 
        
        float cornerHeights[][] = new float[image.getWidth() + 1][image.getHeight() + 1];
        for (int col = 0; col < image.getWidth() + 1; col++)
            for (int row = 0; row < image.getHeight() + 1; row++)
                cornerHeights[col][row] = imageIntersectionPointHeight(image, col, row);
        
        
        
        for (int col = 0; col < image.getWidth(); col++)
            for (int row = 0; row < image.getHeight(); row++) {
                Point3D p1, p2, p3, p4;
                
                p1 = new Point3D(col, row, cornerHeights[col][row]);
                p2 = new Point3D(col + 1, row, cornerHeights[col + 1][row]);
                p3 = new Point3D(col, row + 1, cornerHeights[col][row + 1]);
                p4 = new Point3D(col + 1, row + 1, cornerHeights[col + 1][row + 1]);
                new TriPolygon(p1, p2, p3).writeFacet(fo);
                new TriPolygon(p2, p4, p3).writeFacet(fo);
            }
        
        System.exit(0);
    }
}

class Point3D {
    public float x;
    public float y;
    public float z;

    public Point3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void normalize() {
        float len = (float) Math.sqrt(pow(x, 2) + pow(y, 2) + pow(z, 2));
        if (len != 0) {
            x = x/len;
            y = y/len;
            z = z/len;
        }
    }
    
    public Point3D getNormal(Point3D left, Point3D right){
        Point3D leftDiff = new Point3D(left.x - this.x, left.y - this.y, left.z - this.z);
        Point3D rightDiff = new Point3D(right.x - this.x, right.y - this.y, right.z - this.z);
        
        float x, y, z;
        x = rightDiff.y * leftDiff.z - rightDiff.z * leftDiff.y;
        y = rightDiff.z * leftDiff.x - rightDiff.x * leftDiff.z;
        z = rightDiff.x * leftDiff.y - rightDiff.y * leftDiff.x;
        
        Point3D res = new Point3D(x, y, z);
        res.normalize();
        return res;
    }
    
    public byte [] pointAsByteArray() {
        return ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putFloat(x).putFloat(y).putFloat(z).array();
    }
}

class TriPolygon {
    Point3D points[] = new Point3D[3];
    Point3D normal;
    
    
    public TriPolygon(Point3D p1, Point3D p2, Point3D p3) {
        points[0] = p1;
        points[1] = p2;
        points[2] = p3;
        this.normal = p1.getNormal(p2, p3);
    }

    public TriPolygon(Point3D p1, Point3D p2, Point3D p3, Point3D normal) {
        points[0] = p1;
        points[1] = p2;
        points[2] = p3;
        this.normal = normal;
    }
    
    public void writeFacet(FileOutputStream fo) throws IOException {
        fo.write(normal.pointAsByteArray());
        for (Point3D p : points)
            fo.write(p.pointAsByteArray());
        fo.write(0);    //facet buffer
        fo.write(0);    //facet buffer
    }
}