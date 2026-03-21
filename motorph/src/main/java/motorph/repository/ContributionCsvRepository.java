package motorph.repository;

import com.opencsv.exceptions.CsvValidationException;
import motorph.util.CsvUtils;
import motorph.util.FilePathResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ContributionCsvRepository {

    public List<String[]> readContributionFile(String fileName) throws IOException, CsvValidationException {
        Path path = FilePathResolver.resolveDataPath(fileName);
        return CsvUtils.readAll(path);
    }
}
