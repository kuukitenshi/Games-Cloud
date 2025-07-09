package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class GameOfLifeMetric implements MSSMetric {

    private String id;
    private String mapFile;
    private int iterations;
    private long instructionCount;

    public GameOfLifeMetric() {
    }

    public GameOfLifeMetric(String mapFile, int iterations, long instructionCount) {
        this.id = String.join("#", mapFile, Integer.toString(iterations));
        this.mapFile = mapFile;
        this.iterations = iterations;
        this.instructionCount = instructionCount;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMapFile() {
        return mapFile;
    }

    public void setMapFile(String mapFile) {
        this.mapFile = mapFile;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

    @Override
    public void uploadMetric(MSSClient client) {
        client.getGameOfLifeTable().putItem(this);
    }

}
