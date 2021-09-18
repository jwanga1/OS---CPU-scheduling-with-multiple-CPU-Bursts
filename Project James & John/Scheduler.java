import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Scheduler {

    static Process nullProc;
    static String cpuUsage = "";
    //    static double[] stats = {0.0, 0.0, 0.0};
    static int quanta = 0;
    static int mask = 0;

    public static void main(String[] args) {
        LinkedList<Process> processQueue = new LinkedList<>();
        LinkedList<Process> newQ = new LinkedList<>();
        LinkedList<Process> readyQ = new LinkedList<>();
        LinkedList<Process> runningQ = new LinkedList<>();
        LinkedList<Process> eventList = new LinkedList<>();

        int contextSwitchCount = 0;

        // decide on file or console for input
        // format of data: pid, arrival time, cpu burst rate, priority
        // work on
        getData(processQueue);

        // do each algorithm
        // queues: process queue: holds process until arrival time reached
        //         waiting queue: holds process waiting - make note of arrival time
        //         running queue: process running - decrement cpu burst time, increment cpu time
        //
        // FCFS
        System.out.println("FCFS\n----");
        mask = 1; //fcfs
        initData(processQueue, newQ, readyQ, runningQ, eventList, mask);
        quanta = 0;
        fcfs(newQ, readyQ, runningQ, eventList, contextSwitchCount, mask);
        System.out.println();

        // Round Robin
        //  get time slice
        //  do FCFS until time slice expired
        //  if time slice expired, place on waiting queue
        //  if process finished place on termination queue
        //  continue until only process in ready queue and running queue is nullProcess
        //  print out statistics
        System.out.println("RoundRobin\n----------");
        mask = 2;// rr
        quanta = 5;
        initData(processQueue, newQ, readyQ, runningQ, eventList, mask);
        rr(newQ, readyQ, runningQ, eventList, contextSwitchCount, mask);
        System.out.println();

        // Priority Queue
        //  preemptive on priority
        //  continue until process queue empty
        //  print out statistics
        System.out.println("Priority\n--------");
        mask = 4;
        quanta = 5;
        initData(processQueue, newQ, readyQ, runningQ, eventList, mask);
        priority(newQ, readyQ, runningQ, eventList, contextSwitchCount, mask);
        System.out.println();

        // Multi-Level Queue
        // pre-emptive on quanta, anything exceeded is placed on lower priority queue with longer quanta
        // continue until only null process left
        // print out statistics
        System.out.println("Multi-Level Queue\n--------");
        mask = 6; // multiQ has both quanta and priority features
        quanta = 5; // the default quanta, processes may change this
        initData(processQueue, newQ, readyQ, runningQ, eventList, mask);
        multiLevelQ(newQ, readyQ, runningQ, eventList, contextSwitchCount, mask);

    }

    public static void output(String cpuUsage) {
        System.out.println("CPU: Usage by PID");
        System.out.println(cpuUsage);
        // prof said didn't need
        //       System.out.format("Average time waiting: %.2f\n", stats[1] / stats[0]);
        //       System.out.format("Average turnaround time: %.2f\n\n", stats[2] / stats[0]);
    }

    public static void initData(LinkedList<Process> procQ, LinkedList<Process> newQ, LinkedList<Process> readyQ,
                                LinkedList<Process> runningQ, LinkedList<Process> eventList, int mask) {
        // good
        Random rand = new Random();
        //       stats[0] = 0.0; // number of processes
        //       stats[1] = 0.0; //time waiting
        //       stats[2] = 0.0; //turnaround time
        newQ.clear();
        readyQ.clear();
        runningQ.clear();
        for (Process process : procQ) {
            Process tmp = Process.makeCopy(process); // check for multi
            tmp.timeFirstSeen = -1;
            if (mask == 4) { // priority
                tmp.priority = 0;
                if (tmp.cpuBurstRate <= 10)
                    tmp.priority += 1;
                if (tmp.cpuBurstRate <= 5)
                    tmp.priority += 1;
            } else tmp.priority = 2; //fcfs, rr, mlq
            insert(newQ, tmp);
        }
        for (Process proc : procQ)
            System.out.println(proc.initToString());
        System.out.println();
        nullProc = new Process(-1, 0, 1, -1);
        newQ.addFirst(nullProc);
        cpuUsage = "|";
    }

    public static void insert(LinkedList<Process> newQ, Process process) {
        // fix this for priority
        boolean inserted = false;
        for (int i = 0; i < newQ.size() && !inserted; i++) {
            if (process.arrivalTime >= newQ.get(i).arrivalTime && process.priority <= newQ.get(i).priority) {
                newQ.add(i, process);
                inserted = true;
            } else if (process.arrivalTime > newQ.get(i).arrivalTime) {
                // insert here
                //check priority
                newQ.add(i, process);
                inserted = true;
            }
        }
        if (!inserted)
            newQ.add(newQ.size(), process);
    }

    public static void fcfs(LinkedList<Process> newQ, LinkedList<Process> readyQ, LinkedList<Process> runningQ,
                            LinkedList<Process> eventList, int contextSwitchCount, int mask) {
        //  set cpu time = 0
        //  move any process from process queue to ready queue whose arrival time = cpu time
        //  if process in runningQueue finished
        //      then update process statistic, terminate, bring in next process from readyQueue, update stats in readyQueue
        //      else update process statistics, update process statistics in readyQueue, check processQueue
        //  update waiting queue - add any process to waiting queue whose arrival time has been reached
        //  continue until process in cpu is finished. note arrival time, first service time, finish time
        //  terminate process in cpu, bring in next from waiting queue
        //  continue until process queue empty
        //  print out statistics
        int cpuTime = 0;
        newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        while (newQ.size() + readyQ.size() + runningQ.size() + eventList.size() > 1) { //have to fix this for eventQueue
            updateProcessQueue(readyQ, runningQ, eventList, cpuTime++, false, mask);
            newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        }
        output(cpuUsage);
    }

    public static void rr(LinkedList<Process> newQ, LinkedList<Process> readyQ, LinkedList<Process> runningQ,
                          LinkedList<Process> eventList, int contextSwitchCount, int mask) {
        int cpuTime = 0;
//        System.out.println("RoundRobin\n----------");
        newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        // anything to fix here for eventQ??
        while (newQ.size() + readyQ.size() + runningQ.size() + eventList.size() > 1) {
            if (updateProcessQueue(readyQ, runningQ, eventList, cpuTime++, false, mask))
                quanta = 5;
            newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        }
        output(cpuUsage);
    }

    public static void priority(LinkedList<Process> newQ, LinkedList<Process> readyQ, LinkedList<Process> runningQ,
                                LinkedList<Process> eventList, int contextSwitchCount, int mask) {
        int cpuTime = 0;
        //       System.out.println("Priority\n--------");
        newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        while (newQ.size() + readyQ.size() + runningQ.size() + eventList.size() > 1) { //fix for eventQueue
            updateProcessQueue(readyQ, runningQ, eventList, cpuTime++, true, mask);
            newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        }
        output(cpuUsage);
    }

    public static void multiLevelQ(LinkedList<Process> newQ, LinkedList<Process> readyQ, LinkedList<Process> runningQ,
                                   LinkedList<Process> eventList, int contextSwitchCount, int mask) {
        // multi-level Q
        // quanta determines time-slice
        // anything exceeding quanta is placed in lower priority queue with longer time slice
        //      if priority = 2, quanta = 5
        //                    1, quanta = 10
        //                    0, quanta = 20
        // values set in moveReadyToRunning
        int cpuTime = 0;
//        System.out.println("Multi-Level Queue\n--------");
        newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        while (newQ.size() + readyQ.size() + runningQ.size() + eventList.size() > 1) { //fix for eventQueue +eventList.size()
            updateProcessQueue(readyQ, runningQ, eventList, cpuTime++, true, mask);// quanta 5
            newArrival(newQ, readyQ, cpuTime, mask); //brings in process from newQ to readyQ
        }
        output(cpuUsage);
    }

    public static boolean updateProcessQueue(LinkedList<Process> readyQ, LinkedList<Process> runningQ,
                                             LinkedList<Process> eventList, int cpuTime, boolean priority, int mask) {
        //update waiting time for any process in readyQ
        //move any processes from newQ to readyQ whose arrival time it is - put in correct slot
        //if there is a process in runningQ - check if timed out/priority overriden/finished
        //  timed out - put back on readyQ
        //  priority override - put back on readyQ
        //  finished - write stats and remove
        //if nothing in runningQ check if readyQ is empty
        //      if readyQ empty - do nothing
        //      if readyQ not empty, fill runningQ
        // returns true if process running is either terminated, timed out, or forced out via priority
        boolean runningProcStopped = false;


        // if ....&&.... && higher priority just brought in, kick current process
        if (!readyQ.isEmpty() && !runningQ.isEmpty() && readyQ.getLast().priority > runningQ.getFirst().priority) {
            moveRunningToReady(readyQ, runningQ, mask);
        }
        if (runningQ.isEmpty()) { //bring proc to be in runningQ so waitingTime doesn't get increased
            moveReadyToRunning(readyQ, runningQ, cpuTime, mask);
        }
        incWaitingTime(readyQ); //increments waiting time for any process in readyQ
        incEventList(eventList, readyQ, mask, cpuTime + 1); // dec time in eventList -> moves any proc = 0 to readyQ
        quanta = quanta - 1;
        cpuUsage = cpuUsage + runningQ.getFirst().pid + "|";
        runningQ.getFirst().cpuTimeUsed += 1; //we ran!!
        if (runningQ.getFirst().cpuTimeUsed >= runningQ.getFirst().cpuBurstRate) { //is it finished?; > for nullProc
            if (runningQ.getFirst() == nullProc)
                moveRunningToReady(readyQ, runningQ, mask);
            else {
                // process has finished cpu burst
                // have to consider multiple cpu bursts separated by events
                //   if there is an event - put on event list
                //   if there isn't an event, then the process is finished - send to terminateQ
                if (mask == 6)      // raise MLQ process after finishing CPU burst
                    runningQ.getFirst().priority = 2;
                whichQNext(runningQ, eventList, cpuTime + 1);
                //              terminate(runningQ, cpuTime + 1);  // change for 2nd part
            }
            runningProcStopped = true;
        } else if (quanta == 0) {                            // used up time slice
            // used up time slice - being kicked
            // have to check if running in multiQ
            //    if multiQ, put back in readyQ with lower priority
            if (mask == 6) { // timed out, multiQ, reduce priority if priority > 0; continue as usual
                if (runningQ.getFirst().priority > 0)
                    runningQ.getFirst().priority--;
                else if (runningQ.getFirst().priority == 0) // don't want to chance decrementing nullproc
                    // case where process went all through the queues and didn't finish; reset priority
                    runningQ.getFirst().priority = 2;
            }
            moveRunningToReady(readyQ, runningQ, mask);
            runningProcStopped = true;
        } else if (readyQ.getLast().priority > runningQ.getFirst().priority) {//higher priority in readyQ
            // check if pre-empting needed
            //      kick current process if readyQ process has higher priority than runningQ process
            //      should never get here from FCFS or RR
            //      only for priority and multiQ
            //      if multi-Q process is being pre-empted, don't lower priority
            moveRunningToReady(readyQ, runningQ, mask);
            runningProcStopped = true;
        }
        return runningProcStopped;  //mostly for rr
    }

    public static void moveRunningToReady(LinkedList<Process> readyQ, LinkedList<Process> runningQ, int mask) {
        // add runningQ process to waiting queue - could be different depending on priority
        //  fcfs - doesn't need/ should never get here
        //  rr - adds to first position
        //  priority inserts by priority
        // nullProc always added to first position
        Process tmp;
        tmp = runningQ.getFirst();
        if (tmp == nullProc) {
            readyQ.addFirst(tmp);
        } else if (mask == 2) { // rr case
            readyQ.removeFirst();
            readyQ.addFirst(tmp);
            readyQ.addFirst(nullProc);
        } else if (mask == 4) { //priority case
            insertByPriority(readyQ, tmp);
        } else if (mask == 6)
            insertByPriority(readyQ, tmp);
        runningQ.clear();
    }

    public static void insertByPriority(LinkedList<Process> readyQ, Process tmp) {
        // insert tmp into readyQ, priority is only consideration
        // nullProc should never see this method - shouldn't have to consider its priority
        readyQ.removeFirst();
        int i = 0;
        while (i < readyQ.size() && tmp.priority > readyQ.get(i).priority) {
            i++;
        }
        readyQ.add(i, tmp);//?
        readyQ.addFirst(nullProc);
    }

    public static void moveReadyToRunning(LinkedList<Process> readyQ, LinkedList<Process> runningQ, int cpuTime, int mask) {
        // moves last element of readyQ to runningQ
        Process tmp;
        tmp = readyQ.getLast();
        runningQ.addFirst(tmp);
        if (runningQ.getFirst().timeFirstSeen == -1)   // ?? check
            runningQ.getFirst().timeFirstSeen = cpuTime;  // ?? this is correct
        readyQ.remove(tmp);
        if (runningQ.getFirst() != nullProc)
            switch (mask) {
                case 1: //fcfs
                    quanta = -1;
                    break;
                case 2: // RR
                    quanta = 5;
                    break;
                case 4: // priority
                    quanta = -1;
                    break;
                case 6: //multi-level queue
                    switch (runningQ.getFirst().priority) {
                        case 0: // mult-level Q already at lowest priority, quanta = 20
                            quanta = 20;
                            break;
                        case 1: // multi-level Q, set quanta = 10
                            quanta = 10;
                            break;
                        case 2: // ... quanta = 5
                            quanta = 5;
                            break;
                    }
            }
        else quanta = -1; // in the case of nullProc
    }


    public static void getData(LinkedList<Process> processQ) {
        //brings in data from a file
        //  or creates data given number of processes from keyboard
        Scanner cin = new Scanner(System.in);
        Random irand = new Random();
        String ans;
        int numProc;
        int arrivalTime, cpuBurstRate;
        int numEvents;
        Process tmp;
        System.out.print("Enter data by [F]ile or [K]eyboard: ");
        ans = cin.nextLine();
        ans = ans.length() == 0 ? "K" : (ans.toUpperCase().equals("F") ? "F" : "K");
        if (ans.equals("K")) {
            // get number of processes to create
            System.out.print("Number of processes: ");
            numProc = cin.nextInt();
            cin.nextLine();
            for (int i = 0; i < numProc; i++) {
                tmp = new Process();
                tmp.pid = i;
                tmp.arrivalTime = irand.nextInt(5);   // number of processes to create
                //               cpuBurstRate = 1 + irand.nextInt(11);// have to change this
                // implementing multiple bursts
                //  create at least 2 cpu bursts per process, 1 fewer events than cpu burst
                //  enter into arrays in process
                numEvents = 3 + 2 * irand.nextInt(10);
                for (int j = 0; j < numEvents; j++) {
                    if (j % 2 == 0)
                        tmp.cpuCycles.add(j / 2, 2 + irand.nextInt(10));
                    else tmp.eventCycles.add(j / 2, 1 + irand.nextInt(4));
                }
                tmp.cpuBurstRate = tmp.cpuCycles.get(0);
                tmp.cpuCycles.remove(0);
                tmp.priority = 2;
                //               tmp = new Process(i, arrivalTime, cpuBurstRate, 2);
                processQ.addLast(tmp);
            }
        } else {
            try {
                Scanner fin = new Scanner(new File("data.txt"));
                // enter data either by user or file
                // if user - get number of processes to create and generate process info randomly
                // if file - get process arrival and burst times
                for (int i = 0; fin.hasNextLine(); i++) {
                    tmp = new Process();
                    tmp.pid = i;
                    Scanner dataIn = new Scanner(fin.nextLine());
                    tmp.arrivalTime = dataIn.nextInt();
                    tmp.cpuBurstRate = dataIn.nextInt();
                    for (; dataIn.hasNextInt(); ) {
                        tmp.eventCycles.add(dataIn.nextInt());
                        tmp.cpuCycles.add(dataIn.nextInt());
                    }
                    //tmp = new Process(i, arrivalTime, cpuBurstRate, 2);
                    processQ.addLast(tmp);
                }
            } catch (FileNotFoundException eMsg) {
                System.out.println(eMsg);
            }
        }
//        for (Process proc : processQ)
//            System.out.println(proc.initToString());
    }

    public static void incWaitingTime(LinkedList<Process> readyQ) {
        for (Process p : readyQ) {
            p.timeWaiting += 1;
        }
    }

    public static void newArrival(LinkedList<Process> newQ, LinkedList<Process> readyQ, int cpuTime, int mask) {
        // new arrival moves process from newQ to readyQ if arrival time == cpuTime
        // have to consider moving from eventList to readyQ
        Process tmp;
        int j;
        for (int i = newQ.size() - 1; i >= 0; i--) {
            boolean inserted = false;
            tmp = newQ.get(i);
            if (tmp.arrivalTime == cpuTime) {
                if (tmp == nullProc)
                    readyQ.addFirst(tmp);
                else {
                    if (tmp.timeStarted == 0)
                        tmp.timeStarted = cpuTime;
                    //              if (tmp.timeFirstSeen == 0)
                    //                  tmp.timeFirstSeen = tmp.arrivalTime;
                    switch (mask) {
                        case 1: // fcfs
                        case 2: // rr
                            if (readyQ.size() > 0 && readyQ.getFirst() == nullProc) {
                                readyQ.removeFirst(); //removing nullProcess
                                readyQ.addFirst(tmp);
                                readyQ.addFirst(nullProc);
                            } else readyQ.addFirst(tmp);
                            break;
                        case 4: //priority
                        case 6: //multilevelQ
                            j = 0; // don't have to worry about tmp.priority being less than nullProcess
                            while (!inserted && j < readyQ.size()) {
                                if (readyQ.get(j).priority > tmp.priority) {
                                    readyQ.add(j, tmp);
                                    inserted = true;
                                } else j = j + 1;
                            }
                            if (!inserted)
                                readyQ.addLast(tmp);
                            break;
                    }
                }
                newQ.remove(i);// inserted into readyQ so remove from newQ
            }
        }
    }

    public static void terminate(LinkedList<Process> runningQ, int cpuTime) {
        //nullProc should never get here
        Process tmp;
        int turnAroundTime;
        tmp = runningQ.getFirst();
        tmp.timeFinished = cpuTime;
        runningQ.remove(tmp);
        turnAroundTime = tmp.timeFinished - tmp.timeStarted;
        System.out.println("PID: " + tmp.pid +
                (mask == 4 ? ", priority: " + tmp.priority : "") +
                ", start time: " + tmp.timeStarted +
                ", finished: " + tmp.timeFinished +
                ", timeFirstSeen by CPU: " + tmp.timeFirstSeen +
                ", waiting: " + tmp.totalTimeWaiting +
                ", cpu: " + tmp.totalCpuTimeUsed +
                ", event: " + tmp.totalEventTime +
                ", turnaround time: " + turnAroundTime); //had tmp + in
//        stats[0] += 1;
//        stats[1] += tmp.totalTimeWaiting;
//        stats[2] += turnAroundTime;
    }

    public static void addEventQueue(LinkedList<Process> runningQ, LinkedList<Process> eventList) {
        // add proc to event queue
        // let's hope null proc doesn't get here
        // update process info
        Process tmp = runningQ.getFirst();
        tmp.eventTime = tmp.eventCycles.get(0);
        tmp.eventCycles.remove(0);
        eventList.add(tmp);
        runningQ.clear();
    }

    public static void incEventList(LinkedList<Process> eventList, LinkedList<Process> readyQ, int mask, int cpuTime) {
        // add time (+1) to totalEventTime for each element
        // subtract 1 from eventTime
        // remove any whose eventTime is zero - place on readyQ
        for (Process proc : eventList) {
            proc.eventTime -= 1;
            proc.totalEventTime += 1;
        }
        for (int i = eventList.size() - 1; i >= 0; i--) {
            if (eventList.get(i).eventTime == 0) {
                // remove from List, get next cpu burst, place on readyQ
                // very similar to newArrival
                arrivalToReadyQ(eventList, readyQ, eventList.get(i), mask, cpuTime);
            }
        }
    }

    public static void arrivalToReadyQ(LinkedList<Process> fromQ, LinkedList<Process> toQ, Process proc,
                                       int mask, int cpuTime) {
        // proc is in fromQ, delete it in fromQ and add to toQ
        // insertion method determined by mask
        // what is the cpuTime?
        boolean inserted = false;
        int j;
        fromQ.remove(proc);
        proc.cpuBurstRate = proc.cpuCycles.get(0);
        proc.cpuCycles.remove(0);
        proc.arrivalTime = cpuTime;
        switch (mask) {
            case 1: // fcfs
            case 2: // rr
                if (toQ.size() > 0 && toQ.getFirst() == nullProc) {
                    toQ.removeFirst(); //removing nullProcess
                    toQ.addFirst(proc);
                    toQ.addFirst(nullProc);
                } else toQ.addFirst(proc);
                break;
            case 4: //priority
            case 6: //multilevelQ
                j = 0; // don't have to worry about tmp.priority being less than nullProcess
                while (!inserted && j < toQ.size()) {
                    if (toQ.get(j).priority > proc.priority) {
                        toQ.add(j, proc);
                        inserted = true;
                    } else j = j + 1;
                }
                if (!inserted)
                    toQ.addLast(proc);
                break;
        }
    }

    public static void whichQNext(LinkedList<Process> runningQ, LinkedList<Process> eventList, int cpuTime) {
        // coming off of runningQ; two choices - terminate or event
        // determine if event -> eventQ
        //           if done -> terminateQ
        // nullProc shouldn't get here
        Process tmp = runningQ.getFirst();
        tmp.totalCpuTimeUsed += tmp.cpuBurstRate;
        tmp.totalTimeWaiting += tmp.timeWaiting;
        tmp.timeWaiting = 0;
        tmp.cpuTimeUsed = 0;
        if (tmp.eventCycles.size() == 0) {
            //terminate
            terminate(runningQ, cpuTime);
        } else addEventQueue(runningQ, eventList);
    }

    public static class Process {
        int pid;
        int arrivalTime;    // time used for current cpu burst
        int cpuBurstRate;   // current cup burst rate
        int eventTime;      // current event time
        int priority;
        int timeFirstSeen;  // time the process first enters ready queue for current cpu burst
        int cpuTimeUsed;    // amount of time cpu used in current cpu burst
        int timeFinished;   // time that the process enters terminate queue
        int timeWaiting;    // time waiting in readyQ for current cpu burst
        int timeStarted;    // time process entered readyQ first time
        int totalCpuTimeUsed;   // total of all cpu bursts
        int totalTimeWaiting;   // total of time waiting
        int totalEventTime;     // total of all event
        ArrayList<Integer> cpuCycles;
        ArrayList<Integer> eventCycles;

        public Process() {
            cpuCycles = new ArrayList<>();
            eventCycles = new ArrayList<>();
        }

        public Process(int pid, int arrivalTime, int cpuBurstRate, int priority) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.cpuBurstRate = cpuBurstRate;
            this.priority = priority;
            cpuCycles = new ArrayList<>();
            eventCycles = new ArrayList<>();
        }

        public static Process makeCopy(Process proc) {
            Process retVal;
            retVal = new Process(proc.pid, proc.arrivalTime, proc.cpuBurstRate, proc.priority);
            for (Integer cpu : proc.cpuCycles)
                retVal.cpuCycles.add(cpu);
            for (Integer event : proc.eventCycles)
                retVal.eventCycles.add(event);
            return retVal;
        }

        public String initToString() {
            String retVal;
            String cpuCycles = this.cpuCycles.toString();
            cpuCycles = "[" + cpuBurstRate + ", " + cpuCycles.substring(1);
            retVal = "pid: " + pid + ", arrivalTime: " + arrivalTime + ", CPU burst rates: " +
                    cpuCycles + ", events: " + this.eventCycles.toString();
            return retVal;
        }

        public String toString() {
            String retVal;
            retVal = "pid: " + pid + ((priority != -1) ? ", priority: " + priority : "") + ", arrivalTime: " +
                    arrivalTime + ", timeStarted: " + timeStarted + ", cpuBurstRate: " + cpuBurstRate +
                    ", cpuTimeUsed: " + cpuTimeUsed + ", timeFinished: " + timeFinished + ", timeWaiting: " + timeWaiting;
            retVal = retVal + ", CPUcyc" + this.cpuCycles.toString();
            retVal = retVal + ", events: " + this.eventCycles.toString();
            return retVal;
        }
    }
}