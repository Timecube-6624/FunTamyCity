
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class WorldEnviroment {
    // ==================== 核心变量 ====================
    private static volatile boolean isRunning = true;
    private static volatile boolean isPaused = false;
    private static volatile double speedMultiplier = 1.0;//speedMultipulier用于存储当前的倍速
    private static AtomicLong tickCount = new AtomicLong(0);
    
    // 季节相关
    private static final String[] SEASONS = {"Spring", "Summer", "Autumn", "Winter"};
    private static final long SEASON_LENGTH = 78894000; // 季节长度(Tick数)
    private static int currentSeason = 0;
    private static long seasonTick = 0;
    
    // 控制台输入
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.print("请输入速度倍数 (例如: 2.0 或 0.5): ");
        try {
            speedMultiplier = Double.parseDouble(scanner.nextLine());

            if (speedMultiplier <= 0) {
            // 构建包含原始值的错误信息
            String errorMsg = "Maths Error(1): double speedMultiplier must be greater than zero, but was " + speedMultiplier + ". Resetting to 1.0.";
            // 向控制台输出错误（使用标准错误流更合适）
            System.err.println(errorMsg);
            // 记录日志
            CreateLogFile.getInstance().log(errorMsg);
            // 修正值
            speedMultiplier = 1.0;
            }
        } catch (Exception e) {
            speedMultiplier = 1.0;
        }
        
        System.out.println("=== Tick管理器启动 ===");
        System.out.println("速度: " + speedMultiplier + "x");
        System.out.println("季节: " + SEASONS[currentSeason]);
        System.out.println("命令: p(暂停) r(恢复) s X.X(调速) q(退出)");
        
        // 启动控制台监听线程
        startConsoleListener();
        
        // 主循环
        while (isRunning) {
            try {
                if (isPaused) {
                    Thread.sleep(100);
                    continue;
                }
                
                // 执行一个Tick
                executeTick();
                
                // 计算休眠时间 (100ms / 速度倍数)
                long sleepTime = (long)(100 / speedMultiplier);
                if (sleepTime < 1) sleepTime = 1;
                Thread.sleep(sleepTime);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("Tick管理器已停止，总计执行 " + tickCount.get() + " Ticks");
        scanner.close();
    }
    
    /**
     * 执行单个Tick的逻辑
     */
    private static void executeTick() {
        long currentTick = tickCount.incrementAndGet();
        seasonTick++;
        
        // 季节切换检测
        if (seasonTick >= SEASON_LENGTH) {
            seasonTick = 0;
            currentSeason = (currentSeason + 1) % SEASONS.length;
            System.out.println("=== 季节变更: " + SEASONS[currentSeason] + " ===");
        }
        
        // 每1000个Tick输出一次状态
        if (currentTick % 1000 == 0) {
            System.out.printf("[Tick %d] 季节: %s | 速度: %.1fx | 暂停: %s%n",
                currentTick, SEASONS[currentSeason], speedMultiplier, isPaused ? "是" : "否");
        }
        
        // 在这里添加你的世界逻辑
        // processWorldTick(currentTick);
    }
    
    /**
     * 控制台命令监听
     */
    private static void startConsoleListener() {
        Thread listenerThread = new Thread(() -> {
            while (isRunning) {
                try {
                    if (scanner.hasNextLine()) {
                        String command = scanner.nextLine().trim().toLowerCase();
                        
                        switch (command) {
                            case "p":
                                isPaused = true;
                                System.out.println("[System] Paused");
                                CreateLogFile.getInstance().log("[System] Paused");
                                break;
                                
                            case "r":
                                isPaused = false;
                                System.out.println("[System] Repaird");
                                CreateLogFile.getInstance().log("[System] Repaird");
                                break;
                                
                            case "q":
                                isRunning = false;
                                System.out.println("[System] Exiting...");
                                CreateLogFile.getInstance().log("[System] Exiting...");
                                break;
                                
                            default:
                                if (command.startsWith("s ")) {
                                    try {
                                        double newSpeed = Double.parseDouble(command.substring(2).trim());
                                        if (newSpeed > 0) {
                                            speedMultiplier = newSpeed;
                                            System.out.printf("[系统] 速度已调整为 %.1fx%n", speedMultiplier);
                                        }
                                    } catch (Exception e) {
                                        System.out.println("[错误] 格式错误，请使用: s 2.0");
                                    }
                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略Scanner异常
                }
            }
        });
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * 获取当前Tick数
     */
    public static long getTickCount() {
        return tickCount.get();
    }
    
    /**
     * 获取当前季节
     */
    public static String getCurrentSeason() {
        return SEASONS[currentSeason];
    }
    
    /**
     * 获取当前季节索引
     */
    public static int getCurrentSeasonIndex() {
        return currentSeason;
    }
    
    /**
     * 获取速度倍数
     */
    public static double getSpeedMultiplier() {
        return speedMultiplier;
    }
}