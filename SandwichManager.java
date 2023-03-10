package SandwichManager;
public class SandwichManager {
    static int n_sandwiches,
            bread_capacity,
            egg_capacity,
            n_bread_makers,
            n_egg_makers,
            n_sandwich_packers,
            bread_rate, egg_rate,
            packing_rate;

    static volatile int totalBread = 0, totalEgg = 0, totalSandwich = 0;

    public static void main(String[] args) {
        

        n_sandwiches = Integer.parseInt(args[0]);
        bread_capacity = Integer.parseInt(args[1]);
        egg_capacity = Integer.parseInt(args[2]);
        n_bread_makers = Integer.parseInt(args[3]);
        n_egg_makers = Integer.parseInt(args[4]);
        n_sandwich_packers = Integer.parseInt(args[5]);
        bread_rate = Integer.parseInt(args[6]);
        egg_rate = Integer.parseInt(args[7]);
        packing_rate = Integer.parseInt(args[8]);

        StringBuilder str = new StringBuilder();
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

        // Initialize the total mutexes
        
    }
}
