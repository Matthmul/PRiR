import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Main {

    public static class MyRun implements Runnable {

        private PolygonalChain start;
        private Integer id;

        public MyRun(PolygonalChain start, int id) {
            this.start = start;
            this.id = id;
        }

        public void run() {
            try {
                start.newPolygonalChain(id.toString(), new Position2D(id, 2), new Position2D(id, 3));
                start.addLineSegment(id.toString(), new Position2D(id, 2), new Position2D(id + 1, 4));
                start.addLineSegment(id.toString(), new Position2D(id + 1, 4), new Position2D(id, 4));
                start.addLineSegment(id.toString(), new Position2D(id, 4), new Position2D(id, 3));
                start.addLineSegment("0", new Position2D(10 - id, 2), new Position2D(10 - id - 1, 2));
//                start.addLineSegment("0", new Position2D(10 - id - 1, 2), new Position2D(10 - id, 2));

//                System.out.println(id.toString() + " " + (10 - id) + " ");
                System.out.println(id.toString() + " " + start.getResult(id.toString()));
                while (start.getResult(id.toString()) == null) {
//                while (start.getResult("0") == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(id.toString() + " " + start.getResult(id.toString()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Start start;
        PolygonalChain service;
        try {
            start = new Start();
            service = (PolygonalChain) Naming.lookup("//127.0.0.1:1099/POLYGONAL_CHAIN");

//        start.addLineSegment("A", new Position2D(1, 2), new Position2D(1, 2));

//        start.newPolygonalChain("A", new Position2D(1, 2), new Position2D(1, 3));
//        start.addLineSegment("A", new Position2D(1, 2), new Position2D(1, 2));
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            service.setPolygonalChainProcessorName("//127.0.0.1:1099/POLYGONAL_CHAIN_SERVER");
            int jobNum = 8;
//            start.newPolygonalChain("0", new Position2D(10, 2), new Position2D(10 - jobNum, 2));
//            service.newPolygonalChain("0", new Position2D(10 - jobNum, 2), new Position2D(10, 2));
            Runnable[] runners = new Runnable[jobNum];
            Thread[] threads = new Thread[jobNum];
            for (int a = 0; a < 1; a++) {
                for (int i = 0; i < jobNum; i++) {
                    runners[i] = new MyRun(service, (i));
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

        } catch (
                RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
