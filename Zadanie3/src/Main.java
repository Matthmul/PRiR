import java.rmi.RemoteException;

public class Main {

    public static class MyRun implements Runnable {

        private Start start;
        private Integer id;

        public MyRun(Start start, int id) {
            this.start = start;
            this.id = id;
        }

        public void run() {
            try {
                start.newPolygonalChain(id.toString(), new Position2D(1, 2), new Position2D(1, 3));
                start.addLineSegment(id.toString(), new Position2D(1, 2), new Position2D(1, 4));

                System.out.println(start.getResult(id.toString()));

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Start start;
        try {
            start = new Start();

//        start.addLineSegment("A", new Position2D(1, 2), new Position2D(1, 2));

//        start.newPolygonalChain("A", new Position2D(1, 2), new Position2D(1, 3));
//        start.addLineSegment("A", new Position2D(1, 2), new Position2D(1, 2));
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            int jobNum = 4;
            Runnable[] runners = new Runnable[jobNum];
            Thread[] threads = new Thread[jobNum];
            for (int a = 0; a < 1; a++) {
                for (int i = 0; i < jobNum; i++) {
                    runners[i] = new MyRun(start, (i%3));
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
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
