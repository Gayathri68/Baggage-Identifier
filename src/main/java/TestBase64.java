import java.nio.file.*;

import java.util.Base64;



public class TestBase64 {

    public static void main(String[] args) throws Exception {

        byte[] imageBytes = Files.readAllBytes(Paths.get("img_3 .png"));

        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        System.out.println(base64);

    }
}