package com.berdachuk.medexpertmatch.chunking.api;

import java.util.List;

public interface Chunker {

    List<String> chunk(String text, int chunkSize, int overlap, int minChars);
}
