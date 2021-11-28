import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfejs systemu obsĹugujÄcego linie Ĺamane.
 */
public interface PolygonalChain extends Remote {

    /**
     * Metoda tworzy nowÄ liniÄ ĹamanÄ.
     *
     * @param name       nazwa linii Ĺamanej
     * @param firstPoint pierwszy punkt naleĹźÄcy do linii Ĺamanej
     * @param lastPoint  ostatni punkt naleĹźÄcy do linii Ĺamanej
     * @throws RemoteException wyjÄtek zgĹaszany w przypadku bĹÄdu
     */
    public void newPolygonalChain(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException;

    /**
     * Metoda dodaje odcinek do linii Ĺamanej o podanej nazwie.
     *
     * @param name       nazwa linii ĹÄmanej
     * @param firstPoint pierwszy punkt odcinka
     * @param lastPoint  ostatni punkt odcinka
     * @throws RemoteException wyjÄtek zgĹaszany w przypadku bĹÄdu
     */
    public void addLineSegment(String name, Position2D firstPoint, Position2D lastPoint) throws RemoteException;

    /**
     * Metoda zwraca wynik otrzymany dla danej linii Ĺamanej z serwisu
     * przetwarzajÄcego linie Ĺamane.
     *
     * @param name nazwa linii Ĺamanej
     * @return wynik uzyskany z serwisu przetwarzajÄcego lub null jeĹli linia nie
     *         jest znana lub nie zostaĹa jeszcze przetworzona
     * @throws RemoteException wyjÄtek zgĹaszany w przypadku wystÄpienia bĹÄdu
     */
    public Integer getResult(String name) throws RemoteException;

    /**
     * Metoda przekazuje URI umoĹźliwiajÄc dostÄp do serwisu przetwarzajÄcego linie
     * Ĺamane.
     *
     * @param uri adres serwisu
     * @throws RemoteException wyjÄtek zgĹaszany w przypadku wystpienia bĹÄdu
     */
    public void setPolygonalChainProcessorName(String uri) throws RemoteException;
}