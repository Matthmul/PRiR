public class RAID implements RAIDInterface {
    @Override
    public RAIDState getState() {
        return null;
    }

    @Override
    public void addDisk(DiskInterface disk) {

    }

    @Override
    public void startRAID() {

    }

    @Override
    public void replaceDisk(DiskInterface disk) {

    }

    @Override
    public void write(int sector, int value) {

    }

    @Override
    public int read(int sector) {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void shutdown() {

    }
}
