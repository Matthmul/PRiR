public class Main {
    public static RAID raid;
    public static Disk[] disks = new Disk[6];

    public static class MyRun implements Runnable {

        private int id;

        public MyRun(int id) {
            this.id = id;
        }

        public void run() {
            for (int i = 1; i < 10; ++i) {
//                System.out.println("pr id " + id + " " + raid.read(100 * id));
                raid.read(Disk.SIZE * id);
//                raid.write(Disk.SIZE * id, 10 + id + i);
                int val = raid.read(Disk.SIZE * id);
//                if (val != 10 + id + i && !raid.isShutdown) {
//                    System.out.println("In val " + val + " " + (10 + id + i) + " " + id);
//                }
//                System.out.println("po id " + id + " " + raid.read(100 * id));
//                System.out.println("---");
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }

    public static void main(String[] args) {
        raid = new RAID();
        int id = 0;
        System.out.println(raid.getState());


        for (Disk disk : disks) {
            disk = new Disk();
            for (int i = 0; i < disk.size(); ++i) {
                try {
                    disk.write(i, id);
                } catch (DiskInterface.DiskError ignored) {
                }
            }
            System.out.println(disk);
            raid.addDisk(disk);
            id++;
        }
        System.out.println(raid.getState());
        System.out.println(raid.size());

        raid.startRAID();
        System.out.println(raid.getState());

//        for (int i = 0; i < 1; ++i) {
//            System.out.println(raid.read(1));
//            raid.write(1, 11);
//            System.out.println(raid.read(1));
//        }
        int jobNum = 5;
        Runnable[] runners = new Runnable[jobNum];
        Thread[] threads = new Thread[jobNum];
        Disk d = (Disk) raid.disks.get(0);
        Disk d2 = (Disk) raid.disks.get(2);
        Disk newD = new Disk();
//        for (int i = 0; i < newD.size(); ++i) {
//            try {
//                newD.write(i, 10);
//            } catch (DiskInterface.DiskError ignored) {
//            }
//        }
        while (true) {
            if (raid.getState() == RAIDInterface.RAIDState.NORMAL)
                break;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        System.out.println(raid.getState());
        d2.destroy();
        raid.read(0);
        raid.read(10);
        raid.read(200);
        raid.read(300);
//        System.out.println(raid.getState());
        raid.replaceDisk(newD);
        System.out.println(raid.getState());
        while (true) {
            if (raid.getState() == RAIDInterface.RAIDState.NORMAL)
                break;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        System.out.println("------------------------");
        for (int a = 0; a < 3; a++) {
            for (int i = 0; i < jobNum; i++) {
                runners[i] = new MyRun(i);
                threads[i] = new Thread(runners[i]);
            }
            for (int i = 0; i < jobNum; i++) {
                threads[i].start();
            }
            for (int i = 0; i < jobNum; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (raid.getBackup(0) != 105) {
                System.out.println("Back " + raid.getBackup(0));
            }
            d2.destroy();
        }
        raid.shutdown();
    }
}
