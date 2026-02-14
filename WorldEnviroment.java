
import java.io.File;
import java.io.IOException;

// 调用CreateLogFile中的newLog方法创建log.txt
public class WorldEnviroment {
	public static void main(String[] args) {
        CreateLogFile log = new CreateLogFile();
        log.newLog();
    }
}
