import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class Start {
    public Start() throws RemoteException {
        ServiceStart obj = new ServiceStart();
        PolygonalChain stub = (PolygonalChain) UnicastRemoteObject.exportObject(obj, 0);
        
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("POLYGONAL_CHAIN", stub);
    }
}

class ServiceStart implements PolygonalChain {
    private Map<String, LinkedList<LinkedList<Position2D>>> lines = new LinkedHashMap<>();
    private Map<String, LinkedList<Position2D>> leftPoints = new LinkedHashMap<>();
    private Map<String, Integer> linesLength = new LinkedHashMap<>();
    private Map<String, Object> syncObj = new LinkedHashMap<>();
    private Integer taskLimit;
    private PolygonalChainProcessor service;
    private ExecutorService executorService;
    private final Object sync = new Object();

    ServiceStart() {
        super();
        service = null;
        taskLimit = null;
        executorService = null;
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
            Integer found1 = null;
            Integer found2 = null;
            if (!lineList.isEmpty()) {
                for (int i = 0; i < lineList.size(); ++i) {
                    if (lineList.get(i).getFirst().equals(lastPoint)) {
                        if (found1 == null) {
                            lineList.get(i).add(0, firstPoint);
                            found2 = i;
                        } else {
                            lineList.get(found1).remove(lineList.get(found1).size() - 1);
                            found2 = i;
                            break;
                        }
                    }

                    if (lineList.get(i).getLast().equals(firstPoint)) {
                        if (found2 == null) {
                            lineList.get(i).add(lineList.get(i).size(), lastPoint);
                            found1 = i;
                        } else {
                            lineList.get(found2).remove(0);
                            found1 = i;
                            break;
                        }
                    }
                }
                if (found1 == null && found2 == null) {
                    newList.add(firstPoint);
                    newList.add(lastPoint);
                    lineList.add(newList);
                } else if (found1 != null && found2 != null) {
                    newList.addAll(lineList.get(found1));
                    newList.addAll(lineList.get(found2));
                    lineList.add(newList);
                    if (found1 > found2) {
                        lineList.remove((int) found1);
                        lineList.remove((int) found2);
                    } else {
                        lineList.remove((int) found2);
                        lineList.remove((int) found1);
                    }
                }
            } else {
                newList.add(firstPoint);
                newList.add(lastPoint);
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
        if (!linesLength.containsKey(name)) {
            return null;
        }

        synchronized (syncObj.get(name)) {
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

class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }
}