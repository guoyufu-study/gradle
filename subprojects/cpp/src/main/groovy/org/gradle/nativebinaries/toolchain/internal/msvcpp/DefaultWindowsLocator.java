package org.gradle.nativebinaries.toolchain.internal.msvcpp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.specs.Spec;

public class DefaultWindowsLocator implements WindowsLocator {

    protected Search locateInProgramFiles(Spec<File> condition, VersionLookupPath... candidateLocations) {
        Search search = new Search();
        for (VersionLookupCandidate candidate : programFileCandidates(candidateLocations)) {
            if (condition.isSatisfiedBy(candidate.file)) {
                search.found(candidate);
                break;
            }
            search.notFound(candidate.file);
        }
        return search;
    }

    protected List<File> programFileCandidates(String... candidateDirectory) {
        String[] programFilesDirectories;

        // On 64-bit windows, PROGRAMFILES doesn't return a consistent location:
        // depends on whether the process requesting the environment variable is itself 32-bit or 64-bit.
        String programFilesX64 = System.getenv("ProgramW6432");
        String programFilesX86 = System.getenv("PROGRAMFILES(x86)");
        if (programFilesX64 != null && programFilesX86 != null) {
            programFilesDirectories = new String[]{programFilesX64, programFilesX86};
        } else {
            programFilesDirectories = new String[]{System.getenv("PROGRAMFILES")};
        }

        List<File> candidates = new ArrayList<File>(candidateDirectory.length * 2);
        for (String candidateLocation : candidateDirectory) {
            for (String programFilesDirectory : programFilesDirectories) {
                candidates.add(new File(programFilesDirectory, candidateLocation));
            }
        }
        return candidates;
    }

    protected List<VersionLookupCandidate> programFileCandidates(VersionLookupPath... candidateDirectory) {
        List<VersionLookupCandidate> candidates = new ArrayList<VersionLookupCandidate>(candidateDirectory.length * 2);
        for (VersionLookupPath candidateLocation : candidateDirectory) {
            for (File file : programFileCandidates(candidateLocation.path)) {
                candidates.add(new VersionLookupCandidate(file, candidateLocation.version));
            }
        }
        return candidates;
    }

    protected Search locateInHierarchy(File candidate, Spec<File> condition) {
        Search search = new Search();
        while (candidate != null) {
            if (condition.isSatisfiedBy(candidate)) {
                search.found(candidate);
                return search;
            }
            search.notFound(candidate);
            candidate = candidate.getParentFile();
        }
        return search;
    }

    protected static class VersionLookupPath {
        String path;
        String version;

        public VersionLookupPath(String path, String version) {
            this.path = path;
            this.version = version;
        }

    }
 
    protected static class VersionLookupCandidate {
        File file;
        String version;

        public VersionLookupCandidate(File file, String version) {
            this.file = file;
            this.version = version;
        }

    }

    public class Search implements SearchResult {
        private String version;
        private File file;
        private List<File> searchLocations = new ArrayList<File>();

        public boolean isFound() {
            return file != null;
        }

        public String getVersion() {
            return version;
        }

        public File getResult() {
            return file;
        }

        public List<File> getSearchLocations() {
            return searchLocations;
        }

        void notFound(File candidate) {
            searchLocations.add(candidate);
        }

        void found(File candidate) {
            this.file = candidate;
        }

        void found(VersionLookupCandidate candidate) {
            this.file = candidate.file;
            this.version = candidate.version;
        }
    }

}
