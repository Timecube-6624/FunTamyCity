import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class CreateLogFile {
    // ==================== 单例模式 ====================
    private static volatile CreateLogFile instance;
    
    public static CreateLogFile getInstance() {
        if (instance == null) {
            synchronized (CreateLogFile.class) {
                if (instance == null) {
                    instance = new CreateLogFile();
                }
            }
        }
        return instance;
    }
    
    // ==================== 常量定义 ====================
    private static final String DEFAULT_LOG_DIRECTORY = "logs";
    private static final int MAX_QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = 100;  // 批量写入条数
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd");
    
    // ==================== 日志级别 ====================
    public enum LogLevel {
        DEBUG(0), INFO(1), WARN(2), ERROR(3), FATAL(4);
        
        private final int level;
        LogLevel(int level) { this.level = level; }
        public int getLevel() { return level; }
    }
    
    private LogLevel currentLogLevel = LogLevel.INFO;  // 默认日志级别
    
    // ==================== 成员变量 ====================
    private String logDirectory;
    private String currentLogFileName;
    private BufferedWriter writer;
    private BlockingQueue<String> logQueue;
    private volatile boolean running = true;
    private AtomicLong logCounter = new AtomicLong(0);  // 日志计数
    private Thread writerThread;
    private LogFileNameStrategy fileNameStrategy = LogFileNameStrategy.DAILY;
    
    // 文件名策略枚举
    public enum LogFileNameStrategy {
        DAILY,           // 每日一个文件 Log_20240214.log
        SEQUENTIAL,      // 带序号 Log_20240214_001.log
        HOURLY,          // 每小时 Log_20240214_14.log
        TIMESTAMP        // 时间戳 Log_20240214_143025.log
    }
    
    // ==================== 构造方法 ====================
    private CreateLogFile() {
        this(DEFAULT_LOG_DIRECTORY);
    }
    
    private CreateLogFile(String logDirectory) {
        this.logDirectory = logDirectory;
        this.logQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        initLogFile();
        startWriterThread();
        registerShutdownHook();
    }
    
    // ==================== 初始化方法 ====================
    private void initLogFile() {
        try {
            createLogDirectory();
            rotateLogFileIfNeeded();
        } catch (IOException e) {
            System.err.println("初始化日志文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createLogDirectory() {
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("日志目录创建成功: " + directory.getAbsolutePath());
            }
        }
    }
    
    private void rotateLogFileIfNeeded() throws IOException {
        String newFileName = generateFileName();
        
        // 如果文件名变化，关闭旧文件创建新文件
        if (!newFileName.equals(currentLogFileName)) {
            if (writer != null) {
                writer.close();
            }
            currentLogFileName = newFileName;
            File logFile = new File(logDirectory, currentLogFileName);
            writer = new BufferedWriter(new FileWriter(logFile, true));
            
            // 写入日志会话开始标记
            writeToFile("========== 日志会话开始 ==========");
            writeToFile("日志文件: " + logFile.getAbsolutePath());
        }
    }
    
    private String generateFileName() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DATE_FORMATTER);
        
        switch (fileNameStrategy) {
            case DAILY:
                return "Log_" + dateStr + ".log";
                
            case SEQUENTIAL:
                return generateSequentialFileName(now, dateStr);
                
            case HOURLY:
                return "Log_" + dateStr + "_" + 
                       String.format("%02d", now.getHour()) + ".log";
                
            case TIMESTAMP:
                return "Log_" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log";
                
            default:
                return "Log_" + dateStr + ".log";
        }
    }
    
    private String generateSequentialFileName(LocalDateTime now, String dateStr) {
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            return "Log_" + dateStr + "_001.log";
        }
        
        File[] files = directory.listFiles((dir, name) -> 
            name.startsWith("Log_" + dateStr) && name.endsWith(".log"));
        
        int maxNumber = 0;
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String numberStr = fileName.replaceAll(".*_(\\d+)\\.log", "$1");
                try {
                    int number = Integer.parseInt(numberStr);
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException e) {
                    // 忽略解析失败的文件名
                }
            }
        }
        
        return "Log_" + dateStr + "_" + String.format("%03d", maxNumber + 1) + ".log";
    }
    
    private void startWriterThread() {
        writerThread = new Thread(() -> {
            int batchCount = 0;
            StringBuilder batchBuffer = new StringBuilder();
            
            while (running || !logQueue.isEmpty()) {
                try {
                    // 批量收集日志
                    String log = logQueue.poll();
                    if (log != null) {
                        batchBuffer.append(log).append(System.lineSeparator());
                        batchCount++;
                        
                        // 达到批量大小或队列为空时写入
                        if (batchCount >= BATCH_SIZE || logQueue.isEmpty()) {
                            writeBatchToFile(batchBuffer.toString());
                            batchBuffer.setLength(0);
                            batchCount = 0;
                        }
                    } else {
                        // 队列为空，短暂等待
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 写入剩余日志
            if (batchBuffer.length() > 0) {
                writeBatchToFile(batchBuffer.toString());
            }
        });
        
        writerThread.setDaemon(true);
        writerThread.setName("LogWriter-Thread");
        writerThread.start();
    }
    
    private synchronized void writeBatchToFile(String logs) {
        try {
            writer.write(logs);
            writer.flush();
        } catch (IOException e) {
            System.err.println("批量写入日志失败: " + e.getMessage());
        }
    }
    
    private synchronized void writeToFile(String log) {
        try {
            writer.write(log);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("写入日志失败: " + e.getMessage());
        }
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    // ==================== 日志写入方法 ====================
    
    /**
     * 功能1: 基础日志记录
     */
    public void log(String message) {
        if (!running) return;
        log(LogLevel.INFO, message);
    }
    
    /**
     * 功能2: 带级别的日志记录
     */
    public void log(LogLevel level, String message) {
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return;  // 低于当前日志级别，不记录
        }
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String threadName = Thread.currentThread().getName();
        String formattedLog = String.format("[%s][%s][%s] %s", 
            timestamp, level, threadName, message);
        
        try {
            logQueue.put(formattedLog);
            logCounter.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 功能3: 格式化日志（类似String.format）
     */
    public void log(String format, Object... args) {
        log(LogLevel.INFO, String.format(format, args));
    }
    
    /**
     * 功能4: 带级别的格式化日志
     */
    public void log(LogLevel level, String format, Object... args) {
        log(level, String.format(format, args));
    }
    
    /**
     * 功能5: 记录异常
     */
    public void log(String message, Throwable e) {
        log(LogLevel.ERROR, message);
        logException(e);
    }
    
    /**
     * 功能6: 记录异常堆栈
     */
    public void logException(Throwable e) {
        log(LogLevel.ERROR, "异常: " + e.getMessage());
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        
        // 分割堆栈跟踪，逐行记录
        String[] lines = sw.toString().split("\n");
        for (String line : lines) {
            log(LogLevel.ERROR, "  " + line.trim());
        }
    }
    
    /**
     * 功能7: 快速记录键值对
     */
    public void logKeyValue(String key, Object value) {
        log(String.format("%s: %s", key, value));
    }
    
    /**
     * 功能8: 记录分隔线
     */
    public void logSeparator() {
        log("--------------------------------------------------");
    }
    
    /**
     * 功能9: 记录带标题的分隔线
     */
    public void logSection(String title) {
        logSeparator();
        log(">> " + title + " <<");
        logSeparator();
    }
    
    // ==================== 配置方法 ====================
    
    /**
     * 功能10: 设置日志级别
     */
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
        log(LogLevel.INFO, "日志级别设置为: " + level);
    }
    
    /**
     * 功能11: 设置文件名策略
     */
    public void setFileNameStrategy(LogFileNameStrategy strategy) {
        this.fileNameStrategy = strategy;
        log(LogLevel.INFO, "文件名策略设置为: " + strategy);
        try {
            rotateLogFileIfNeeded();
        } catch (IOException e) {
            logException(e);
        }
    }
    
    /**
     * 功能12: 手动切换日志文件
     */
    public boolean rotateLogFile() {
        try {
            rotateLogFileIfNeeded();
            return true;
        } catch (IOException e) {
            logException(e);
            return false;
        }
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 功能13: 获取当前日志文件路径
     */
    public String getCurrentLogPath() {
        return new File(logDirectory, currentLogFileName).getAbsolutePath();
    }
    
    /**
     * 功能14: 获取日志总数
     */
    public long getLogCount() {
        return logCounter.get();
    }
    
    /**
     * 功能15: 获取队列剩余容量
     */
    public int getQueueRemainingCapacity() {
        return logQueue.remainingCapacity();
    }
    
    // ==================== 管理方法 ====================
    
    /**
     * 功能16: 立即刷新缓冲区
     */
    public synchronized void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            System.err.println("刷新缓冲区失败: " + e.getMessage());
        }
    }
    
    /**
     * 功能17: 关闭日志系统
     */
    public void shutdown() {
        running = false;
        try {
            writerThread.join(5000);  // 等待写入线程结束
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        flush();
        try {
            if (writer != null) {
                writeToFile("========== 日志会话结束 ==========");
                writeToFile("总计写入日志: " + logCounter.get() + " 条");
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("关闭日志文件失败: " + e.getMessage());
        }
    }
    
    // ==================== 测试方法 ====================
    public static void main(String[] args) throws InterruptedException {
        // 获取日志实例
        CreateLogFile logger = CreateLogFile.getInstance();
        
        System.out.println("=== 测试日志服务功能 ===\n");
        
        // 测试基础功能
        logger.logSection("基础日志功能测试");
        logger.log("这是一条普通日志");
        logger.log(LogLevel.WARN, "这是一条警告日志");
        logger.log(LogLevel.ERROR, "这是一条错误日志");
        
        // 测试格式化日志
        logger.logSection("格式化日志测试");
        logger.log("玩家 %s 在位置 (%d, %d, %d) 登录", "Steve", 100, 64, 200);
        
        // 测试键值对
        logger.logSection("键值对测试");
        logger.logKeyValue("玩家名称", "Alex");
        logger.logKeyValue("生命值", 80);
        logger.logKeyValue("饥饿值", 65);
        
        // 测试异常记录
        logger.logSection("异常记录测试");
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            logger.log("发生算术异常", e);
        }
        
        // 测试文件名策略
        logger.logSection("文件名策略测试");
        logger.setFileNameStrategy(LogFileNameStrategy.HOURLY);
        logger.log("这是每小时日志文件中的一条记录");
        
        // 测试批量日志
        logger.logSection("批量日志测试");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            logger.log("批量测试日志 #%d", i + 1);
        }
        long end = System.currentTimeMillis();
        logger.log("批量写入1000条日志耗时: %d ms", (end - start));
        
        // 查询信息
        logger.logSection("日志统计信息");
        logger.log("当前日志文件: %s", logger.getCurrentLogPath());
        logger.log("总日志条数: %d", logger.getLogCount());
        logger.log("队列剩余容量: %d", logger.getQueueRemainingCapacity());
        
        // 等待所有日志写入
        Thread.sleep(1000);
        
        System.out.println("\n测试完成！日志文件位置: " + logger.getCurrentLogPath());
        
        // 程序结束时自动调用shutdown（通过关闭钩子）
    }
}
