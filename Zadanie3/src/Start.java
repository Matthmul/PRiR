import java.net.MalformedURLException;
import java.rmi.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.rmi.RemoteException;
import java.util.concurrent.*;

public class Start implements PolygonalChain {
    private Map<String, LinkedList<Position2D>> lines = new LinkedHashMap<>();
    private Map<String, Integer> linesLength = new LinkedHashMap<>();
    private Integer taskLimit;
    private PolygonalChainProcessor service;
    private ExecutorService executorService;
    private final Object sync = new Object();

    public class DaemonThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }

    public Start() throws RemoteException {
        super();
        service = null;
        taskLimit = null;
        executorService = Executors.newFixedThreadPool(2, new DaemonThreadFactory());
    }

    private class LineTask implements Callable<Boolean> {
        LinkedList<Position2D> lineList;
        String name;

        public LineTask(String name, LinkedList<Position2D> lineList) {
            this.name = name;
            this.lineList = lineList;
        }

        @Override
        public Boolean call() throws RemoteException {
//            int result = service.process(name, lineList);
            int result = 1000 * lineList.size();
            synchronized (sync) {
                if (lines.get(name).size() > lineList.size()) {
                    return true;
                }
                linesLength.replace(name, result);
            }
            System.out.println(name);
            System.out.println(Arrays.toString(lines.get(name).toArray()));
            System.out.println(Arrays.toString(lineList.toArray()));
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    @Override
    public void newPolygonalChain(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException {
        if (executorService == null) {
            return;
        }

        Future<Boolean> future;
        synchronized (sync) {
            LinkedList<Position2D> lineList = new LinkedList<>();
            lineList.add(firstPoint);
            lineList.add(lastPoint);
            lines.put(name, lineList);
            linesLength.put(name, null);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            future = executorService.submit(new LineTask(name, lineList));
        }
        try {
            future.get();
        } catch (ExecutionException ex) {
            throw (RemoteException) ex.getCause();
        } catch (InterruptedException ignored) {
        }
    }


    @Override
    public void addLineSegment(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException {
        if (executorService == null) {
            return;
        }
        if (!linesLength.containsKey(name)) {
            return;
        }

        Future<Boolean> future;
        synchronized (sync) {
            LinkedList<Position2D> lineList = lines.get(name);
            int id = 0;
            for (; id < lineList.size() - 1; ++id) {
                if (lineList.get(id) == firstPoint) {
                    break;
                }
            }
            if (lineList.getLast() != lastPoint) {
                lines.get(name).add(id, lastPoint);
            }
            linesLength.replace(name, null);

            future = executorService.submit(new LineTask(name, lineList));
        }
        try {
            future.get();
        } catch (ExecutionException ex) {
            throw (RemoteException) ex.getCause();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public Integer getResult(String name) throws RemoteException {
        synchronized (sync) {
            if (!linesLength.containsKey(name)) {
                return null;
            }
            return linesLength.get(name);
        }
    }

    @Override
    public void setPolygonalChainProcessorName(String uri) throws RemoteException {
        try {
            service = (PolygonalChainProcessor) Naming.lookup(uri);
        } catch (NotBoundException | MalformedURLException ignored) {
        }
        taskLimit = service.getConcurrentTasksLimit();
        executorService = Executors.newFixedThreadPool(taskLimit, new DaemonThreadFactory());
    }
}
