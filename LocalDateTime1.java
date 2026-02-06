
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LocalDateTime1 {
    private static final List<LocalDateTime>timestamps = new ArrayList<>();
    private static LocalDateTime previousTime = null;
    public static long CurrentTps = 10;
    public static String[] Season = {"Spring","Summer","Autumn","Winter"};
    private static long SeasonLength = 78894000;//季节长度
    private static int CurrentSeasonIndex = 0;
    private static long AccumulatedTick = 0;//累计时间

    private static volatile boolean isPaused = false;//是否暂停，使用volatile保持线程可见
    private static volatile double currentSpeedMultiplier = 1.0;//当前速度倍数
    private static Thread commandThread;//控制台命令处理线程

    private static long dayLength = 86400;//默认一天长度为24小时(86400秒)
    private static long dayTimeLength = 0;//初始化白日时间
    private static long nightTimeLength = 0;//初始化黑夜时间




   
    public static void main(String[] args) {
        Scanner scan1 = new Scanner(System.in);
        System.out.println("输入速度倍数 (例如: 2 = 2倍速, 0.5 = 半速): ");
        
        // 获取用户输入的倍数（可以是小数）
        currentSpeedMultiplier = scan1.nextDouble();
        startCommandThread(scan1);//启动进程监听线程
        
        // 计算休眠时间：基础时间100ms ÷ 倍数
        long sleepTime = (long)(100 / currentSpeedMultiplier);
        
        try {
            long TickCount = 0;//初始化count
            while(true){
                //暂停检查
                if (isPaused){
                    System.out.println("[已暂停 按下R键恢复|键入S<X.X>以修改速度|Q退出程序]");
                    Thread.sleep(100);
                    continue;
                }
                LocalDateTime now = LocalDateTime.now();  //获取目前的时间
                //System.out.println(now);  //输出目前时间
                timestamps.add(now);//添加时间戳到列表

                if (previousTime != null) {
                    long interval = previousTime.until(now,ChronoUnit.MILLIS);//previousTime.until(now,ChronoUnit.MILLIS)是Java8+用于引入日期API
                    System.out.println("[INFO]" + now + "|interval" + interval + "ms" + "| ticks" + (TickCount + 1) + "|Season" + CurrentSeasonIndex);
                    AccumulatedTick++;

                    if (AccumulatedTick >= SeasonLength) {
                        CurrentSeasonIndex = (CurrentSeasonIndex + 1)%Season.length;
                        System.out.println("季节：" + Season[CurrentSeasonIndex]);
                        AccumulatedTick = AccumulatedTick % SeasonLength;

                        switch (CurrentSeasonIndex) {
                            case 0://replace dayLightLength to 43200(12hours)
                                dayTimeLength = 43200;
                            case 1://replace dayLightLength to 50400(14hours)
                                dayTimeLength = 50400;
                            case 2://replace dayLightLength to 43200(12hours)
                                dayTimeLength = 43200;
                            case 3://replace dayLightLength to 36000(10hours)
                                dayTimeLength = 36000;
                                break;
                        }

                        
                        // 这里可以添加你想执行的特定程序
                        executeSeasonalEvent();

                    }

                } else {
                    System.out.println(now + "|开始" + (TickCount + 1));
                }
                TickCount++;
                previousTime = now;

                Thread.sleep(sleepTime);//设定时间为0.1s循环一次
            
            }

        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            stopCommandThread();//停止监控
            scan1.close();
        }
    }

    //倍数不为0计算
    private static long calculateSleepTime(){
        long baseTime = 100;//基础时间为100ms
        long sleepTime = (long)(baseTime / currentSpeedMultiplier);
        if (sleepTime <= 0){
            System.out.println("错误的睡眠时间,睡眠时间区间为(0,999),已修改为1");//错误提示
            sleepTime = 1;//修改错误变量为1

        }
        return sleepTime;
    }

   
    // 启动命令处理线程
    private static void startCommandThread(Scanner commandThreadInputScanner) {
        commandThread = new Thread(() -> {//新建线程
            System.out.println("\n=== 控制命令 ===");
            System.out.println("p - 暂停");
            System.out.println("r - 恢复");
            System.out.println("s X.X - 修改速度 (例如: s 2.0 或 s 0.5)");
            System.out.println("q - 退出程序");
            System.out.println("================\n");
            
            while (!Thread.currentThread().isInterrupted()) {//若当前线程未被中断，继续循环
                if (commandThreadInputScanner.hasNextLine()) {
                    String pausedCommand = commandThreadInputScanner.nextLine().trim().toLowerCase();
                    
                    switch (pausedCommand) {
                        case "p":
                            isPaused = true;
                            System.out.println("【已暂停】TPS: " + CurrentTps + " | 当前季节: " + Season[CurrentSeasonIndex]);
                            break;
                            
                        case "r":
                            isPaused = false;
                            System.out.println("【已恢复】继续运行...");
                            break;
                            
                        case "q":
                            System.out.println("正在退出程序...");
                            System.exit(0);
                            break;
                            
                        default:
                            if (pausedCommand.startsWith("s ")) {
                                try {
                                    String[] parts = pausedCommand.split(" ");//Parts数组用于存储分割后的命令部分
                                    if (parts.length >= 2) {//数组长度在2以上，防止输入错误命令
                                        double newSpeed = Double.parseDouble(parts[1]);//将数据格式转换为Double
                                        if (newSpeed > 0) {
                                            currentSpeedMultiplier = newSpeed;
                                            System.out.println("【速度已修改】新的速度倍数: " + currentSpeedMultiplier);
                                        } else {
                                            System.out.println("错误:速度必须大于0");
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("错误：无效的速度格式。请使用 's X.X' 格式");
                                }
                            } else if (!pausedCommand.isEmpty()) {
                                System.out.println("未知命令。可用命令: p, r, s X.X, q");
                            }
                            break;
                    }
                }
            }
        });
        
        commandThread.setDaemon(true); // 设置为守护线程，主线程退出时自动结束
        commandThread.start();
    }
    
    // 停止命令处理线程
    private static void stopCommandThread() {
        if (commandThread != null && commandThread.isAlive()) {
            commandThread.interrupt();
        }
    }
  
    //用Season[CurrentSeasonIndex]索引季节名称返回值CurrentSeasonIndex
    private static void executeSeasonalEvent(){
        System.out.println("更替：" + Season[CurrentSeasonIndex]);
        switch (CurrentSeasonIndex) {
            case 0:
                System.out.println("春");
                break;
            case 1:
                System.out.println("夏");
                break;
            case 2:
                System.out.println("秋");
                break;
            case 3:
                System.out.println("冬");
                break;
        }
    }

    //计算昼夜循环
    private static void calculateDayNightCycle(){
        nightTimeLength = dayLength - dayTimeLength;
        //检测AccumulateTick是否为dayTimeLength的倍数
        if (AccumulatedTick % dayTimeLength == 0){
            System.out.println("目前为日落或日出时分");
            
        } else {

        }
    }
}

