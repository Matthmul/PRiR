import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class RAID implements RAIDInterface {
    private RAIDState state;
    public ArrayList<DiskInterface> disks = new ArrayList<DiskInterface>();
    private ArrayList<Boolean> broken = new ArrayList<Boolean>();
    private ArrayList<ReentrantLock> disksLock = new ArrayList<ReentrantLock>();
    public DiskInterface backupDisk;
    private int matrixSize;
    private int diskSize;
    public boolean isShutdown;

    public RAID() {
        state = RAIDState.UNKNOWN;
        isShutdown = false;
    }

    @Override
    public RAIDState getState() {
        return state;
    }

    @Override
    public void addDisk(DiskInterface disk) {
        if (isShutdown) {
            return;
        }
        if (state != RAIDState.UNKNOWN) {
            return;
        }
        if (!disks.isEmpty()) {
            matrixSize += disk.size();
            diskSize = disk.size();
        }
        disks.add(disk);
        broken.add(Boolean.FALSE);
        disksLock.add(new ReentrantLock());
    }

    private class InitTask extends Thread {
        public InitTask() {
        }

        public void run() {
            for (int i = 0; i < diskSize; ++i) {
                if (isShutdown) {
                    return;
                }
                int sum = 0;
                for (DiskInterface disk : disks) {
                    try {
                        sum += disk.read(i);
                    } catch (DiskInterface.DiskError ignored) {
                    }
                }
                try {
                    backupDisk.write(i, sum);
                } catch (DiskInterface.DiskError ignored) {
                }
            }
            state = RAIDState.NORMAL;
        }
    }

    @Override
    public void startRAID() {
        if (isShutdown) {
            return;
        }
        if (state != RAIDState.UNKNOWN) {
            return;
        }

        backupDisk = disks.get(disks.size() - 1);
        disks.remove(disks.size() - 1);

        InitTask job = new InitTask();
        job.start();
        state = RAIDState.INITIALIZATION;
    }

    private class RebuildTask extends Thread {
        private int diskNum;

        public RebuildTask(int diskNum) {
            this.diskNum = diskNum;
        }

        public void run() {
            for (int i = 0; i < diskSize; ++i) {
                if (isShutdown) {
                    return;
                }
                int num = 0;
                int val = 0;
                try {
                    disksLock.get(disks.size()).lock();

                    for (; num < disks.size(); ++num) {
                        if (broken.get(num)) {
                            continue;
                        }
                        try {
                            disksLock.get(num).lock();
                            val += disks.get(num).read(i);
                        } catch (DiskInterface.DiskError ignored) {
                        } finally {
                            disksLock.get(num).unlock();
                        }
                    }
                    if (diskNum == disks.size()) {
                        backupDisk.write(i, val);
                    } else {
                        val = backupDisk.read(i) - val;
                        disks.get(diskNum).write(i, val);
                    }
                } catch (DiskInterface.DiskError ignored) {
                } finally {
                    disksLock.get(disks.size()).unlock();
                }
            }
            broken.set(diskNum, false);
            state = RAIDState.NORMAL;
        }
    }

    @Override
    public void replaceDisk(DiskInterface disk) {
        if (isShutdown) {
            return;
        }
        if (state != RAIDState.DEGRADED) {
            return;
        }

        state = RAIDState.REBUILD;
        int diskNum = 0;

        for (Boolean b : broken) {
            if (b) {
                break;
            }
            ++diskNum;
        }

        if (diskNum == disks.size()) {
            backupDisk = disk;
        } else {
            disks.set(diskNum, disk);
        }

        RebuildTask job = new RebuildTask(diskNum);
        job.start();
    }

    private void writeTask(int sector, int value) {
        int diskNum = sector / diskSize;
        int diskSector = sector % diskSize;

        int old_val = 0;
        if (broken.get(diskNum)) {
            if (!broken.get(disks.size())) {
                int num = 0;
                int val = 0;
                try {
                    disksLock.get(disks.size()).lock();
                    disksLock.get(diskNum).lock();
                    for (; num < disks.size(); ++num) {
                        if (broken.get(num)) {
                            continue;
                        }
                        try {
                            val += disks.get(num).read(diskSector);
                        } catch (DiskInterface.DiskError diskError) {
                            state = RAIDState.DEGRADED;
                            broken.set(num, Boolean.TRUE);
                        }
                    }
                    if (state == RAIDState.REBUILD) {
                        disks.get(diskNum).write(diskSector, value);
                    }
                    backupDisk.write(diskSector, val + value);
                } catch (DiskInterface.DiskError diskError) {
                    state = RAIDState.DEGRADED;
                    broken.set(disks.size(), Boolean.TRUE);
                } finally {
                    disksLock.get(diskNum).unlock();
                    disksLock.get(disks.size()).unlock();
                }
            }
        } else {
            try {
                disksLock.get(disks.size()).lock();
                disksLock.get(diskNum).lock();
                try {
                    old_val = disks.get(diskNum).read(diskSector);
                    disks.get(diskNum).write(diskSector, value);
                } catch (DiskInterface.DiskError diskError) {
                    state = RAIDState.DEGRADED;
                    broken.set(diskNum, Boolean.TRUE);
                    this.writeTask(sector, value);
                    return;
                }
                if (!broken.get(disks.size())) {
                    try {
                        int tmp = backupDisk.read(diskSector);
                        tmp -= old_val;
                        backupDisk.write(diskSector, tmp + value);
                    } catch (DiskInterface.DiskError ignored) {
                        state = RAIDState.DEGRADED;
                        broken.set(broken.size() - 1, Boolean.TRUE);
                    }
                }
            } finally {
                disksLock.get(diskNum).unlock();
                disksLock.get(disks.size()).unlock();
            }
        }
    }

    @Override
    public void write(int sector, int value) {
        if (isShutdown) {
            return;
        }
        if (state == RAIDState.UNKNOWN || state == RAIDState.INITIALIZATION) {
            return;
        }
        if (sector / diskSize >= disks.size()) {
            return;
        }

        writeTask(sector, value);
    }


    private int readTask(int sector) {
        int diskNum = sector / diskSize;
        int diskSector = sector % diskSize;
        int value = 0;

        if (broken.get(diskNum)) {
            if (!broken.get(disks.size())) {
                int num = 0;
                int val = 0;
                try {
                    disksLock.get(disks.size()).lock();
                    disksLock.get(diskNum).lock();
                    for (; num < disks.size(); ++num) {
                        if (broken.get(num)) {
                            continue;
                        }
                        try {
                            disksLock.get(num).lock();
                            val += disks.get(num).read(diskSector);
                        } catch (DiskInterface.DiskError diskError) {
                            state = RAIDState.DEGRADED;
                            broken.set(num, Boolean.TRUE);
                        } finally {
                            disksLock.get(num).unlock();
                        }
                    }
                    value = backupDisk.read(diskSector);
                    value -= val;
                } catch (DiskInterface.DiskError diskError) {
                    state = RAIDState.DEGRADED;
                    broken.set(disks.size(), Boolean.TRUE);
                } finally {
                    disksLock.get(diskNum).unlock();
                    disksLock.get(disks.size()).unlock();
                }
            }
        } else {
            try {
                disksLock.get(diskNum).lock();
                value = disks.get(diskNum).read(diskSector);
            } catch (DiskInterface.DiskError diskError) {
                state = RAIDState.DEGRADED;
                broken.set(diskNum, Boolean.TRUE);
                value = this.readTask(sector);
            } finally {
                disksLock.get(diskNum).unlock();
            }
        }
        return value;
    }

    @Override
    public int read(int sector) {
        if (isShutdown) {
            return 0;
        }
        if (state == RAIDState.UNKNOWN || state == RAIDState.INITIALIZATION) {
            return 0;
        }
        if (sector / diskSize >= disks.size()) {
            return 0;
        }

        return readTask(sector);
    }

    @Override
    public int size() {
        if (isShutdown) {
            return 0;
        }
        return matrixSize;
    }

    @Override
    public void shutdown() {
        state = RAIDState.UNKNOWN;
        isShutdown = true;
    }

    public int getBackup(int sector) {
        int val = 0;
        try {
            disksLock.get(disks.size()).lock();

            val = backupDisk.read(sector);
        } catch (DiskInterface.DiskError ignored) {
        } finally {
            disksLock.get(disks.size()).unlock();
        }
        return val;
    }
}
