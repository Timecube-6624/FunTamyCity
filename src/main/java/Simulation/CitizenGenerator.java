package Simulation;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CitizenGenerator { //CitizenGenerator
    public static void main(String[] args) {
        try {
            //read citizen name repository prepare for random generate citizen name

            //Read NameRepository.json file to String
            String content = new String(Files.readAllBytes(Paths.get("src/main/resources/PersonalityResources/NameRepository.json")));
            //Create a log massage
            CreateLogFile.getInstance().log("Successfully loaded NameRepository.json");
            CreateLogFile.getInstance().setLogLevel(CreateLogFile.LogLevel.INFO);
            CreateLogFile.getInstance().flush();
            //read name repository to a 
            JSONObject NameRepository = new JSONObject(content);
            //pick a first name randomly

        } catch (IOException e){
            e.printStackTrace();
        }




        try {
            //read and write generated citizen data

            //read CitizenData.json file to String
            String content = new String(Files.readAllBytes(Paths.get("src/main/resources/PersonalityResources/CitizenData")));
            CreateLogFile.getInstance().log("Successfully loaded CitizenData.json");
            CreateLogFile.getInstance().setLogLevel(CreateLogFile.LogLevel.INFO);
            CreateLogFile.getInstance().flush();
            //Create JSONObject from String
            JSONObject obj = new JSONObject(content);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
