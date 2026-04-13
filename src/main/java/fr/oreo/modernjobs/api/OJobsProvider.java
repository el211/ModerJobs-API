package fr.oreo.modernjobs.api;

import fr.oreo.modernjobs.ModernJobs;

import java.util.Optional;


public final class OJobsProvider {

    private OJobsProvider() {}

    public static Optional<OJobsApi> get() {
        ModernJobs plugin = ModernJobs.getInstance();
        return plugin == null ? Optional.empty() : Optional.of(plugin.getApi());
    }

    public static OJobsApi getOrThrow() {
        return get().orElseThrow(() -> new IllegalStateException("ModernJobs API is not available"));
    }
}
