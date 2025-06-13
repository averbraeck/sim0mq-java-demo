package org.sim0mq.demo.mm1;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.flow.Create;
import nl.tudelft.simulation.dsol.formalisms.flow.Delay;
import nl.tudelft.simulation.dsol.formalisms.flow.Entity;
import nl.tudelft.simulation.dsol.formalisms.flow.Release;
import nl.tudelft.simulation.dsol.formalisms.flow.Resource;
import nl.tudelft.simulation.dsol.formalisms.flow.Seize;
import nl.tudelft.simulation.dsol.model.AbstractDsolModel;
import nl.tudelft.simulation.dsol.simtime.dist.DistContinuousSimulationTime;
import nl.tudelft.simulation.dsol.simulators.DevsSimulator;
import nl.tudelft.simulation.dsol.statistics.SimPersistent;
import nl.tudelft.simulation.dsol.statistics.SimTally;
import nl.tudelft.simulation.jstats.distributions.DistConstant;
import nl.tudelft.simulation.jstats.distributions.DistDiscreteConstant;
import nl.tudelft.simulation.jstats.distributions.DistExponential;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * The M/M/1 example as published in Simulation Modeling and Analysis by A.M. Law &amp; W.D. Kelton section 1.4 and 2.4.
 * <p>
 * (c) copyright 2015-2025 Delft University of Technology </a>, the Netherlands. <br>
 * See for project information <a href="http://www.simulation.tudelft.nl">www.simulation.tudelft.nl </a> <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser General Public License (LGPL) </a>, no warranty.
 * @version 2.0 21.09.2003 <br>
 * @author <a href="https://www.linkedin.com/in/peterhmjacobs">Peter Jacobs </a>
 */
public class MM1Queue41Model extends AbstractDsolModel<Double, DevsSimulator<Double>>
{
    /** The default serial version UID for serializable classes. */
    private static final long serialVersionUID = 1L;

    /** tally dN. */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    SimTally<Double> dN;

    /** persistent qN. */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    SimPersistent<Double> qN;

    /** utilization uN. */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    SimPersistent<Double> uN;

    /** PARAMETER iat. */
    public double iat = Double.NaN;

    /** PARAMETER serviceTime. */
    public double serviceTime = Double.NaN;

    /** PARAMETER seed. */
    public long seed = 1;

    /**
     * Construct the model.
     * @param simulator the simulator
     */
    public MM1Queue41Model(final DevsSimulator<Double> simulator)
    {
        super(simulator);
    }

    @Override
    public final void constructModel() throws SimRuntimeException
    {
        if (Double.isNaN(this.iat))
        {
            throw new SimRuntimeException("Parameter iat not defined for model");
        }
        if (Double.isNaN(this.serviceTime))
        {
            throw new SimRuntimeException("Parameter servicetime not defined for model");
        }
        
        System.out.println("iat=" + this.iat + ", st=" + this.serviceTime);

        StreamInterface defaultStream = new MersenneTwister(this.seed);

        // The Generator
        Create<Double> generator = new Create<Double>("generate", this.simulator)
                .setStartTimeDist(new DistContinuousSimulationTime.TimeDouble(new DistConstant(defaultStream, 0.0)))
                .setIntervalDist(new DistContinuousSimulationTime.TimeDouble(new DistExponential(defaultStream, this.iat)))
                .setBatchSizeDist(new DistDiscreteConstant(defaultStream, 1))
                .setEntitySupplier(() -> new Entity<Double>("entity", getSimulator()));
        generator.setMaxNumberGeneratedEntities(1000);

        // The queue and resource
        var resource = new Resource.DoubleCapacity<>("resource", this.simulator, 1.0);
        resource.setDefaultStatistics();

        // created the claiming of the resource
        var seize = new Seize.DoubleCapacity<Double>("Seize", this.simulator, resource);
        seize.setFixedCapacityClaim(1.0);
        seize.setDefaultStatistics();

        // The delay for the service
        DistContinuousSimulationTime<Double> serviceTime =
                new DistContinuousSimulationTime.TimeDouble(new DistExponential(defaultStream, this.serviceTime));
        var service = new Delay<Double>("Delay", this.simulator).setDelayDistribution(serviceTime);

        // release the claimed resource
        var release = new Release.DoubleCapacity<Double>("Release", this.simulator, resource);
        release.setFixedCapacityRelease(1.0);

        // The flow
        generator.setDestination(seize);
        seize.setDestination(service);
        service.setDestination(release);

        // Statistics
        this.dN = seize.getStorageTimeStatistic();
        this.qN = seize.getNumberStoredStatistic();
        this.uN = resource.getUtilizationStatistic();
    }

}
