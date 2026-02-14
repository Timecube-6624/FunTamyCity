import java.io.File;
import java.io.IOException;

public class CreateLogFile {
    public static void newLog() {
        // 指定日志文件路径
        String logDirectory = "logs";
        String logFileName = "application.txt";
        
        // 创建目录（如果不存在）
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("日志目录创建成功: " + directory.getAbsolutePath());
            }
        }
        
        // 创建日志文件
        File logFile = new File(directory, logFileName);
        try {
            if (logFile.createNewFile()) {
                System.out.println("日志文件创建成功: " + logFile.getAbsolutePath());
            } else {
                System.out.println("日志文件已存在: " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("创建日志文件失败: " + e.getMessage());
        }
    }
}