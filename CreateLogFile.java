import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CreateLogFile {
    private String logDirectory;
    private String logFileName;
    
    public CreateLogFile() {
        this.logDirectory = "logs";
    }
    
    // 生成带日期的文件名
    private String generateDateBasedFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return "Log_" + now.format(formatter) + ".txt";
    }
    
    // 生成带日期和序号的文件名
    private String generateSequentialFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = now.format(formatter);
        
        // 查找当天已存在的文件，确定序号
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            return "Log_" + dateStr + "_001.txt";
        }
        
        // 获取当天所有的日志文件
        File[] files = directory.listFiles((dir, name) -> 
            name.startsWith("Log_" + dateStr) && name.endsWith(".txt"));
        
        int maxNumber = 0;
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                // 提取序号部分 Log_20240101_001.txt
                String numberStr = fileName.replaceAll(".*_(\\d+)\\.txt", "$1");
                try {
                    int number = Integer.parseInt(numberStr);
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException e) {
                    // 忽略解析失败的文件名
                }
            }
        }
        
        // 生成新的序号（最大序号+1，不足3位补0）
        int newNumber = maxNumber + 1;
        String sequentialNumber = String.format("%03d", newNumber);
        return "Log_" + dateStr + "_" + sequentialNumber + ".txt";
    }
    
    // 生成带年月日时分秒的文件名（最精确，不会重复）
    private String generateTimestampFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return "Log_" + now.format(formatter) + ".txt";
    }
    
    // newLog方法 - 使用日期生成文件名
    public boolean newLog() {
        return createLogFile(generateDateBasedFileName());
    }
    
    // newLog方法 - 使用序号生成文件名（重载版本）
    public boolean newLog(boolean useSequential) {
        if (useSequential) {
            return createLogFile(generateSequentialFileName());
        } else {
            return createLogFile(generateDateBasedFileName());
        }
    }
    
    // newLog方法 - 自定义文件名
    public boolean newLog(String customFileName) {
        return createLogFile(customFileName);
    }
    
    // 核心方法：创建日志文件
    private boolean createLogFile(String fileName) {
        this.logFileName = fileName;
        File directory = new File(logDirectory);
        
        // 创建目录
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("日志目录创建成功: " + directory.getAbsolutePath());
            } else {
                System.err.println("日志目录创建失败");
                return false;
            }
        }
        
        // 创建文件
        File logFile = new File(directory, logFileName);
        try {
            if (logFile.createNewFile()) {
                System.out.println("日志文件创建成功: " + logFile.getAbsolutePath());
                return true;
            } else {
                System.out.println("日志文件已存在: " + logFile.getAbsolutePath());
                // 如果文件已存在，可以选择覆盖或返回
                return false;  // 返回false表示文件已存在，没有创建新文件
            }
        } catch (IOException e) {
            System.err.println("创建日志文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // 获取当前日志文件名
    public String getCurrentLogFileName() {
        return logFileName;
    }

    public static void main(String[] args) {
        CreateLogFile logCreator = new CreateLogFile();

        System.out.println("=== 测试简单日期文件名 ===");
        boolean result1 = logCreator.newLog();
        System.out.println("创建结果: " + result1 + ", 文件名: " + logCreator.getCurrentLogFileName());
        
        System.out.println("\n=== 测试带序号的日期文件名 ===");
        boolean result2 = logCreator.newLog(true);
        System.out.println("创建结果: " + result2 + ", 文件名: " + logCreator.getCurrentLogFileName());
        
        System.out.println("\n=== 再次测试序号（应该递增） ===");
        boolean result3 = logCreator.newLog(true);
        System.out.println("创建结果: " + result3 + ", 文件名: " + logCreator.getCurrentLogFileName());
        
        System.out.println("\n=== 测试时间戳文件名 ===");
        boolean result4 = logCreator.createLogFile(logCreator.generateTimestampFileName());
        System.out.println("创建结果: " + result4 + ", 文件名: " + logCreator.getCurrentLogFileName());
        
        System.out.println("\n=== 测试自定义文件名 ===");
        boolean result5 = logCreator.newLog("my_special_log.txt");
        System.out.println("创建结果: " + result5 + ", 文件名: " + logCreator.getCurrentLogFileName());
    }
}
    