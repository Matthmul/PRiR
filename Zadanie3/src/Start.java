import java.net.MalformedURLException;
import java.rmi.*;
import java.util.*;
import java.rmi.RemoteException;
import java.util.concurrent.*;

public class Start implements PolygonalChain {
    private Map<String, LinkedList<LinkedList<Position2D>>> lines = new LinkedHashMap<>();
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
            lines.put(name, new LinkedList<LinkedList<Position2D>>());
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
        if (!leftPoints.containsKey(name)) {
            return;
        }

        Future<Boolean> future = null;
        synchronized (syncObj.get(name)) {
            LinkedList<Position2D> newList = new LinkedList<>();
            LinkedList<LinkedList<Position2D>> lineList = lines.get(name);
            Integer found = null;
            if (!lineList.isEmpty()) {
                int i = 0;
                Iterator<LinkedList<Position2D>> l = lineList.iterator();
                while (l.hasNext()) {
                    LinkedList<Position2D> p = l.next(); // must be called before you can call i.remove()
                    if (p.getFirst().equals(lastPoint)) {
                        if (found == null) {
                            lineList.get(i).add(0, firstPoint);
                            found = i;
                        } else {
                            lineList.get(found).addAll(lineList.get(i));
                            lineList.remove(found);
                        }
                    }

                    if (p.getLast().equals(firstPoint)) {
                        if (found == null) {
                            p.add(p.size(), lastPoint);
                            found = i;
                        } else {
                            p.addAll(lineList.get(found));
                            l.remove();
                        }
                    }
                }
                if (found == null) {
                    newList.add(firstPoint);
                    newList.add(lastPoint);
                }
            } else {
                newList.add(firstPoint);
                newList.add(lastPoint);
            }
            if (!newList.isEmpty()) {
                lineList.add(newList);
            }


            boolean f1 = false, f2 = false;
            List<Position2D> points = leftPoints.get(name);
            Iterator<Position2D> i = points.iterator();
            while (i.hasNext()) {
                Position2D p = i.next(); // must be called before you can call i.remove()
                if (firstPoint.equals(p)) {
                    f1 = true;
                    i.remove();
                }
                if (lastPoint.equals(p)) {
                    f2 = true;
                    i.remove();
                }
                if (f1 & f2) {
                    break;
                }
            }
            if (!f1) {
                leftPoints.get(name).add(firstPoint);
            }
            if (!f2) {
                leftPoints.get(name).add(lastPoint);
            }

            if (leftPoints.get(name).isEmpty()) {
                linesLength.replace(name, null);
                future = executorService.submit(new LineTask(name, lineList.getFirst()));
            }
        }
    }

    @Override
    public Integer getResult(String name) throws RemoteException {
        if (executorService == null) {
            return null;
        }
        if (!leftPoints.containsKey(name)) {
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
