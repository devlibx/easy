package io.gitbub.devlibx.easy.helper.pid;

public abstract class MiniPIDExample {
    public static void main(String[] args) throws InterruptedException {
        MiniPID miniPID = new MiniPID(0.25, 0.01, 0.4);
        miniPID.setSetpoint(10);
        miniPID.setMaxIOutput(53);
        miniPID.setOutputRampRate(2);
        double actual = 0;
        double output = 0;
        double target = 50;

        System.err.printf("Target\tActual\tOutput\tError\n");
        for (int i = 0; i < 100; i++) {
            output = miniPID.getOutput(actual, target);
            System.err.printf("%3.2f\t%3.2f\t%3.2f\t%3.2f\n", target, actual, output, (target - actual));

            actual = actual + output;
            Thread.sleep(100);
        }
    }
}
