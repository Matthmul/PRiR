import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;

public class Server extends UnicastRemoteObject implements PolygonalChainProcessor {

    public Server() throws RemoteException {
    }

    private void wywal() throws RemoteException {
        throw new RemoteException("err");
    }

    public static void main(String[] args) throws AccessException, RemoteException, AlreadyBoundException {
        int PORT = 1099;
        Registry registry = java.rmi.registry.LocateRegistry.createRegistry(PORT);

        registry.rebind("POLYGONAL_CHAIN", new Server());
        for (String service : registry.list()) {
            System.out.println("Service : " + service);
        }
    }

    @Override
    public int getConcurrentTasksLimit() throws RemoteException {
        return 3;
    }

    @Override
    public int process(String name, List<Position2D> polygonalChain) throws RemoteException {
        System.out.println(name + " " + Arrays.toString(polygonalChain.toArray()) + " start");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(name + " " + Arrays.toString(polygonalChain.toArray()) + " koniec");
//        wywal();
        return polygonalChain.get(0).getCol();
    }
}
