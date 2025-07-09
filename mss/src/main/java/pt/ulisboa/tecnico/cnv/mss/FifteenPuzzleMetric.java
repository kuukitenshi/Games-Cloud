package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FifteenPuzzleMetric implements MSSMetric {

    public String id;
    private int size;
    private int shuffles;
    private long instructionCount;

    public FifteenPuzzleMetric() {
    }

    public FifteenPuzzleMetric(int size, int shuffles, long instructionCount) {
        this.id = String.join("#", Integer.toString(size), Integer.toString(shuffles));
        this.size = size;
        this.shuffles = shuffles;
        this.instructionCount = instructionCount;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getShuffles() {
        return shuffles;
    }

    public void setShuffles(int shuffles) {
        this.shuffles = shuffles;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

    @Override
    public void uploadMetric(MSSClient client) {
        client.getFifteenPuzzleTable().putItem(this);
    }

}
