package com.berdachuk.medexpertmatch.documents.api;

import java.util.List;

public interface DocumentIngestApi {

    int ingestPaths(List<String> paths);

    int ingestFromDirectory(String directoryPath);
}
