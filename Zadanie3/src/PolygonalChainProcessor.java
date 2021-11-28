import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PolygonalChainProcessor extends Remote {

    /**
     * Metoda pozwala na poznanie limitu wspĂłĹbieĹźnych konwersji, ktĂłre moĹźe
     * realizowaÄ serwis. Limit nie ulega zmianie w trakcie pracy serwisu.
     *
     * @return limit wspĂłĹbieĹźnych zadaĹ przetwarzania linii Ĺamanych.
     * @throws RemoteException wyjÄtek sygnalizujÄcy wystÄpinie bĹÄdu
     */
    public int getConcurrentTasksLimit() throws RemoteException;

    /**
     * Metoda dokonuje przetworzenia linii Ĺamanej. Wynikiem jest pojedyncza liczba
     * typu caĹkowitego.
     *
     * @param name           nazwa linii łamanej
     * @param polygonalChain lista istotnych punktów należących do linii łamanej. Pierwszy punkt na liście to początek
     *                       linii, ostatni to koniec linii, punkty pośrednie to początki kolejnych odcinków
     *                       prowadzących od początku linii po jej koniec. Uwaga: punkt pierwszy jest jednocześnie
     *                       początkiem linii łamanej i pierwszego odcinka - nie powinien on zostać powielony.
     * @return wynik przetworzenia linii Ĺamanej.
     * @throws RemoteException sygnalizacja bĹÄdu
     */
    public int process(String name, List<Position2D> polygonalChain) throws RemoteException;

}