import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.rmi.*;
import java.util.*;
import java.rmi.RemoteException;
import java.util.concurrent.*;

public class Start implements PolygonalChain {
    private Map<String, LinkedList<Position2D>> lines = new LinkedHashMap<>();
    private Map<String, LinkedList<Position2D>> leftPoints = new LinkedHashMap<>();
    private Map<String, Integer> linesLength = new LinkedHashMap<>();
    private Map<String, Object> syncObj = new LinkedHashMap<>();
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
    }

    private class LineTask implements Callable<Boolean> {
        LinkedList<Position2D> lineList;
        String name;

        public LineTask(String name, LinkedList<Position2D> lineList) {
            this.name = name;
            this.lineList = lineList;
        }

        @Override
        public Boolean call() {
            int result = 0;
            try {
                result = service.process(name, lineList);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            synchronized (syncObj.get(name)) {
                if (lines.get(name).size() > lineList.size()) {
                    return true;
                }
                linesLength.replace(name, result);
            }

            return true;
        }
    }

    @Override
    public void newPolygonalChain(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException {
        if (executorService == null) {
            return;
        }

        synchronized (sync) {
            LinkedList<Position2D> lineList = new LinkedList<>();
            lineList.add(firstPoint);
            lineList.add(lastPoint);
            lines.put(name, lineList);
            linesLength.put(name, null);
            LinkedList<Position2D> pointList = new LinkedList<>();
            pointList.add(firstPoint);
            pointList.add(lastPoint);
            leftPoints.put(name, pointList);
            syncObj.put(name, new Object());
        }
    }


    @Override
    public void addLineSegment(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException {
        if (executorService == null) {
            return;
        }
        if (!lines.containsKey(name)) {
            return;
        }

        Future<Boolean> future = null;
        synchronized (syncObj.get(name)) {
            LinkedList<Position2D> lineList = lines.get(name);
            int id = 1;
            for (; id < lineList.size() - 1; ++id) {
                if (lineList.get(id - 1).equals(firstPoint)) {
                    break;
                }
            }
            if (!lineList.getLast().equals(lastPoint)) {
                lines.get(name).add(id, lastPoint);
            }
            linesLength.replace(name, null);

            if (firstPoint.equals(lineList.get(id - 1))) {
                leftPoints.get(name).remove(lineList.get(id - 1));
            } else {
                leftPoints.get(name).add(id - 1, firstPoint);
            }
            if (lastPoint.equals(lineList.get(id))) {
                leftPoints.get(name).remove(lineList.get(id));
            } else {
                leftPoints.get(name).add(id, lastPoint);
            }

            if (leftPoints.get(name).isEmpty()) {
                future = executorService.submit(new LineTask(name, lineList));
            }
        }
    }

    @Override
    public Integer getResult(String name) throws RemoteException {
        if (executorService == null) {
            return null;
        }
        if (!lines.containsKey(name)) {
            return null;
        }

        synchronized (syncObj.get(name)) {
            return linesLength.get(name);
        }
    }

    @Override
    public void setPolygonalChainProcessorName(String uri) throws RemoteException {
        try {
            service = (PolygonalChainProcessor) Naming.lookup(uri + "/POLYGONAL_CHAIN");
        } catch (NotBoundException | MalformedURLException ignored) {
        }
        taskLimit = service.getConcurrentTasksLimit();
        executorService = Executors.newFixedThreadPool(taskLimit, new DaemonThreadFactory());
    }
}
