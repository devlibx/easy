package io.gitbub.harishb2k.easy.helper.mysql;

public class MySqlTestHelper implements IMySqlTestHelper {
    private DockerMySqTestHelper dockerMySqTestHelper;
    private IMySqlTestHelper customMySqlTestHelper;

    public MySqlTestHelper() {
        dockerMySqTestHelper = new DockerMySqTestHelper();
    }

    @Override
    public void installCustomMySqlTestHelper(IMySqlTestHelper customMySqlTestHelper) {
        this.customMySqlTestHelper = customMySqlTestHelper;
    }

    @Override
    public void startMySql(TestMySqlConfig config) {
        if (customMySqlTestHelper != null) {
            customMySqlTestHelper.startMySql(config);
            if (customMySqlTestHelper.isMySqlRunning()) {
                return;
            } else {
                customMySqlTestHelper = null;
            }
        }
        dockerMySqTestHelper.startMySql(config);
    }

    @Override
    public void stopMySql() {
        if (customMySqlTestHelper != null) {
            customMySqlTestHelper.stopMySql();
        } else {
            dockerMySqTestHelper.stopMySql();
        }
    }

    @Override
    public TestMySqlConfig getMySqlConfig() {
        if (customMySqlTestHelper != null) {
            return customMySqlTestHelper.getMySqlConfig();
        } else {
            return dockerMySqTestHelper.getMySqlConfig();
        }
    }

    @Override
    public boolean isMySqlRunning() {
        if (customMySqlTestHelper != null) {
            return customMySqlTestHelper.isMySqlRunning();
        } else {
            return dockerMySqTestHelper.isMySqlRunning();
        }
    }
}
