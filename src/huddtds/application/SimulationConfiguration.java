package huddtds.application;

import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.algorithm.mining.HUIItemsetMiner;

import java.io.File;

/**
 * Immutable configuration for creating a simulation through the application facade.
 */
public final class SimulationConfiguration {
    private final String datasetName;
    private final File customInvestmentFile;
    private final double minutil;
    private final int interval;
    private final int windowSize;
    private final double alphaConfidence;
    private final int maxItemsetSize;
    private final HUIItemsetMiner huiItemsetMiner;
    private final GlobalDriftStrategy globalDriftStrategy;
    private final LocalDriftStrategy localDriftStrategy;

    private SimulationConfiguration(Builder builder) {
        this.datasetName = builder.datasetName;
        this.customInvestmentFile = builder.customInvestmentFile;
        this.minutil = builder.minutil;
        this.interval = builder.interval;
        this.windowSize = builder.windowSize;
        this.alphaConfidence = builder.alphaConfidence;
        this.maxItemsetSize = builder.maxItemsetSize;
        this.huiItemsetMiner = builder.huiItemsetMiner;
        this.globalDriftStrategy = builder.globalDriftStrategy;
        this.localDriftStrategy = builder.localDriftStrategy;
    }

    public static Builder builder(String datasetName) {
        return new Builder(datasetName);
    }

    public String getDatasetName() {
        return datasetName;
    }

    public File getCustomInvestmentFile() {
        return customInvestmentFile;
    }

    public double getMinutil() {
        return minutil;
    }

    public int getInterval() {
        return interval;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public double getAlphaConfidence() {
        return alphaConfidence;
    }

    public int getMaxItemsetSize() {
        return maxItemsetSize;
    }

    public HUIItemsetMiner getHuiItemsetMiner() {
        return huiItemsetMiner;
    }

    public GlobalDriftStrategy getGlobalDriftStrategy() {
        return globalDriftStrategy;
    }

    public LocalDriftStrategy getLocalDriftStrategy() {
        return localDriftStrategy;
    }

    public static final class Builder {
        private final String datasetName;
        private File customInvestmentFile;
        private double minutil = 10.0;
        private int interval = 1;
        private int windowSize = 2;
        private double alphaConfidence = 0.05;
        private int maxItemsetSize;
        private HUIItemsetMiner huiItemsetMiner;
        private GlobalDriftStrategy globalDriftStrategy;
        private LocalDriftStrategy localDriftStrategy;

        private Builder(String datasetName) {
            if (datasetName == null || datasetName.isBlank()) {
                throw new IllegalArgumentException("datasetName must not be blank");
            }
            this.datasetName = datasetName;
        }

        public Builder withCustomInvestmentFile(File file) {
            this.customInvestmentFile = file;
            return this;
        }

        public Builder withMinutil(double minutil) {
            if (!Double.isFinite(minutil) || minutil < 0.0) {
                throw new IllegalArgumentException("minutil must be finite and non-negative");
            }
            this.minutil = minutil;
            return this;
        }

        public Builder withInterval(int interval) {
            if (interval <= 0) {
                throw new IllegalArgumentException("interval must be greater than zero");
            }
            this.interval = interval;
            return this;
        }

        public Builder withWindowSize(int windowSize) {
            if (windowSize <= 0) {
                throw new IllegalArgumentException("windowSize must be greater than zero");
            }
            this.windowSize = windowSize;
            return this;
        }

        public Builder withAlphaConfidence(double alphaConfidence) {
            if (!Double.isFinite(alphaConfidence) || alphaConfidence <= 0.0 || alphaConfidence >= 1.0) {
                throw new IllegalArgumentException("alphaConfidence must be in (0, 1)");
            }
            this.alphaConfidence = alphaConfidence;
            return this;
        }

        public Builder withMaxItemsetSize(int maxItemsetSize) {
            if (maxItemsetSize < 0) {
                throw new IllegalArgumentException("maxItemsetSize must not be negative");
            }
            this.maxItemsetSize = maxItemsetSize;
            return this;
        }

        public Builder withHuiItemsetMiner(HUIItemsetMiner miner) {
            this.huiItemsetMiner = miner;
            return this;
        }

        public Builder withGlobalDriftStrategy(GlobalDriftStrategy strategy) {
            this.globalDriftStrategy = strategy;
            return this;
        }

        public Builder withLocalDriftStrategy(LocalDriftStrategy strategy) {
            this.localDriftStrategy = strategy;
            return this;
        }

        public SimulationConfiguration build() {
            return new SimulationConfiguration(this);
        }
    }
}
