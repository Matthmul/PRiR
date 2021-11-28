import java.util.concurrent.atomic.AtomicIntegerArray;

public class Disk implements DiskInterface {
    public AtomicIntegerArray value;
    static private int SIZE = 100000000;
    private boolean broken = false;

    public Disk() {
        this.value = new AtomicIntegerArray(SIZE);
    }

    @Override
    public void write(int sector, int value) throws DiskError {
        if (broken)
        {
            throw new DiskError();
        }
//        System.out.println("Write " + sector + " " + value + " process " + Thread.currentThread());
        this.value.set(sector, value);
    }

    @Override
    public int read(int sector) throws DiskError {
        if (broken)
        {
            throw new DiskError();
        }
//        System.out.println("Read " + sector + " process " + Thread.currentThread());
        return value.get(sector);
    }

    public void destroy() {
        broken = true;
    }

    @Override
    public int size() {
        return SIZE;
    }
}
