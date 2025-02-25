package org.sim0mq.demo.mm1;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.Resource;
import nl.tudelft.simulation.dsol.formalisms.flow.Create;
import nl.tudelft.simulation.dsol.formalisms.flow.Delay;
import nl.tudelft.simulation.dsol.formalisms.flow.Entity;
import nl.tudelft.simulation.dsol.formalisms.flow.FlowObject;
import nl.tudelft.simulation.dsol.formalisms.flow.Release;
import nl.tudelft.simulation.dsol.formalisms.flow.Seize;
import nl.tudelft.simulation.dsol.formalisms.flow.statistics.Utilization;
import nl.tudelft.simulation.dsol.model.AbstractDsolModel;
import nl.tudelft.simulation.dsol.simtime.dist.DistContinuousSimulationTime;
import nl.tudelft.simulation.dsol.simulators.DevsSimulator;
import nl.tudelft.simulation.dsol.statistics.SimPersistent;
import nl.tudelft.simulation.dsol.statistics.SimTally;
import nl.tudelft.simulation.jstats.distributions.DistConstant;
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
    Utilization<Double> uN;

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

        StreamInterface defaultStream = new MersenneTwister(this.seed);

        // The Generator
        Create<Double> generator = new Create<Double>("generate", this.simulator,
                new DistContinuousSimulationTime.TimeDouble(new DistConstant(defaultStream, 0.0)),
                new DistContinuousSimulationTime.TimeDouble(new DistExponential(defaultStream, this.iat)), 1)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected Entity<Double> generateEntity()
            {
                return new Entity<Double>("entity", getSimulator().getSimulatorTime());
            }
        };
        generator.setMaxNumber(1000);

        // The queue, the resource and the release
        Resource<Double> resource = new Resource<>("resource", this.simulator, 1.0);

        // created the claiming and releasing of the resource
        FlowObject<Double> queue = new Seize<Double>("Seize", this.simulator, resource);
        FlowObject<Double> release = new Release<Double>("Release", this.simulator, resource, 1.0);

        // The server
        DistContinuousSimulationTime<Double> serviceTimeDistribution =
                new DistContinuousSimulationTime.TimeDouble(new DistExponential(defaultStream, this.serviceTime));
        FlowObject<Double> server = new Delay<Double>("Delay", this.simulator, serviceTimeDistribution);

        // The flow
        generator.setDestination(queue);
        queue.setDestination(server);
        server.setDestination(release);

        // Statistics
        this.dN = new SimTally<Double>("d(n)", this, queue, Seize.DELAY_TIME);
        this.qN = new SimPersistent<Double>("q(n)", this, queue, Seize.QUEUE_LENGTH_EVENT);
        this.uN = new Utilization<>("u(n)", this, server);
    }

}
