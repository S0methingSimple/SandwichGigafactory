public class SandwichManager {
    static int n_sandwiches,
            bread_capacity,
            egg_capacity,
            n_bread_makers,
            n_egg_makers,
            n_sandwich_packers,
            bread_rate, egg_rate,
            packing_rate;

    static volatile int requiredBread = 0, requiredEgg = 0, requiredSandwich = 0;
    private static final Object breadLock = new Object(), eggLock = new Object(), sandwichLock = new Object(), allocatorLock = new Object();
    static boolean makeBread() {
        synchronized (breadLock) {
            if (requiredBread > 0) {
                requiredBread--;
                return true;
            } 
            return false;
        }
    }
    static boolean makeEgg() {
        synchronized (eggLock) {
            if (requiredEgg > 0) {
                requiredEgg--;
                return true;
            } 
            return false;
        }
    }
    static boolean makeSandwich() {
        synchronized (sandwichLock) {
            if (requiredSandwich > 0) {
                requiredSandwich--;
                return true;
            } 
            return false;
        }
    }
    static Ingredients allocateIngredients() {
        synchronized (allocatorLock) {
            Bread top = getBread();
            Bread bottom = getBread();
            return new Ingredients(top, bottom);
        }
        
    }

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
            String log = String.format(" %s puts bread %d", bread.threadName, bread.id);
            System.out.println("Item count: " + breadBufferItemCount + log);
            SandwichManager.writeToLog(log + System.getProperty("line.separator"));
            breadBufferItemCount++;
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
            System.out.println("Item count: " + breadBufferItemCount + ", Consuming " + bread);
            breadBufferItemCount--;
            breadBufferLock.notifyAll();
            return bread;
        }
    }

    static volatile StringBuilder str = new StringBuilder();
    private static final Object strLock = new Object();
    static void writeToLog(String s) {
        synchronized (strLock) {
            str.append(s);
        }
    }


    public static void main(String[] args) {
        
        // Read the command line arguments
        // n_sandwiches = Integer.parseInt(args[0]);
        // bread_capacity = Integer.parseInt(args[1]);
        // egg_capacity = Integer.parseInt(args[2]);
        // n_bread_makers = Integer.parseInt(args[3]);
        // n_egg_makers = Integer.parseInt(args[4]);
        // n_sandwich_packers = Integer.parseInt(args[5]);
        // bread_rate = Integer.parseInt(args[6]);
        // egg_rate = Integer.parseInt(args[7]);
        // packing_rate = Integer.parseInt(args[8]);

        // Test inputs
        n_sandwiches = 10;
        bread_capacity = 5;
        egg_capacity = 5;
        n_bread_makers = 2;
        n_egg_makers = 2;
        n_sandwich_packers = 2;
        bread_rate = 1;
        egg_rate = 1;
        packing_rate = 1;

        // Initialize the total mutexes
        requiredBread = n_sandwiches * 2;
        requiredEgg = n_sandwiches;
        requiredSandwich = n_sandwiches;

        // Initialize the buffers
        breadBuffer = new Bread[bread_capacity];
        eggBuffer = new Egg[egg_capacity];
        
        // Write the inputs to the log
        String newLine = System.getProperty("line.separator");
        str.append("sandwiches: " + n_sandwiches + newLine);
        str.append("bread capacity: " + bread_capacity + newLine);
        str.append("egg capacity: " + egg_capacity + newLine);
        str.append("bread makers: " + n_bread_makers + newLine);
        str.append("egg makers: " + n_egg_makers + newLine);
        str.append("sandwich packers: " + n_sandwich_packers + newLine);
        str.append("bread rate: " + bread_rate + newLine);
        str.append("egg rate: " + egg_rate + newLine);
        str.append("packing rate: " + packing_rate + newLine + newLine);

        // Create the threads
        BreadMachine[] breadMachines = new BreadMachine[n_bread_makers];
        SandwichMachine[] packingMachines = new SandwichMachine[n_sandwich_packers];

        for (int i = 0; i < n_bread_makers; i++) {
            breadMachines[i] = new BreadMachine(i, bread_rate);
            breadMachines[i].start();
        }

        for (int i = 0; i < n_sandwich_packers; i++) {
            packingMachines[i] = new SandwichMachine(i, packing_rate);
            packingMachines[i].start();
        }

        for (Thread t : breadMachines) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Thread t : packingMachines) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("completed");
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

class Ingredients {
    Bread top;
    Bread bottom;
    public Ingredients(Bread top, Bread bottom) {
        this.top = top;
        this.bottom = bottom;
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
        return "bread " + id;
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
            SandwichManager.gowork(rate);
            Bread bread = new Bread(breadMade++, threadName);
            SandwichManager.putBread(bread);
        }
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
        return "egg " + id;
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
            SandwichManager.gowork(rate);
            Ingredients ingredients = SandwichManager.allocateIngredients();
            String log = String.format("%s packs sandwich %d with bread %d from %s and bread %d from %s ", threadName, sandwichesMade++, ingredients.top.id, ingredients.top.threadName, ingredients.bottom.id, ingredients.bottom.threadName);
            System.out.println(log);
            SandwichManager.writeToLog(log + System.getProperty("line.separator"));
        }
        
    }
}
