package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

public class MSSClient {

    private final DynamoDbClient dynamoClient;
    private final DynamoDbEnhancedClient enhancedClient;

    private final DynamoDbTable<CaptureTheFlagMetric> ctfTable;
    private final DynamoDbTable<GameOfLifeMetric> golTable;
    private final DynamoDbTable<FifteenPuzzleMetric> fpTable;

    public MSSClient() {
        this.dynamoClient = DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.US_EAST_1).build();
        this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(this.dynamoClient).build();
        this.ctfTable = this.enhancedClient.table("CaptureTheFlag", TableSchema.fromClass(CaptureTheFlagMetric.class));
        this.golTable = this.enhancedClient.table("GameOfLife", TableSchema.fromClass(GameOfLifeMetric.class));
        this.fpTable = this.enhancedClient.table("FifteenPuzzle", TableSchema.fromClass(FifteenPuzzleMetric.class));
        createTable(this.ctfTable);
        createTable(this.golTable);
        createTable(this.fpTable);
    }

    private void createTable(DynamoDbTable<?> table) {
        try {
            table.createTable();
        } catch (ResourceInUseException e) {
            // ignore if already exists
        }
    }

    public DynamoDbTable<CaptureTheFlagMetric> getCaptureTheFlagTable() {
        return this.ctfTable;
    }

    public DynamoDbTable<GameOfLifeMetric> getGameOfLifeTable() {
        return this.golTable;
    }

    public DynamoDbTable<FifteenPuzzleMetric> getFifteenPuzzleTable() {
        return this.fpTable;
    }

}
