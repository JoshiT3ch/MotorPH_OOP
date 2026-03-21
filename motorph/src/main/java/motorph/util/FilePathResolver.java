package motorph.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class FilePathResolver {

    private FilePathResolver() {
    }

    public static Path resolveDataPath(String fileName) {
        Path normalizedFile = Paths.get(fileName).normalize();
        if (normalizedFile.isAbsolute()) {
            return normalizedFile;
        }

        Path[] candidates = new Path[] {
                Paths.get("data").resolve(normalizedFile),
                Paths.get(normalizedFile.toString()),
                Paths.get("motorph").resolve("data").resolve(normalizedFile),
                Paths.get("motorph").resolve(normalizedFile)
        };

        for (Path candidate : candidates) {
            Path normalizedCandidate = candidate.normalize();
            if (normalizedCandidate.toFile().exists()) {
                return normalizedCandidate;
            }
        }

        return candidates[0].normalize();
    }

    public static Optional<Path> resolveExistingPath(String... relativeCandidates) {
        for (String candidate : relativeCandidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path resolved = Paths.get(candidate).normalize();
            if (!resolved.isAbsolute()) {
                resolved = resolveDataPath(candidate);
            }
            if (resolved.toFile().exists()) {
                return Optional.of(resolved);
            }
        }
        return Optional.empty();
    }
}
