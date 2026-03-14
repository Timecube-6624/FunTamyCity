package Simulation;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CitizenSimulator {
    private static void main(String args[]) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("src/main/resources/PersonalityResources/CitizenData.json")));
            CreateLogFile.getInstance().log("Successfully loaded CitizenData.json");
            CreateLogFile.getInstance().setLogLevel(CreateLogFile.LogLevel.INFO);
            CreateLogFile.getInstance().flush();
            JSONObject CitizenData = new JSONObject(content);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
