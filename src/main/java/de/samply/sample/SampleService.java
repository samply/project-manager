package de.samply.sample;

import de.samply.db.repository.SampleCollectionRepository;
import de.samply.db.repository.SampleRepository;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

    private final SampleRepository sampleRepository;
    private final SampleCollectionRepository sampleCollectionRepository;

    public SampleService(SampleRepository sampleRepository, SampleCollectionRepository sampleCollectionRepository) {
        this.sampleRepository = sampleRepository;
        this.sampleCollectionRepository = sampleCollectionRepository;
    }


}
