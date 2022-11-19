package io.gitbub.devlibx.easy.helper.pid;

public abstract class MiniPIDExample {
    public static void main(String[] args) throws InterruptedException {
        MiniPID miniPID = new MiniPID(0.25, 0.01, 0.4);
        miniPID.setSetpoint(100);
        double actual = 0;
        double output = 0;
        double target = 50;

        System.err.printf("Target\tActual\tOutput\tError\n");
        for (int i = 0; i < 100; i++) {
            output = miniPID.getOutput(actual, target);
            actual = actual + output;

            //System.out.println("==========================");
            //System.out.printf("Current: %3.2f , Actual: %3.2f, Error: %3.2f\n",actual, output, (target-actual));
            System.err.printf("%3.2f\t%3.2f\t%3.2f\t%3.2f\n", target, actual, output, (target - actual));
            Thread.sleep(1000);
        }
    }
}
