import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SandwichManager {
    // Static variables
    static int n_sandwiches,
            bread_capacity,
            egg_capacity,
            n_bread_makers,
            n_egg_makers,
            n_sandwich_packers,
            bread_rate, 
            egg_rate,
            packing_rate;

    // Synchronized methods to get permission to make bread, egg, or sandwich
    static int requiredBread = 0, requiredEgg = 0, requiredSandwich = 0;
    static final Object allocatorMonitor = new Object(); // Monitor for the allocating ingredients to the packers
    static synchronized boolean makeBread() {
        if (requiredBread > 0) {
            requiredBread--;
            return true;
        } 
        return false;
    }
    static synchronized boolean makeEgg() {
        if (requiredEgg > 0) {
            requiredEgg--;
            return true;
        } 
        return false;
    }
    static synchronized boolean makeSandwich() {
        if (requiredSandwich > 0) {
            requiredSandwich--;
            return true;
        } 
        return false;
    }

    // Initialize the circular buffers and their producer/consumer methods
    static volatile Bread[] breadBuffer;
    static volatile Egg[] eggBuffer;
    private static final Object breadBufferLock = new Object(), eggBufferLock = new Object();
    static volatile int breadBufferFront = 0, breadBufferBack = 0, breadBufferItemCount = 0;
    static volatile int eggBufferFront = 0, eggBufferBack = 0, eggBufferItemCount = 0;
    static void putBread(Bread bread) {
        synchronized (breadBufferLock) {
            while (breadBufferItemCount == breadBuffer.length){
                // Wait for the buffer to be consumed, release the lock
                try { breadBufferLock.wait(); } catch (InterruptedException e) {}
            }
            breadBuffer[breadBufferBack] = bread;
            breadBufferBack = (breadBufferBack + 1) % breadBuffer.length;

            ++breadBufferItemCount;
            breadBufferLock.notifyAll(); 
        }
    }
    static Bread getBread() {
        synchronized (breadBufferLock) {
            while (breadBufferItemCount == 0) {
                // Wait for the buffer to be filled, release the lock
                try { breadBufferLock.wait(); } catch (InterruptedException e) {}
            }
            Bread bread = breadBuffer[breadBufferFront];
            breadBufferFront = (breadBufferFront + 1) % breadBuffer.length;

            --breadBufferItemCount;
            breadBufferLock.notifyAll();
            return bread;
        }
    }

    static void putEgg(Egg egg) {
        synchronized (eggBufferLock) {
            while (eggBufferItemCount == eggBuffer.length){
                // Wait for the buffer to be consumed, release the lock
                try { eggBufferLock.wait(); } catch (InterruptedException e) {}
            }
            eggBuffer[eggBufferBack] = egg;
            eggBufferBack = (eggBufferBack + 1) % eggBuffer.length;

            ++eggBufferItemCount;
            eggBufferLock.notifyAll(); 
        }
    }
    static Egg getEgg() {
        synchronized (eggBufferLock) {
            while (eggBufferItemCount == 0) {
                // Wait for the buffer to be filled, release the lock
                try { eggBufferLock.wait(); } catch (InterruptedException e) {}
            }
            Egg egg = eggBuffer[eggBufferFront];
            eggBufferFront = (eggBufferFront + 1) % eggBuffer.length;

            --eggBufferItemCount;
            eggBufferLock.notifyAll();
            return egg;
        }
    }

    // Synchronized method to write to the log 
    static synchronized void writeToLog(String s, boolean append) {
        try {
            FileWriter fw = new FileWriter(new File("./log.txt"), append);
            fw.write(s);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        
        // Read the command line arguments
        n_sandwiches = Integer.parseInt(args[0]);
        bread_capacity = Integer.parseInt(args[1]);
        egg_capacity = Integer.parseInt(args[2]);
        n_bread_makers = Integer.parseInt(args[3]);
        n_egg_makers = Integer.parseInt(args[4]);
        n_sandwich_packers = Integer.parseInt(args[5]);
        bread_rate = Integer.parseInt(args[6]);
        egg_rate = Integer.parseInt(args[7]);
        packing_rate = Integer.parseInt(args[8]);

        // Write the inputs to the log
        String osNewline = System.getProperty("line.separator");
        String logHeader = String.format("%s%d%s%s%d%s%s%d%s%s%d%s%s%d%s%s%d%s%s%d%s%s%d%s%s%d%s%s", 
            "sandwiches: ", n_sandwiches, osNewline,
            "bread capacity: ", bread_capacity, osNewline,
            "egg capacity: ", egg_capacity, osNewline,
            "bread makers: ", n_bread_makers, osNewline,
            "egg makers: ", n_egg_makers, osNewline,
            "sandwich packers: ", n_sandwich_packers, osNewline,
            "bread rate: ", bread_rate, osNewline,
            "egg rate: ", egg_rate, osNewline,
            "packing rate: ", packing_rate, osNewline, osNewline);
        writeToLog(logHeader, false);

        // Initialize the total required ingredients
        requiredBread = n_sandwiches * 2;
        requiredEgg = n_sandwiches;
        requiredSandwich = n_sandwiches;

        // Initialize the buffers
        breadBuffer = new Bread[bread_capacity];
        eggBuffer = new Egg[egg_capacity];

        // Create the threads
        BreadMachine[] breadMachines = new BreadMachine[n_bread_makers];
        EggMachine[] eggMachines = new EggMachine[n_egg_makers];
        SandwichMachine[] packingMachines = new SandwichMachine[n_sandwich_packers];

        // Start the threads
        for (int i = 0; i < n_bread_makers; i++) {
            breadMachines[i] = new BreadMachine(i, bread_rate);
            breadMachines[i].start();
        }
        for (int i = 0; i < n_egg_makers; i++) {
            eggMachines[i] = new EggMachine(i, egg_rate);
            eggMachines[i].start();
        }
        for (int i = 0; i < n_sandwich_packers; i++) {
            packingMachines[i] = new SandwichMachine(i, packing_rate);
            packingMachines[i].start();
        }

        // Wait for the threads to finish
        for (Thread bm : breadMachines) {
            try {
                bm.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Thread em : eggMachines) {
            try {
                em.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Thread pm : packingMachines) {
            try {
                pm.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Print out completion status with the number of sandwiches made and buffer status
        System.out.println("------------------------------\nJob status: " + (requiredSandwich == 0 ? "SUCCESS" : "FAILURE") );
        System.out.println("Sandwiches made: " + (n_sandwiches - requiredSandwich));
        System.out.println("Bread buffer: " + breadBufferItemCount);
        System.out.println("Egg buffer: " + eggBufferItemCount);

        // Get the production summary
        StringBuilder summary = new StringBuilder();
        summary.append(osNewline + "summary:" + osNewline);
        for (BreadMachine bm : breadMachines) {
            summary.append(bm.getSummary() + osNewline);
        }
        for (EggMachine em : eggMachines) {
            summary.append(em.getSummary() + osNewline);
        }
        for (SandwichMachine sm : packingMachines) {
            summary.append(sm.getSummary() + osNewline);
        }

        System.out.println(summary.toString());
        writeToLog(summary.toString(), true);

    }

    static void gowork(int n) {
        for (int i = 0; i < n; i++) {
            long m = 300000000;
            while (m > 0) {
                m--;
            }
        }
    }
}

class Bread {
    int id;
    String threadName;

    public Bread(int id, String name) {
        this.id = id;
        this.threadName = name;
    }

    @Override
    public String toString() {
        return "bread " + id + " from " + threadName;
    }
}

class BreadMachine extends Thread {
    private String threadName;
    private int rate;
    private int breadMade = 0;

    public BreadMachine(int id, int rate) {
        this.threadName = "B" + id;
        this.rate = rate;
    }

    @Override
    public void run() {
        while (SandwichManager.makeBread()) {
            SandwichManager.gowork(rate); // Making bread...
            // Put made bread into the buffer
            String log = String.format("%s puts bread %d%s", threadName, breadMade, System.getProperty("line.separator"));
            SandwichManager.writeToLog(log, true);
            SandwichManager.putBread(new Bread(breadMade++, threadName));
        }
    }

    public String getSummary() {
        return threadName + " makes " + breadMade;
    }
}

class Egg {
    int id;
    String threadName;

    public Egg(int id, String name) {
        this.id = id;
        this.threadName = name;
    }

    @Override
    public String toString() {
        return "egg " + id + " from " + threadName;
    }
}

class EggMachine extends Thread {
    private String threadName;
    private int rate;
    private int eggMade = 0;

    public EggMachine(int id, int rate) {
        this.threadName = "E" + id;
        this.rate = rate;
    }

    @Override
    public void run() {
        while (SandwichManager.makeEgg()) {
            SandwichManager.gowork(rate); // Making an egg...
            // Put made egg into the buffer
            String log = String.format("%s puts egg %d%s", threadName, eggMade, System.getProperty("line.separator"));
            SandwichManager.writeToLog(log, true);
            SandwichManager.putEgg(new Egg(eggMade++, threadName));
        }
    }

    public String getSummary() {
        return threadName + " makes " + eggMade;
    }
}

class SandwichMachine extends Thread {
    private String threadName;
    private int rate;
    private int sandwichesMade = 0;

    public SandwichMachine(int id, int rate) {
        this.threadName = "S" + id;
        this.rate = rate;
    }

    @Override
    public void run() {
        while (SandwichManager.makeSandwich()) {
            // Get ingredients from the buffer [DEADLOCK PREVENTION]
            Bread top, bottom;
            Egg egg;
            // NOTE: synchronized block is used to ensure that the three get operations are atomic to prevent deadlock on ingredient allocation
            synchronized (SandwichManager.allocatorMonitor) {
                top = SandwichManager.getBread();
                egg = SandwichManager.getEgg();
                bottom = SandwichManager.getBread();
            }

            SandwichManager.gowork(rate); // Packing sandwich...
            // Write to log
            String log = String.format("%s packs sandwich %d with bread %d from %s and egg %d from %s and bread %d from %s%s", 
                threadName, 
                sandwichesMade++, 
                top.id, 
                top.threadName, 
                egg.id,
                egg.threadName,
                bottom.id, 
                bottom.threadName,
                System.getProperty("line.separator"));
            SandwichManager.writeToLog(log, true);
        }
        
    }

    public String getSummary() {
        return threadName + " makes " + sandwichesMade;
    }
}
