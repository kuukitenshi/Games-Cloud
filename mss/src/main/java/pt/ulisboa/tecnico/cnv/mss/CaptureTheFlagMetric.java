package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class CaptureTheFlagMetric implements MSSMetric {

    private String id;
    private int gridSize;
    private int blue;
    private int red;
    private char flagPlacement;
    private long instructionCount;

    public CaptureTheFlagMetric() {
    }

    public CaptureTheFlagMetric(int gridSize, int blue, int red, char flagPlacement, long instructionCount) {
        this.id = String.join("#", Integer.toString(gridSize), Integer.toString(blue), Integer.toString(red),
                Character.toString(flagPlacement));
        this.gridSize = gridSize;
        this.blue = blue;
        this.red = red;
        this.flagPlacement = flagPlacement;
        this.instructionCount = instructionCount;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public char getFlagPlacement() {
        return flagPlacement;
    }

    public void setFlagPlacement(char flagPlacement) {
        this.flagPlacement = flagPlacement;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

    @Override
    public void uploadMetric(MSSClient client) {
        client.getCaptureTheFlagTable().putItem(this);
    }

}
